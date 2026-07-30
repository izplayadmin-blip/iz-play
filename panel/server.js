const express = require('express')
const { exec } = require('child_process')
const fs = require('fs')
const path = require('path')
const crypto = require('crypto')
const http = require('http')
const https = require('https')
const { createCollector } = require('./lib/xui/collector')

const app = express()
const PORT = Number(process.env.PANEL_PORT || process.env.PORT || 3000)
const DATA_DIR = path.join(__dirname, 'data')
const REPORTS_FILE = path.join(DATA_DIR, 'reports.json')
const DEVICES_FILE = path.join(DATA_DIR, 'devices.json')
const CONFIG_FILE = path.join(DATA_DIR, 'config.json')
const COMMANDS_FILE = path.join(DATA_DIR, 'commands.json')
const P2P_FILE = path.join(DATA_DIR, 'p2p.json')
const UPDATES_FILE = path.join(DATA_DIR, 'updates.json')
const APK_DIR = path.join(DATA_DIR, 'apks')

// ── CONFIG ──────────────────────────────────────────────────────────────────
const ADMIN_USER = process.env.PANEL_USER || 'admin'
const ADMIN_PASS = process.env.PANEL_PASS || ''
const ALLOWED_ORIGINS = String(process.env.PANEL_ALLOWED_ORIGINS || 'https://izplay.tv,https://www.izplay.tv,https://web.izplay.tv')
  .split(',').map(value => value.trim()).filter(Boolean)
const sessions = new Map()
const loginAttempts = new Map()

if (!ADMIN_PASS && process.env.NODE_ENV === 'production') {
  throw new Error('PANEL_PASS é obrigatório em produção')
}

fs.mkdirSync(DATA_DIR, { recursive: true })
fs.mkdirSync(APK_DIR, { recursive: true })
const xuiCollector = createCollector({
  dataDir: DATA_DIR,
  intervalMs: Math.max(15000, Number(process.env.XUI_SYNC_INTERVAL_MS || 60000))
})

const DEFAULT_CONFIG = {
  defaultDns: process.env.DEFAULT_DNS || '',
  dnsServers: (process.env.DNS_SERVERS || '').split(',').map(s => s.trim()).filter(Boolean),
  proxyUrl: process.env.PROXY_URL || '/gateway',
  telemetryUrl: process.env.TELEMETRY_URL || '/control/api',
  gatewayUrl: process.env.GATEWAY_URL || '/gateway',
  videoGatewayUrl: process.env.PUBLIC_GATEWAY_URL || '/video-gateway',
  protectedGatewayUrl: process.env.PROTECTED_GATEWAY_URL || '',
  webPlayerUrl: process.env.WEB_PLAYER_URL || 'https://web.izplay.tv/',
  swarmCloudEnabled: process.env.SWARM_CLOUD_ENABLED === 'true',
  swarmCloudUrl: process.env.SWARM_CLOUD_SDK_URL || '/player/vendor/swarmcloud-hls.min.js',
  swarmCloudKey: process.env.SWARM_CLOUD_TOKEN || '',
  swarmCloudAppId: process.env.SWARM_CLOUD_APP_ID || '',
  superPeerUrl: process.env.SUPER_PEER_URL || 'http://209.14.85.55:8080',
  vpnServers: [],
  appVersion: process.env.APP_VERSION || '2.1.0',
  updateUrl: process.env.UPDATE_URL || '',
  supportUrl: process.env.SUPPORT_URL || '',
  adminNote: ''
}
const DEFAULT_CATALOG_UPSTREAMS = ['http://ortyu.online']

function loadJson(file, fallback) {
  try {
    if (!fs.existsSync(file)) return fallback
    return JSON.parse(fs.readFileSync(file, 'utf8'))
  } catch (e) {
    console.error('[storage] Erro ao ler', path.basename(file), e.message)
    return fallback
  }
}

function saveJson(file, data) {
  try {
    fs.writeFileSync(file, JSON.stringify(data, null, 2))
  } catch (e) {
    console.error('[storage] Erro ao salvar', path.basename(file), e.message)
  }
}

function safeText(value, max = 500) {
  return String(value ?? '')
    .replace(/[\u0000-\u001f\u007f]/g, ' ')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
    .slice(0, max)
}

function sanitizeTelemetry(value, depth = 0) {
  if (depth > 4) return undefined
  if (typeof value === 'string') return safeText(value, 1000)
  if (typeof value === 'number') return Number.isFinite(value) ? value : 0
  if (typeof value === 'boolean' || value === null) return value
  if (Array.isArray(value)) return value.slice(0, 50).map(item => sanitizeTelemetry(item, depth + 1))
  if (value && typeof value === 'object') {
    const output = {}
    for (const [key, item] of Object.entries(value).slice(0, 80)) {
      output[safeText(key, 80)] = sanitizeTelemetry(item, depth + 1)
    }
    return output
  }
  return undefined
}

function normalizeConfig(input) {
  const cfg = { ...DEFAULT_CONFIG, ...(input || {}) }
  const dnsServers = Array.isArray(cfg.dnsServers)
    ? cfg.dnsServers
    : String(cfg.dnsServers || cfg.defaultDns || '').split('\n')
  const cleanDnsServers = [...new Set(dnsServers.map(s => String(s || '').trim()).filter(Boolean))].slice(0, 10)
  if (!cleanDnsServers.length && cfg.defaultDns) cleanDnsServers.push(String(cfg.defaultDns).trim())
  const vpnServers = Array.isArray(cfg.vpnServers) ? cfg.vpnServers : []
  return {
    defaultDns: String(cfg.defaultDns || cleanDnsServers[0] || '').trim(),
    dnsServers: cleanDnsServers,
    proxyUrl: String(cfg.proxyUrl || '').trim(),
    telemetryUrl: String(cfg.telemetryUrl || '').trim(),
    gatewayUrl: String(cfg.gatewayUrl || '').trim(),
    videoGatewayUrl: String(cfg.videoGatewayUrl || '').trim(),
    protectedGatewayUrl: String(cfg.protectedGatewayUrl || '').trim(),
    webPlayerUrl: String(cfg.webPlayerUrl || '').trim(),
    swarmCloudEnabled: cfg.swarmCloudEnabled === true || cfg.swarmCloudEnabled === 'true',
    swarmCloudUrl: String(cfg.swarmCloudUrl || '').trim(),
    swarmCloudKey: String(cfg.swarmCloudKey || '').trim().slice(0, 200),
    swarmCloudAppId: String(cfg.swarmCloudAppId || '').trim().slice(0, 120),
    superPeerUrl: String(cfg.superPeerUrl || '').trim().slice(0, 300),
    vpnServers: vpnServers.map(server => ({
      id: String(server.id || '').trim().slice(0, 80),
      name: String(server.name || '').trim().slice(0, 120),
      country: String(server.country || '').trim().slice(0, 20),
      url: String(server.url || '').trim().replace(/\/+$/, '').slice(0, 300),
      endpoint: String(server.endpoint || '').trim().slice(0, 120),
      subnet: String(server.subnet || '').trim().slice(0, 80)
    })).filter(server => server.id && server.url).slice(0, 20),
    appVersion: String(cfg.appVersion || '').trim(),
    updateUrl: String(cfg.updateUrl || '').trim(),
    supportUrl: String(cfg.supportUrl || '').trim(),
    adminNote: String(cfg.adminNote || '').slice(0, 500)
  }
}

let panelConfig = normalizeConfig(loadJson(CONFIG_FILE, DEFAULT_CONFIG))
saveJson(CONFIG_FILE, panelConfig)
let clientCommands = loadJson(COMMANDS_FILE, [])
let appUpdates = loadJson(UPDATES_FILE, {})

const UPDATE_PLATFORMS = new Set(['android-mobile', 'android-tv'])
const UPDATE_CHANNELS = new Set(['internal', 'reseller', 'production'])

function updateKey(platform, channel) {
  return `${platform}:${channel}`
}

function normalizeUpdateRelease(input, platform, channel) {
  const versionCode = Number(input?.versionCode)
  const versionName = String(input?.versionName || '').trim().slice(0, 40)
  const downloadUrl = String(input?.downloadUrl || '').trim().slice(0, 500)
  const sha256 = String(input?.sha256 || '').trim().toLowerCase()
  if (!Number.isSafeInteger(versionCode) || versionCode < 1) throw new Error('versionCode inválido')
  if (!versionName) throw new Error('versionName é obrigatório')
  if (!downloadUrl.startsWith('https://') && !downloadUrl.startsWith('/downloads/')) {
    throw new Error('downloadUrl deve usar HTTPS ou /downloads/')
  }
  if (sha256 && !/^[a-f0-9]{64}$/.test(sha256)) throw new Error('sha256 inválido')
  return {
    platform,
    channel,
    versionCode,
    versionName,
    minimumVersionCode: Math.max(1, Number(input?.minimumVersionCode || 1)),
    mandatory: input?.mandatory === true,
    downloadUrl,
    sha256,
    fileSize: Math.max(0, Number(input?.fileSize || 0)),
    releaseNotes: safeText(input?.releaseNotes || '', 1500),
    enabled: input?.enabled !== false,
    publishedAt: new Date().toISOString()
  }
}

function probeProxyUrl(callback) {
  if (!panelConfig.proxyUrl) return callback(null, { running: false })
  try {
    const url = new URL(panelConfig.proxyUrl)
    const healthPath = String(process.env.PROXY_HEALTH_PATH || '/health').trim() || '/health'
    const client = url.protocol === 'https:' ? https : http
    const req = client.request({
      hostname: url.hostname,
      port: url.port || (url.protocol === 'https:' ? 443 : 80),
      path: healthPath.startsWith('/') ? healthPath : `/${healthPath}`,
      method: 'GET',
      timeout: 2500
    }, res => {
      res.resume()
      callback(null, {
        running: res.statusCode >= 200 && res.statusCode < 300,
        status: `http_${res.statusCode}`,
        remote: true,
        proxyUrl: panelConfig.proxyUrl,
        uptime: Date.now()
      })
    })
    req.on('timeout', () => req.destroy(new Error('timeout')))
    req.on('error', err => callback(err, { running: false, remote: true, proxyUrl: panelConfig.proxyUrl, error: err.message }))
    req.end()
  } catch (err) {
    callback(err, { running: false, remote: true, proxyUrl: panelConfig.proxyUrl, error: err.message })
  }
}

function fetchJsonUrl(targetUrl, timeoutMs = 3500, headers = {}) {
  return new Promise((resolve, reject) => {
    try {
      const url = new URL(targetUrl)
      const client = url.protocol === 'https:' ? https : http
      const req = client.request({
        hostname: url.hostname,
        port: url.port || (url.protocol === 'https:' ? 443 : 80),
        path: `${url.pathname || '/'}${url.search || ''}`,
        method: 'GET',
        headers,
        timeout: timeoutMs
      }, res => {
        let body = ''
        res.setEncoding('utf8')
        res.on('data', chunk => { body += chunk; if (body.length > 1024 * 1024) req.destroy(new Error('response_too_large')) })
        res.on('end', () => {
          try {
            resolve({
              statusCode: res.statusCode,
              ok: res.statusCode >= 200 && res.statusCode < 300,
              json: body ? JSON.parse(body) : {}
            })
          } catch (err) {
            reject(err)
          }
        })
      })
      req.on('timeout', () => req.destroy(new Error('timeout')))
      req.on('error', reject)
      req.end()
    } catch (err) {
      reject(err)
    }
  })
}

function allowedCatalogPath(pathname) {
  if (/^\/(?:live|movie|series)\//i.test(pathname)) return true
  return [
    '/player_api.php',
    '/get.php',
    '/xmltv.php',
    '/iptv.m3u'
  ].includes(pathname)
}

function catalogUpstreams() {
  const envUpstreams = String(process.env.CATALOG_UPSTREAMS || '').split(',')
  return [...new Set([
    ...envUpstreams,
    ...panelConfig.dnsServers,
    panelConfig.defaultDns,
    ...DEFAULT_CATALOG_UPSTREAMS
  ].map(value => String(value || '').trim().replace(/\/+$/, '')).filter(Boolean))]
}

function pipeCatalogEmergency(req, res, upstreams, catalogPath, queryString, lastError) {
  const upstream = upstreams.shift()
  if (!upstream) {
    return res.status(502).json({
      ok: false,
      error: 'catalog_emergency_unavailable',
      message: 'Catálogo de emergência indisponível',
      detail: lastError ? lastError.message : ''
    })
  }

  let target
  try {
    target = new URL(upstream + catalogPath)
    if (queryString) target.search = queryString
  } catch (err) {
    return pipeCatalogEmergency(req, res, upstreams, catalogPath, queryString, err)
  }

  const client = target.protocol === 'https:' ? https : http
  const upstreamReq = client.request(target, {
    method: 'GET',
    timeout: 18000,
    headers: {
      'User-Agent': 'Mozilla/5.0 IZPlay-Panel-Catalog/2.1',
      'Accept': req.headers.accept || '*/*'
    }
  }, upstreamRes => {
    const statusCode = upstreamRes.statusCode || 502
    if ([403, 404, 429, 500, 502, 503, 504].includes(statusCode) && upstreams.length) {
      upstreamRes.resume()
      return pipeCatalogEmergency(req, res, upstreams, catalogPath, queryString, new Error(`upstream_http_${statusCode}`))
    }
    res.setHeader('Cache-Control', 'no-store')
    res.setHeader('Content-Type', upstreamRes.headers['content-type'] || 'application/json; charset=utf-8')
    res.status(statusCode)
    upstreamRes.pipe(res)
  })
  upstreamReq.on('timeout', () => upstreamReq.destroy(new Error('upstream_timeout')))
  upstreamReq.on('error', err => pipeCatalogEmergency(req, res, upstreams, catalogPath, queryString, err))
  req.on('close', () => upstreamReq.destroy())
  upstreamReq.end()
}

app.disable('x-powered-by')
app.set('trust proxy', process.env.PANEL_TRUST_PROXY === '0' ? false : 1)
app.use((req, res, next) => {
  const origin = String(req.headers.origin || '')
  if (ALLOWED_ORIGINS.includes(origin)) {
    res.setHeader('Access-Control-Allow-Origin', origin)
    res.setHeader('Vary', 'Origin')
  }
  res.setHeader('Access-Control-Allow-Methods', 'GET,POST,PUT,OPTIONS')
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type,x-session-token')
  res.setHeader('X-Content-Type-Options', 'nosniff')
  res.setHeader('X-Frame-Options', 'SAMEORIGIN')
  res.setHeader('Referrer-Policy', 'same-origin')
  res.setHeader('Permissions-Policy', 'camera=(), microphone=(), geolocation=()')
  if (req.method === 'OPTIONS') return res.sendStatus(204)
  next()
})
app.use(express.json({ limit: '256kb' }))
app.use(express.urlencoded({ extended: true }))
app.use('/downloads', express.static(APK_DIR, {
  fallthrough: false,
  immutable: true,
  maxAge: '1d',
  setHeaders: res => {
    res.setHeader('Content-Disposition', 'attachment')
    res.setHeader('X-Content-Type-Options', 'nosniff')
  }
}))
app.use(express.static(path.join(__dirname, 'public')))
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'index.html'))
})

// ── AUTH MIDDLEWARE ──────────────────────────────────────────────────────────
function requireAuth(req, res, next) {
  const token = req.headers['x-session-token']
  if (token && sessions.has(token)) {
    const s = sessions.get(token)
    if (Date.now() - s.created < 8 * 60 * 60 * 1000) { // 8h
      return next()
    }
    sessions.delete(token)
  }
  res.status(401).json({ error: 'Não autorizado' })
}

// ── AUTH ROUTES ──────────────────────────────────────────────────────────────
app.post('/api/login', (req, res) => {
  const key = req.ip
  const now = Date.now()
  const attempt = loginAttempts.get(key)
  if (attempt && attempt.blockedUntil > now) {
    return res.status(429).json({ error: 'Muitas tentativas. Aguarde alguns minutos.' })
  }
  const user = String(req.body?.user || '')
  const pass = String(req.body?.pass || '')
  const userOk = crypto.timingSafeEqual(
    crypto.createHash('sha256').update(user).digest(),
    crypto.createHash('sha256').update(ADMIN_USER).digest()
  )
  const passOk = ADMIN_PASS && crypto.timingSafeEqual(
    crypto.createHash('sha256').update(pass).digest(),
    crypto.createHash('sha256').update(ADMIN_PASS).digest()
  )
  if (userOk && passOk) {
    loginAttempts.delete(key)
    const token = crypto.randomBytes(32).toString('hex')
    sessions.set(token, { created: Date.now(), user })
    res.json({ token, user })
  } else {
    const failures = (attempt?.failures || 0) + 1
    loginAttempts.set(key, {
      failures,
      blockedUntil: failures >= 5 ? now + 15 * 60 * 1000 : 0
    })
    res.status(401).json({ error: 'Usuário ou senha incorretos' })
  }
})

app.post('/api/logout', requireAuth, (req, res) => {
  const token = req.headers['x-session-token']
  sessions.delete(token)
  res.json({ ok: true })
})

// ── PROXY STATUS ─────────────────────────────────────────────────────────────
app.get('/api/proxy/status', requireAuth, (req, res) => {
  exec('pm2 jlist', (err, stdout) => {
    if (err) return probeProxyUrl((probeErr, probe) => res.json(probeErr ? { running: false, error: err.message } : probe))
    try {
      const list = JSON.parse(stdout)
      const proxy = list.find(p => p.name === 'iptv-proxy')
      if (!proxy) {
        return probeProxyUrl((probeErr, probe) => res.json(probe))
      }
      res.json({
        running: proxy.pm2_env.status === 'online',
        status: proxy.pm2_env.status,
        pid: proxy.pid,
        uptime: proxy.pm2_env.pm_uptime,
        restarts: proxy.pm2_env.restart_time,
        memory: proxy.monit?.memory || 0,
        cpu: proxy.monit?.cpu || 0,
      })
    } catch(e) { res.json({ running: false, error: e.message }) }
  })
})

app.post('/api/proxy/restart', requireAuth, (req, res) => {
  exec('pm2 restart iptv-proxy', (err) => {
    if (err) return res.json({ ok: false, error: err.message })
    res.json({ ok: true, message: 'Proxy reiniciado' })
  })
})

app.post('/api/proxy/stop', requireAuth, (req, res) => {
  exec('pm2 stop iptv-proxy', (err) => {
    if (err) return res.json({ ok: false, error: err.message })
    res.json({ ok: true, message: 'Proxy parado' })
  })
})

app.post('/api/proxy/start', requireAuth, (req, res) => {
  exec('pm2 start iptv-proxy', (err) => {
    if (err) return res.json({ ok: false, error: err.message })
    res.json({ ok: true, message: 'Proxy iniciado' })
  })
})

// ── PROXY LOGS ───────────────────────────────────────────────────────────────
app.get('/api/proxy/logs', requireAuth, (req, res) => {
  const lines = parseInt(req.query.lines) || 50
  exec(`pm2 logs iptv-proxy --lines ${lines} --nostream 2>&1 | tail -${lines}`, (err, stdout) => {
    if (err) {
      // Tenta ler o arquivo de log diretamente
      try {
        const logFile = `${process.env.HOME}/.pm2/logs/iptv-proxy-out.log`
        if (fs.existsSync(logFile)) {
          const content = fs.readFileSync(logFile, 'utf8')
          const lastLines = content.trim().split('\n').slice(-lines)
          return res.json({ logs: lastLines })
        }
      } catch(e) {}
      return res.json({ logs: ['Sem logs disponíveis'] })
    }
    const logLines = stdout.trim().split('\n').filter(Boolean)
    res.json({ logs: logLines.slice(-lines) })
  })
})

// ── SISTEMA ───────────────────────────────────────────────────────────────────
app.get('/api/system', requireAuth, (req, res) => {
  exec('free -m && df -h / && uptime', (err, stdout) => {
    const lines = stdout ? stdout.split('\n') : []
    let ramTotal = 0, ramUsed = 0, diskTotal = '', diskUsed = '', uptime = ''
    lines.forEach(l => {
      if (l.startsWith('Mem:')) {
        const p = l.trim().split(/\s+/)
        ramTotal = parseInt(p[1]); ramUsed = parseInt(p[2])
      }
      if (l.includes('/dev/') && l.includes('%')) {
        const p = l.trim().split(/\s+/)
        diskUsed = p[2]; diskTotal = p[1]
      }
      if (l.includes('load average')) uptime = l.trim()
    })
    res.json({ ramTotal, ramUsed, diskTotal, diskUsed, uptime })
  })
})

app.get('/api/config', requireAuth, (req, res) => {
  res.json({ config: panelConfig })
})

// XuiOne is read-only from this panel. Credentials remain server-side and are
// never included in these responses.
app.get('/api/xui/config', requireAuth, (req, res) => {
  res.json({ config: xuiCollector.getPublicConfig() })
})

app.get('/api/xui/overview', requireAuth, (req, res) => {
  res.json(xuiCollector.overview())
})

app.get('/api/xui/state', requireAuth, (req, res) => {
  const state = xuiCollector.getState()
  res.json({
    status: state.status,
    lastAttemptAt: state.lastAttemptAt,
    lastSuccessAt: state.lastSuccessAt,
    lastError: state.lastError,
    capabilities: state.capabilities,
    data: state.data
  })
})

app.post('/api/xui/sync', requireAuth, async (req, res) => {
  try {
    await xuiCollector.sync()
    res.json({ ok: true, overview: xuiCollector.overview() })
  } catch (error) {
    console.error('[xui] sync manual:', error.message)
    res.status(502).json({ ok: false, error: 'Falha ao sincronizar com o XuiOne' })
  }
})

function validSuperNodeFeedToken(value) {
  const expected = String(process.env.SUPERNODE_FEED_TOKEN || '')
  const supplied = String(value || '')
  if (!expected || supplied.length !== expected.length) return false
  return crypto.timingSafeEqual(Buffer.from(supplied), Buffer.from(expected))
}

// Feed privado consumido pelo Super Node. A resposta contém somente ID, nome
// e audiência agregada; nenhuma credencial ou dado de assinante sai do painel.
app.get('/api/supernode/top-channels', (req, res) => {
  const authorization = String(req.headers.authorization || '')
  const token = authorization.startsWith('Bearer ') ? authorization.slice(7) : req.headers['x-supernode-token']
  if (!validSuperNodeFeedToken(token)) return res.status(401).json({ error: 'Não autorizado' })
  const limit = Math.max(1, Math.min(10, Number(req.query.limit || 10)))
  const overview = xuiCollector.overview()
  res.setHeader('Cache-Control', 'no-store')
  res.json({
    generatedAt: new Date().toISOString(),
    source: 'xuione',
    status: overview.status,
    channels: (overview.topStreams || []).slice(0, limit).map(stream => ({
      channel_id: String(stream.id || ''),
      name: String(stream.name || ''),
      viewers: Number(stream.viewers || 0),
      online: stream.online === true
    }))
  })
})

app.get('/api/p2p/super-node', requireAuth, async (req, res) => {
  const base = String(process.env.SUPERNODE_STATUS_URL || '').replace(/\/+$/, '')
  if (!base) return res.json({ running: false, status: 'not_configured' })
  try {
    const result = await fetchJsonUrl(`${base}/status`, 5000)
    res.status(result.ok ? 200 : 502).json({
      running: result.ok,
      statusCode: result.statusCode,
      ...(result.json || {})
    })
  } catch (error) {
    res.status(502).json({ running: false, url: base, error: error.message })
  }
})

app.put('/api/config', requireAuth, (req, res) => {
  panelConfig = normalizeConfig(req.body || {})
  saveJson(CONFIG_FILE, panelConfig)
  res.json({ ok: true, config: panelConfig })
})

// Metadados públicos de atualização. O APK nunca recebe credenciais do painel.
app.get('/api/client/updates/:platform', (req, res) => {
  const platform = String(req.params.platform || '').trim()
  const channel = String(req.query.channel || 'production').trim()
  const currentVersionCode = Math.max(0, Number(req.query.versionCode || 0))
  if (!UPDATE_PLATFORMS.has(platform) || !UPDATE_CHANNELS.has(channel)) {
    return res.status(400).json({ error: 'Plataforma ou canal inválido' })
  }
  const release = appUpdates[updateKey(platform, channel)]
  res.setHeader('Cache-Control', 'no-store')
  if (!release || release.enabled === false) {
    return res.json({ updateAvailable: false, platform, channel })
  }
  const updateAvailable = release.versionCode > currentVersionCode
  res.json({
    updateAvailable,
    mandatory: updateAvailable && (
      release.mandatory === true ||
      currentVersionCode < Number(release.minimumVersionCode || 1)
    ),
    ...release
  })
})

app.get('/api/updates', requireAuth, (req, res) => {
  res.json({ releases: Object.values(appUpdates) })
})

app.put('/api/updates/:platform/:channel', requireAuth, (req, res) => {
  const platform = String(req.params.platform || '').trim()
  const channel = String(req.params.channel || '').trim()
  if (!UPDATE_PLATFORMS.has(platform) || !UPDATE_CHANNELS.has(channel)) {
    return res.status(400).json({ error: 'Plataforma ou canal inválido' })
  }
  try {
    const release = normalizeUpdateRelease(req.body, platform, channel)
    appUpdates[updateKey(platform, channel)] = release
    saveJson(UPDATES_FILE, appUpdates)
    res.json({ ok: true, release })
  } catch (error) {
    res.status(400).json({ error: error.message })
  }
})

app.post(
  '/api/updates/:platform/:channel/apk',
  requireAuth,
  express.raw({
    type: ['application/vnd.android.package-archive', 'application/octet-stream'],
    limit: '150mb'
  }),
  (req, res) => {
    const platform = String(req.params.platform || '').trim()
    const channel = String(req.params.channel || '').trim()
    if (!UPDATE_PLATFORMS.has(platform) || !UPDATE_CHANNELS.has(channel)) {
      return res.status(400).json({ error: 'Plataforma ou canal inválido' })
    }
    if (!Buffer.isBuffer(req.body) || req.body.length < 1024) {
      return res.status(400).json({ error: 'APK não recebido' })
    }
    try {
      const versionCode = Number(req.query.versionCode)
      const versionName = String(req.query.versionName || '').trim()
      const fileName = `${platform}-${channel}-${versionCode}.apk`
      const target = path.join(APK_DIR, fileName)
      fs.writeFileSync(target, req.body, { mode: 0o640 })
      const sha256 = crypto.createHash('sha256').update(req.body).digest('hex')
      const release = normalizeUpdateRelease({
        versionCode,
        versionName,
        minimumVersionCode: req.query.minimumVersionCode,
        mandatory: String(req.query.mandatory || '') === 'true',
        releaseNotes: req.query.releaseNotes,
        downloadUrl: `/downloads/${fileName}`,
        sha256,
        fileSize: req.body.length,
        enabled: true
      }, platform, channel)
      appUpdates[updateKey(platform, channel)] = release
      saveJson(UPDATES_FILE, appUpdates)
      res.status(201).json({ ok: true, release })
    } catch (error) {
      res.status(400).json({ error: error.message })
    }
  }
)

app.get('/api/client/config', (req, res) => {
  res.json({
    defaultDns: panelConfig.defaultDns,
    dnsServers: panelConfig.dnsServers,
    proxyUrl: panelConfig.proxyUrl,
    telemetryUrl: panelConfig.telemetryUrl,
    gatewayUrl: panelConfig.gatewayUrl,
    videoGatewayUrl: panelConfig.videoGatewayUrl,
    protectedGatewayUrl: panelConfig.protectedGatewayUrl,
    webPlayerUrl: panelConfig.webPlayerUrl,
    swarmCloud: {
      enabled: panelConfig.swarmCloudEnabled,
      url: panelConfig.swarmCloudUrl,
      sdkUrl: panelConfig.swarmCloudUrl,
      token: panelConfig.swarmCloudKey,
      appId: panelConfig.swarmCloudAppId,
      superPeerUrl: panelConfig.superPeerUrl
    },
    vpnServers: panelConfig.vpnServers,
    appVersion: panelConfig.appVersion,
    updateUrl: panelConfig.updateUrl,
    supportUrl: panelConfig.supportUrl
  })
})

app.get('/api/client/catalog/*', (req, res) => {
  const prefix = '/api/client/catalog'
  const catalogPath = req.path.slice(prefix.length) || '/'
  if (!allowedCatalogPath(catalogPath)) {
    return res.status(404).json({ ok: false, error: 'not_found' })
  }
  const queryString = (req.originalUrl.split('?')[1] || '').trim()
  pipeCatalogEmergency(req, res, catalogUpstreams(), catalogPath, queryString)
})

function contentEventRows(maxAgeMs = 30 * 24 * 60 * 60 * 1000) {
  const cutoff = Date.now() - maxAgeMs
  return reports.filter(report => {
    const receivedAt = Date.parse(report.receivedAt || '') || 0
    return receivedAt >= cutoff &&
      ['watch_content', 'content_progress', 'watch_channel'].includes(String(report.type || ''))
  })
}

function buildContentRanking(rows, limit = 24) {
  const ranked = new Map()
  for (const report of rows) {
    const contentType = String(report.contentType || (report.type === 'watch_channel' ? 'live' : '')).trim()
    const contentId = String(report.contentId || report.channelId || '').trim()
    const contentName = String(report.contentName || report.channelName || report.content || '').trim()
    if (!contentType || !contentId || !contentName) continue
    const key = `${contentType}:${contentId}`
    const current = ranked.get(key) || {
      contentType,
      contentId,
      contentName,
      category: String(report.contentCategory || report.channelCategory || '').trim(),
      image: String(report.contentImage || '').trim(),
      views: 0,
      completions: 0,
      progressTotal: 0,
      progressSamples: 0,
      lastWatchedAt: report.receivedAt || ''
    }
    if (report.type === 'watch_content' || report.type === 'watch_channel') current.views += 1
    const progress = Math.max(0, Math.min(100, Number(report.progressPct || 0)))
    if (progress > 0) {
      current.progressTotal += progress
      current.progressSamples += 1
      if (progress >= 85) current.completions += 1
    }
    if (String(report.receivedAt || '') > current.lastWatchedAt) current.lastWatchedAt = report.receivedAt
    if (!current.image && report.contentImage) current.image = String(report.contentImage)
    ranked.set(key, current)
  }
  return [...ranked.values()]
    .map(item => ({
      ...item,
      averageProgress: item.progressSamples
        ? Math.round(item.progressTotal / item.progressSamples)
        : 0,
      score: item.views * 10 + item.completions * 4 + Math.min(item.progressSamples, 20)
    }))
    .sort((a, b) => b.score - a.score || String(b.lastWatchedAt).localeCompare(String(a.lastWatchedAt)))
    .slice(0, limit)
}

app.get('/api/client/home', (req, res) => {
  const rows = contentEventRows()
  const ranking = buildContentRanking(rows)
  res.setHeader('Cache-Control', 'public, max-age=60, stale-while-revalidate=300')
  res.json({
    generatedAt: new Date().toISOString(),
    hot: ranking,
    movies: ranking.filter(item => item.contentType === 'vod').slice(0, 18),
    series: ranking.filter(item => item.contentType === 'series').slice(0, 18),
    live: ranking.filter(item => item.contentType === 'live').slice(0, 18)
  })
})

app.get('/api/client/commands', (req, res) => {
  const user = String(req.query.user || '').trim()
  if (!user) return res.json({ commands: [] })
  const now = Date.now()
  const commands = clientCommands.filter(c => c.user === user && !c.deliveredAt && (!c.expiresAt || c.expiresAt > now))
  if (commands.length) {
    const ids = new Set(commands.map(c => c.id))
    clientCommands = clientCommands.map(c => ids.has(c.id) ? { ...c, deliveredAt: now } : c)
    saveJson(COMMANDS_FILE, clientCommands)
  }
  res.json({ commands })
})

app.post('/api/client/speedtest', requireAuth, (req, res) => {
  const user = String(req.body?.user || '').trim()
  if (!user) return res.status(400).json({ error: 'Informe o login do cliente' })
  const command = {
    id: crypto.randomBytes(12).toString('hex'),
    type: 'speedtest',
    user,
    requestedBy: req.headers['x-session-token'] ? 'admin' : 'painel',
    createdAt: Date.now(),
    expiresAt: Date.now() + 10 * 60 * 1000
  }
  clientCommands.unshift(command)
  if (clientCommands.length > 300) clientCommands.splice(300)
  saveJson(COMMANDS_FILE, clientCommands)
  res.json({ ok: true, command })
})

// ── TELEMETRIA (recebe reports do IZ Play) ────────────────────────────────────
const reports = loadJson(REPORTS_FILE, [])
const p2pEvents = loadJson(P2P_FILE, [])
app.post('/api/telemetry/report', (req, res) => {
  const report = {
    ...sanitizeTelemetry(req.body || {}),
    ip: req.ip,
    receivedAt: new Date().toISOString()
  }
  reports.unshift(report)
  if (reports.length > 500) reports.splice(500)
  saveJson(REPORTS_FILE, reports)
  console.log('[telemetry] Report recebido:', report.type, report.user)
  res.json({ ok: true })
})

app.get('/api/telemetry/reports', requireAuth, (req, res) => {
  res.json({ reports: reports.slice(0, 100) })
})

app.get('/api/telemetry/content-ranking', requireAuth, (req, res) => {
  res.json({
    generatedAt: new Date().toISOString(),
    ranking: buildContentRanking(contentEventRows(), 100)
  })
})

app.post('/api/telemetry/p2p', (req, res) => {
  const body = req.body || {}
  const event = {
    user: String(body.user || '').slice(0, 80),
    deviceId: String(body.deviceId || body.sessionId || '').slice(0, 120),
    channel: String(body.channel || body.watching || '').slice(0, 160),
    channelId: String(body.channelId || '').slice(0, 80),
    swarmId: String(body.swarmId || '').slice(0, 120),
    peers: Math.max(0, Number(body.peers || 0)),
    p2pDown: Number(body.p2pDown || 0),
    p2pUp: Number(body.p2pUp || 0),
    httpDown: Number(body.httpDown || 0),
    speed: Number(body.speed || 0),
    active: body.active === true || body.active === 'true',
    reason: String(body.reason || '').slice(0, 120),
    token: body.token === true || body.token === 'true',
    appId: String(body.appId || '').slice(0, 120),
    platform: String(body.platform || '').slice(0, 80),
    userAgent: String(body.userAgent || '').slice(0, 240),
    ip: req.ip,
    receivedAt: new Date().toISOString(),
    lastSeen: Date.now()
  }
  p2pEvents.unshift(event)
  if (p2pEvents.length > 1000) p2pEvents.splice(1000)
  saveJson(P2P_FILE, p2pEvents)
  res.json({ ok: true })
})

app.get('/api/telemetry/p2p', requireAuth, async (req, res) => {
  const now = Date.now()
  const recent = p2pEvents.filter(e => now - Number(e.lastSeen || 0) < 5 * 60 * 1000)
  const todayKey = new Date().toISOString().slice(0, 10)
  const today = p2pEvents.filter(e => String(e.receivedAt || '').slice(0, 10) === todayKey)
  const sum = (arr, key) => arr.reduce((acc, e) => acc + Number(e[key] || 0), 0)
  const byChannel = new Map()
  for (const e of today) {
    const key = e.channel || e.channelId || 'Sem canal'
    const row = byChannel.get(key) || { channel: key, sessions: 0, p2pDown: 0, httpDown: 0 }
    row.sessions += 1
    row.p2pDown += Number(e.p2pDown || 0)
    row.httpDown += Number(e.httpDown || 0)
    byChannel.set(key, row)
  }
  let superNode = null
  const superNodeBase = String(process.env.SUPERNODE_STATUS_URL || '').replace(/\/+$/, '')
  if (superNodeBase) {
    try {
      const result = await fetchJsonUrl(`${superNodeBase}/status`, 4000)
      if (result.ok) superNode = result.json || null
    } catch (_) {}
  }
  const nodeChannels = Array.isArray(superNode?.channels) ? superNode.channels : []
  for (const channel of nodeChannels) {
    const key = channel.name || channel.channel_id || 'Sem canal'
    const row = byChannel.get(key) || { channel: key, sessions: 0, p2pDown: 0, httpDown: 0 }
    row.sessions += 1
    row.p2pDown += Number(channel.p2pDownKB || 0)
    row.httpDown += Number(channel.httpDownKB || 0)
    byChannel.set(key, row)
  }
  const nodeTotals = superNode?.totals || {}
  const p2pDown = sum(today, 'p2pDown') + Number(nodeTotals.p2pDownKB || 0)
  const httpDown = sum(today, 'httpDown') + Number(nodeTotals.httpDownKB || 0)
  const total = p2pDown + httpDown
  res.json({
    summary: {
      currentSessions: Number(nodeTotals.peers || 0)
        + recent.filter(e => Number(e.p2pDown || 0) > 0 || Number(e.p2pUp || 0) > 0).length,
      configuredSessions: recent.filter(e => e.active).length,
      seededChannels: Number(superNode?.activeChannels || 0),
      eventsToday: today.length,
      p2pDown,
      httpDown,
      efficiency: total ? Math.round((p2pDown / total) * 1000) / 10 : 0
    },
    recent: p2pEvents.slice(0, 100),
    superNode: superNode ? {
      running: superNode.status === 'running',
      status: superNode.status,
      activeChannels: Number(superNode.activeChannels || 0),
      peers: Number(nodeTotals.peers || 0),
      p2pUp: Number(nodeTotals.p2pUpKB || 0),
      p2pDown: Number(nodeTotals.p2pDownKB || 0),
      httpDown: Number(nodeTotals.httpDownKB || 0),
      system: superNode.system || {}
    } : { running: false },
    topChannels: [...byChannel.values()]
      .sort((a, b) => (b.p2pDown + b.httpDown) - (a.p2pDown + a.httpDown))
      .slice(0, 12)
  })
})

app.get('/api/p2p/super-peer', requireAuth, async (req, res) => {
  const base = String(panelConfig.superPeerUrl || '').replace(/\/+$/, '')
  if (!base) return res.json({ running: false, error: 'Super Peer não configurado' })
  try {
    const accessToken = String(process.env.SUPER_PEER_ACCESS_TOKEN || '')
    const result = await fetchJsonUrl(
      `${base}/stats`,
      3500,
      accessToken ? { 'x-access-token': accessToken } : {}
    )
    const stats = result.json || {}
    const master = stats.master || {}
    const workers = stats.workers || {}
    res.json({
      running: result.ok && !!master.version,
      statusCode: result.statusCode,
      url: base,
      version: master.version || '',
      workers: Object.keys(workers).length || Number(master.workers || 0) || 0,
      currentPeers: Number(master.currentPeers || 0),
      currentUplink: Number(master.currentUplink || 0),
      averageUplink: Number(master.averageUplink || 0),
      memory: Number(master.memory || 0),
      elapsed: Number(master.elapsed || 0),
      restarts: Number(master.restarts || 0),
      raw: stats
    })
  } catch (err) {
    res.json({ running: false, url: base, error: err.message })
  }
})

app.get('/api/vpn/status', requireAuth, async (req, res) => {
  const servers = Array.isArray(panelConfig.vpnServers) ? panelConfig.vpnServers : []
  const results = await Promise.all(servers.map(async server => {
    const base = String(server.url || '').replace(/\/+$/, '')
    try {
      const result = await fetchJsonUrl(`${base}/status`, 5000)
      const status = result.json || {}
      return {
        ...server,
        running: result.ok && status.ok !== false,
        statusCode: result.statusCode,
        online: Number(status.online || 0),
        totalPeers: Number(status.totalPeers || 0),
        listenPort: Number(status.listenPort || 0),
        checkedAt: status.checkedAt || null,
        peers: Array.isArray(status.peers) ? status.peers : []
      }
    } catch (err) {
      return {
        ...server,
        running: false,
        online: 0,
        totalPeers: 0,
        error: err.message
      }
    }
  }))

  res.json({
    ok: true,
    totalOnline: results.reduce((sum, server) => sum + Number(server.online || 0), 0),
    totalPeers: results.reduce((sum, server) => sum + Number(server.totalPeers || 0), 0),
    servers: results
  })
})

// Ping de telemetria: guarda multiplos dispositivos por login
function makeDeviceKey(d) {
  const user = String(d.user || 'sem-login').trim()
  const id = String(d.deviceId || d.sessionId || d.mac || d.platform || d.userAgent || 'device').trim()
  return user + '::' + id
}
const deviceMap = new Map(loadJson(DEVICES_FILE, []).map(d => [makeDeviceKey(d), d]))
app.post('/api/telemetry/ping', (req, res) => {
  const { user, mac, speed, dns, watching, version, localIp, proxyUrl, proxyLocalUrl, proxyLanUrl, serverDns, deviceId, sessionId, ...extra } = req.body
  if (user) {
    const current = {
      ...sanitizeTelemetry(extra),
      user: safeText(user, 80),
      deviceId: deviceId || sessionId || mac || (extra.webPlayer ? 'WEB-PLAYER' : ''),
      sessionId,
      mac,
      speed,
      dns,
      watching: safeText(watching, 200),
      version: safeText(version, 40),
      localIp: safeText(localIp, 80),
      proxyUrl: safeText(proxyUrl, 300),
      proxyLocalUrl: safeText(proxyLocalUrl, 300),
      proxyLanUrl: safeText(proxyLanUrl, 300),
      serverDns: safeText(serverDns, 300),
      ip: req.ip,
      lastSeen: Date.now()
    }
    deviceMap.set(makeDeviceKey(current), current)
    saveJson(DEVICES_FILE, [...deviceMap.values()])
  }
  res.json({ ok: true })
})

app.get('/api/devices', requireAuth, (req, res) => {
  const now = Date.now()
  const devices = [...deviceMap.values()].map(d => ({
    ...d,
    online: (now - d.lastSeen) < 5 * 60 * 1000 // online se visto nos últimos 5min
  }))
  res.json({ devices })
})

// ── START ─────────────────────────────────────────────────────────────────────
xuiCollector.start()

app.listen(PORT, () => {
  console.log(`[painel] Rodando na porta ${PORT}`)
  console.log(`[painel] Usuário: ${ADMIN_USER}`)
  console.log(`[painel] Acesse: http://SEU_IP/`)
})
