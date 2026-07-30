'use strict'

// ─────────────────────────────────────────────────────────────────────────────
// IZ Play — Super Node P2P
// Peer PERMANENTE da malha SwarmCloud. Mantem os canais mais assistidos sempre
// conectados e redistribui os segmentos para acelerar a formacao da malha e a
// abertura dos canais. NAO transcodifica e NAO e um CDN — so semeia P2P.
//
// Como funciona (resumo):
//   1) a cada POLL_MS, consulta a lista de canais mais assistidos (TOP_CHANNELS_URL);
//   2) mantem 1 aba de Chromium headless por canal do Top-N (Puppeteer), cada uma
//      rodando hls.js + SDK SwarmCloud como um peer real (mesmo appId/token/zona
//      e mesmo channelId dos clientes);
//   3) canal que sai do Top-N por mais de DROP_GRACE_MS tem a aba fechada.
//
// Processo independente (Docker), pronto para escalar (Top 10 -> 50 -> 100) e
// para mover para outra VPS/regiao sem tocar no resto da arquitetura.
// ─────────────────────────────────────────────────────────────────────────────

const path = require('path')
const http = require('http')
const os = require('os')

const int = (v, d) => (Number.isFinite(Number(v)) && Number(v) > 0 ? Number(v) : d)

const cfg = {
  topN: int(process.env.SUPERNODE_TOP_N, 10),
  pilotChannelIds: String(process.env.SUPERNODE_PILOT_CHANNEL_IDS || '')
    .split(',').map(v => v.trim()).filter(Boolean),
  pollMs: int(process.env.SUPERNODE_POLL_MS, 60000),
  dropGraceMs: int(process.env.SUPERNODE_DROP_GRACE_MS, 300000), // 5 min
  // teto de seguranca: nunca abrir mais conexoes simultaneas que isto
  // (proteja o limite max_connections da sua conta do provedor!)
  maxConcurrent: int(process.env.SUPERNODE_MAX_CONCURRENT, 10),
  // fonte dos canais mais assistidos. Aceita:
  //  a) o formato do spec:            [{channel_id, name, viewers}]
  //  b) o /api/client/home do painel: {live:[{contentId, contentName, views}]}
  topChannelsUrl: process.env.SUPERNODE_TOP_CHANNELS_URL || 'http://127.0.0.1/gateway/top-channels',
  feedToken: process.env.SUPERNODE_FEED_TOKEN || '',
  // credenciais Xtream que o super node usa p/ ABRIR os streams (conta dedicada!)
  xtream: {
    host: (process.env.XTREAM_HOST || 'http://cxst.shop').replace(/\/+$/, ''),
    user: process.env.XTREAM_USER || '',
    pass: process.env.XTREAM_PASS || ''
  },
  // P2P so funciona em HLS segmentado -> use m3u8
  streamExt: (process.env.SUPERNODE_STREAM_EXT || 'ts').replace(/^\./, ''),
  mediaGatewayUrl: (process.env.SUPERNODE_MEDIA_GATEWAY_URL || '').replace(/\/+$/, ''),
  mediaGatewayToken: process.env.SUPERNODE_MEDIA_GATEWAY_TOKEN || '',
  // SwarmCloud: MESMOS valores dos clientes p/ entrar no MESMO swarm
  swarm: {
    appId: process.env.SWARM_APP_ID || 'web.izplay.tv',
    token: process.env.SWARM_TOKEN || '',
    trackerZone: process.env.SWARM_TRACKER_ZONE || 'us'
  },
  sdkUrl: process.env.SUPERNODE_SDK_URL || 'https://web.izplay.tv/player/vendor/swarmcloud-hls.min.js',
  headless: (process.env.SUPERNODE_HEADLESS || 'true') !== 'false',
  chromePath: process.env.PUPPETEER_EXECUTABLE_PATH || undefined,
  // endpoint HTTP de status/metricas p/ monitoramento
  statusPort: int(process.env.SUPERNODE_STATUS_PORT, 9099)
}

// channel_id -> { page, name, since, lastTop }
const active = new Map()
let browser = null
let stopping = false
let serviceStatus = 'starting'
let lastFeedAt = null
let lastFeedError = ''

function log(...a) { console.log(new Date().toISOString(), '[supernode]', ...a) }

// Normaliza a resposta da fonte de top-channels para [{channel_id, name, viewers}]
function normalizeTop(data) {
  const rows = Array.isArray(data) ? data : (data && Array.isArray(data.live) ? data.live : [])
  return rows
    .map(r => ({
      channel_id: String(r.channel_id ?? r.contentId ?? r.id ?? '').trim(),
      name: String(r.name ?? r.contentName ?? '').trim(),
      viewers: Number(r.viewers ?? r.views ?? 0)
    }))
    .filter(r => r.channel_id)
}

async function fetchTopChannels() {
  const ctrl = new AbortController()
  const timer = setTimeout(() => ctrl.abort(), 8000)
  try {
    const headers = { 'Accept': 'application/json' }
    if (cfg.feedToken) headers.Authorization = `Bearer ${cfg.feedToken}`
    const res = await fetch(cfg.topChannelsUrl, { signal: ctrl.signal, headers })
    if (!res.ok) throw new Error('HTTP ' + res.status)
    const payload = await res.json()
    lastFeedAt = new Date().toISOString()
    lastFeedError = ''
    return normalizeTop(Array.isArray(payload) ? payload : payload.channels || payload)
  } finally {
    clearTimeout(timer)
  }
}

// URL HLS direta do provedor para um channel_id (P2P precisa de segmentos m3u8)
function streamUrl(channelId) {
  const u = encodeURIComponent(cfg.xtream.user)
  const p = encodeURIComponent(cfg.xtream.pass)
  const direct = `${cfg.xtream.host}/live/${u}/${p}/${encodeURIComponent(channelId)}.${cfg.streamExt}`
  if (!cfg.mediaGatewayUrl) return direct
  const gateway = new URL(`${cfg.mediaGatewayUrl}/proxy`)
  gateway.searchParams.set('url', direct)
  if (cfg.mediaGatewayToken) gateway.searchParams.set('key', cfg.mediaGatewayToken)
  return gateway.toString()
}

function peerPageUrl(ch) {
  const params = new URLSearchParams({
    src: streamUrl(ch.channel_id),
    channelId: String(ch.channel_id),
    appId: cfg.swarm.appId,
    token: cfg.swarm.token,
    zone: cfg.swarm.trackerZone,
    sdk: cfg.sdkUrl
  })
  return 'file://' + path.join(__dirname, 'peer.html') + '?' + params.toString()
}

async function openChannel(ch) {
  const id = String(ch.channel_id)
  if (active.has(id)) return
  if (active.size >= cfg.maxConcurrent) {
    log('teto de conexoes atingido (' + cfg.maxConcurrent + '); adiando canal', id, ch.name)
    return
  }
  try {
    const page = await browser.newPage()
    page.on('console', msg => {
      const t = msg.text()
      const s = active.get(id)
      if (!s) return
      let m
      if ((m = t.match(/p2pDown=(\d+)KB up=(\d+)KB http=(\d+)KB/))) { s.p2pDown = +m[1]; s.p2pUp = +m[2]; s.httpDown = +m[3] }
      else if ((m = t.match(/peers=(\d+)/))) { s.peers = +m[1] }
      if (/erro|fatal|falha/i.test(t)) log('canal ' + id + ':', t)
    })
    await page.goto(peerPageUrl(ch), { waitUntil: 'domcontentloaded', timeout: 30000 }).catch(() => {})
    active.set(id, { page, name: ch.name, since: Date.now(), lastTop: Date.now(), peers: 0, p2pDown: 0, p2pUp: 0, httpDown: 0 })
    log('canal LIGADO', id, ch.name, '(' + ch.viewers + ' viewers)')
  } catch (e) {
    log('falha ao abrir canal', id, e.message)
  }
}

async function closeChannel(id) {
  const s = active.get(id)
  if (!s) return
  active.delete(id)
  try { await s.page.close() } catch (e) {}
  log('canal LIBERADO', id, s.name)
}

async function tick() {
  if (stopping) return
  let top
  try {
    top = await fetchTopChannels()
  } catch (e) {
    lastFeedError = e.message
    log('erro ao buscar top-channels:', e.message)
    return
  }
  const wanted = cfg.pilotChannelIds.length
    ? cfg.pilotChannelIds.map(id =>
        top.find(channel => String(channel.channel_id) === id)
        || { channel_id: id, name: `Canal ${id}`, viewers: 0 })
    : top.slice(0, cfg.topN)
  const wantedIds = new Set(wanted.map(c => String(c.channel_id)))

  // liga/renova os canais do Top-N
  for (const ch of wanted) {
    const id = String(ch.channel_id)
    if (active.has(id)) active.get(id).lastTop = Date.now()
    else await openChannel(ch)
  }

  // libera os que sairam do Top-N ha mais de DROP_GRACE_MS
  for (const [id, s] of active) {
    if (!wantedIds.has(id) && Date.now() - s.lastTop > cfg.dropGraceMs) await closeChannel(id)
  }

  log(`ativos=${active.size}/${cfg.topN} top=[${wanted.map(c => c.channel_id).join(',')}]`)
}

// ── STATUS / METRICAS (endpoint HTTP p/ monitoramento) ──
let lastCpu = process.cpuUsage()
let lastCpuAt = Date.now()
function systemMetrics() {
  const cpu = process.cpuUsage(lastCpu)
  const dtUs = (Date.now() - lastCpuAt) * 1000
  lastCpu = process.cpuUsage()
  lastCpuAt = Date.now()
  const cpuPct = dtUs > 0 ? Math.min(100, Math.round(((cpu.user + cpu.system) / dtUs) * 100)) : 0
  const mem = process.memoryUsage()
  return {
    // Obs: cpu/ram aqui refletem o PROCESSO Node. As abas do Chromium sao processos
    // filhos e nao entram nessa conta — para o total do host use hostFreeMB/loadavg.
    cpuPercentProc: cpuPct,
    procRssMB: Math.round(mem.rss / 1048576),
    hostTotalMB: Math.round(os.totalmem() / 1048576),
    hostFreeMB: Math.round(os.freemem() / 1048576),
    loadavg: os.loadavg().map(n => Math.round(n * 100) / 100)
  }
}
function statusPayload() {
  const channels = [...active.entries()].map(([id, s]) => ({
    channel_id: id,
    name: s.name,
    peers: s.peers || 0,
    p2pDownKB: s.p2pDown || 0,
    p2pUpKB: s.p2pUp || 0,
    httpDownKB: s.httpDown || 0,
    uptimeSec: Math.round((Date.now() - s.since) / 1000)
  }))
  const totals = channels.reduce((a, c) => ({
    peers: a.peers + c.peers,
    p2pDownKB: a.p2pDownKB + c.p2pDownKB,
    p2pUpKB: a.p2pUpKB + c.p2pUpKB,
    httpDownKB: a.httpDownKB + c.httpDownKB,
    uploadKB: a.uploadKB + c.p2pUpKB,
    downloadKB: a.downloadKB + c.p2pDownKB + c.httpDownKB
  }), { peers: 0, p2pDownKB: 0, p2pUpKB: 0, httpDownKB: 0, uploadKB: 0, downloadKB: 0 })
  return {
    service: 'izplay-supernode',
    version: '0.1.0',
    ts: new Date().toISOString(),
    status: serviceStatus,
    config: { topN: cfg.topN, maxConcurrent: cfg.maxConcurrent, pollMs: cfg.pollMs },
    feed: { lastSuccessAt: lastFeedAt, lastError: lastFeedError },
    activeChannels: active.size,
    totals,
    channels,
    system: systemMetrics()
  }
}
function startStatusServer() {
  http.createServer((req, res) => {
    const url = (req.url || '').split('?')[0]
    if (url === '/healthz') { res.writeHead(200, { 'Content-Type': 'application/json' }); return res.end('{"ok":true}') }
    if (url === '/status' || url === '/') {
      res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' })
      return res.end(JSON.stringify(statusPayload(), null, 2))
    }
    res.writeHead(404); res.end()
  }).listen(cfg.statusPort, () => log('status HTTP em :' + cfg.statusPort + ' (/status, /healthz)'))
}

async function main() {
  startStatusServer()
  if (!cfg.xtream.user || !cfg.xtream.pass) {
    serviceStatus = 'waiting_credentials'
    log('AGUARDANDO: defina XTREAM_USER e XTREAM_PASS de uma conta dedicada com 10 conexoes.')
    return
  }
  if (!cfg.swarm.token) log('AVISO: SWARM_TOKEN vazio — o P2P pode nao parear sem token.')

  // Lazy load: o coordenador pode expor status sem consumir Chromium enquanto
  // aguarda capacidade e uma conta Xtream dedicada.
  const puppeteer = require('puppeteer')
  browser = await puppeteer.launch({
    headless: cfg.headless ? 'new' : false,
    executablePath: cfg.chromePath,
    args: [
      '--no-sandbox',
      '--disable-setuid-sandbox',
      '--disable-dev-shm-usage',
      '--disable-web-security',
      '--allow-file-access-from-files',
      '--autoplay-policy=no-user-gesture-required',
      '--disable-gpu',
      '--mute-audio'
    ]
  })
  serviceStatus = 'running'
  log(`super node iniciado. topN=${cfg.topN} poll=${cfg.pollMs}ms grace=${cfg.dropGraceMs}ms host=${cfg.xtream.host}`)
  await tick()
  const loop = setInterval(tick, cfg.pollMs)
  const shutdown = async () => {
    stopping = true
    clearInterval(loop)
    log('encerrando...')
    try { await browser.close() } catch (e) {}
    process.exit(0)
  }
  process.on('SIGTERM', shutdown)
  process.on('SIGINT', shutdown)
}

main().catch(e => { log('fatal', e && e.stack || e); process.exit(1) })
