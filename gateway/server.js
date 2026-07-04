'use strict'

const http = require('http')
const https = require('https')
const dns = require('dns').promises
const net = require('net')
const os = require('os')
const fs = require('fs')
const path = require('path')
const crypto = require('crypto')
const { spawn } = require('child_process')
const { URL } = require('url')

const VERSION = '2.1.0'
const PORT = Number(process.env.GATEWAY_PORT || process.env.PORT || 4100)
const TOKEN = String(process.env.GATEWAY_TOKEN || '')
const ALLOWED_ORIGINS = String(process.env.GATEWAY_ALLOWED_ORIGINS || 'https://izplay.tv')
  .split(',').map(value => value.trim()).filter(Boolean)
const ALLOWED_HOSTS = String(process.env.GATEWAY_ALLOWED_HOSTS || '')
  .split(',').map(value => value.trim().toLowerCase()).filter(Boolean)
const HLS_ROOT = process.env.GATEWAY_HLS_ROOT || process.env.HLS_ROOT || '/tmp/iz-gateway-hls'
const SESSION_TTL = Number(process.env.SESSION_TTL_MS || 10 * 60 * 1000)
const START_TIMEOUT = Number(process.env.START_TIMEOUT_MS || 25000)
const MAX_TRANSCODES = Math.max(1, Number(process.env.GATEWAY_MAX_TRANSCODES || 2))
const VPN_INTERFACE = String(process.env.VPN_INTERFACE || 'wg0')
const VPN_LABEL = String(process.env.VPN_LABEL || 'Rota protegida')
const FFMPEG_USER_AGENT = process.env.FFMPEG_USER_AGENT || 'Mozilla/5.0 IZPlay/1.3'
const sessions = new Map()
const rateBuckets = new Map()

if (!TOKEN && process.env.NODE_ENV === 'production') {
  throw new Error('GATEWAY_TOKEN é obrigatório em produção')
}

function originFor(req) {
  const origin = String(req.headers.origin || '')
  return ALLOWED_ORIGINS.includes(origin) ? origin : ALLOWED_ORIGINS[0] || 'null'
}

function commonHeaders(req) {
  return {
    'Access-Control-Allow-Origin': originFor(req),
    'Access-Control-Allow-Methods': 'GET,OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type,Range,X-IZ-Gateway-Token',
    'Vary': 'Origin',
    'Cache-Control': 'no-store',
    'X-Content-Type-Options': 'nosniff'
  }
}

function send(req, res, status, body, headers = {}) {
  const payload = typeof body === 'string' ? body : JSON.stringify(body)
  res.writeHead(status, {
    ...commonHeaders(req),
    'Content-Type': typeof body === 'string'
      ? 'text/plain; charset=utf-8'
      : 'application/json; charset=utf-8',
    ...headers
  })
  res.end(payload)
}

function timingSafeToken(value) {
  if (!TOKEN) return process.env.NODE_ENV !== 'production'
  const supplied = Buffer.from(String(value || ''))
  const expected = Buffer.from(TOKEN)
  return supplied.length === expected.length && crypto.timingSafeEqual(supplied, expected)
}

function authorized(req, parsed) {
  return timingSafeToken(req.headers['x-iz-gateway-token'] || parsed.searchParams.get('key'))
}

function clientKey(req) {
  return String(req.headers['cf-connecting-ip'] || req.headers['x-forwarded-for'] || req.socket.remoteAddress || '')
    .split(',')[0].trim()
}

function rateAllowed(req, limit = 180, windowMs = 60000) {
  const now = Date.now()
  const key = clientKey(req)
  const bucket = rateBuckets.get(key)
  if (!bucket || now - bucket.startedAt >= windowMs) {
    rateBuckets.set(key, { startedAt: now, count: 1 })
    return true
  }
  bucket.count += 1
  return bucket.count <= limit
}

function isPrivateIp(ip) {
  if (net.isIPv4(ip)) {
    const parts = ip.split('.').map(Number)
    return parts[0] === 10 ||
      parts[0] === 127 ||
      parts[0] === 0 ||
      (parts[0] === 169 && parts[1] === 254) ||
      (parts[0] === 172 && parts[1] >= 16 && parts[1] <= 31) ||
      (parts[0] === 192 && parts[1] === 168) ||
      (parts[0] === 100 && parts[1] >= 64 && parts[1] <= 127) ||
      parts[0] >= 224
  }
  if (net.isIPv6(ip)) {
    const normalized = ip.toLowerCase()
    return normalized === '::1' ||
      normalized === '::' ||
      normalized.startsWith('fc') ||
      normalized.startsWith('fd') ||
      normalized.startsWith('fe8') ||
      normalized.startsWith('fe9') ||
      normalized.startsWith('fea') ||
      normalized.startsWith('feb')
  }
  return true
}

function hostAllowed(hostname) {
  if (!ALLOWED_HOSTS.length) return true
  const host = hostname.toLowerCase()
  return ALLOWED_HOSTS.some(allowed => host === allowed || host.endsWith(`.${allowed}`))
}

async function safeTarget(parsed) {
  const raw = parsed.searchParams.get('url') || parsed.searchParams.get('u')
  if (!raw || raw.length > 4096) throw new Error('missing_or_invalid_url')
  const target = new URL(raw)
  if (!['http:', 'https:'].includes(target.protocol)) throw new Error('invalid_protocol')
  if (target.username || target.password) throw new Error('userinfo_not_allowed')
  if (!hostAllowed(target.hostname)) throw new Error('host_not_allowed')
  const addresses = await dns.lookup(target.hostname, { all: true, verbatim: true })
  if (!addresses.length || addresses.some(item => isPrivateIp(item.address))) {
    throw new Error('private_destination_blocked')
  }
  return target
}

function publicBase(req) {
  const proto = String(req.headers['x-forwarded-proto'] || 'http').split(',')[0]
  const host = String(req.headers['x-forwarded-host'] || req.headers.host || '')
  const prefix = String(req.headers['x-forwarded-prefix'] || '').trim().replace(/\/+$/, '')
  return `${proto}://${host}${prefix.startsWith('/') ? prefix : ''}`
}

function withToken(url, parsed) {
  const key = parsed.searchParams.get('key')
  return key ? `${url}&key=${encodeURIComponent(key)}` : url
}

function rewritePlaylist(text, baseUrl, req, parsed) {
  return text.split(/\r?\n/).map(line => {
    const trimmed = line.trim()
    if (!trimmed || trimmed.startsWith('#')) return line
    try {
      const absolute = new URL(trimmed, baseUrl).toString()
      return withToken(`${publicBase(req)}/proxy?url=${encodeURIComponent(absolute)}`, parsed)
    } catch (_) {
      return line
    }
  }).join('\n')
}

function requestRemote(target, req, onResponse, onError, redirects = 0) {
  const client = target.protocol === 'https:' ? https : http
  const headers = {
    'User-Agent': FFMPEG_USER_AGENT,
    'Accept': req.headers.accept || '*/*'
  }
  if (req.headers.range) headers.Range = req.headers.range
  const upstream = client.request(target, { method: 'GET', headers, timeout: 20000 }, response => {
    if ([301, 302, 303, 307, 308].includes(response.statusCode) && response.headers.location && redirects < 4) {
      response.resume()
      const next = new URL(response.headers.location, target)
      return safeResolvedRedirect(next)
        .then(() => requestRemote(next, req, onResponse, onError, redirects + 1))
        .catch(onError)
    }
    onResponse(response)
  })
  upstream.on('timeout', () => upstream.destroy(new Error('upstream_timeout')))
  upstream.on('error', onError)
  req.on('close', () => upstream.destroy())
  upstream.end()
}

async function safeResolvedRedirect(target) {
  if (!['http:', 'https:'].includes(target.protocol) || !hostAllowed(target.hostname)) {
    throw new Error('redirect_not_allowed')
  }
  const addresses = await dns.lookup(target.hostname, { all: true, verbatim: true })
  if (!addresses.length || addresses.some(item => isPrivateIp(item.address))) {
    throw new Error('redirect_private_destination')
  }
}

async function handleProxy(req, res, parsed) {
  if (!authorized(req, parsed)) return send(req, res, 401, { ok: false, error: 'unauthorized' })
  let target
  try { target = await safeTarget(parsed) } catch (error) {
    return send(req, res, 400, { ok: false, error: error.message })
  }

  requestRemote(target, req, upstream => {
    const contentType = String(upstream.headers['content-type'] || '')
    const isPlaylist = contentType.includes('mpegurl') || target.pathname.toLowerCase().includes('.m3u8')
    if (!isPlaylist) {
      res.writeHead(upstream.statusCode || 200, {
        ...commonHeaders(req),
        'Accept-Ranges': upstream.headers['accept-ranges'] || 'bytes',
        'Content-Type': contentType || 'application/octet-stream',
        ...(upstream.headers['content-length'] ? { 'Content-Length': upstream.headers['content-length'] } : {}),
        ...(upstream.headers['content-range'] ? { 'Content-Range': upstream.headers['content-range'] } : {})
      })
      return upstream.pipe(res)
    }
    const chunks = []
    let size = 0
    upstream.on('data', chunk => {
      size += chunk.length
      if (size <= 4 * 1024 * 1024) chunks.push(chunk)
      else upstream.destroy(new Error('playlist_too_large'))
    })
    upstream.on('end', () => {
      const rewritten = rewritePlaylist(Buffer.concat(chunks).toString('utf8'), target, req, parsed)
      res.writeHead(200, {
        ...commonHeaders(req),
        'Content-Type': 'application/vnd.apple.mpegurl; charset=utf-8'
      })
      res.end(rewritten)
    })
  }, error => send(req, res, 502, { ok: false, error: 'upstream_error', detail: error.message }))
}

async function handleProbe(req, res, parsed) {
  if (!authorized(req, parsed)) return send(req, res, 401, { ok: false, error: 'unauthorized' })
  let target
  try { target = await safeTarget(parsed) } catch (error) {
    return send(req, res, 400, { ok: false, error: error.message })
  }
  const args = ['-v', 'quiet', '-print_format', 'json', '-show_format', '-show_streams', target.toString()]
  const child = spawn('ffprobe', args, { timeout: 25000 })
  let out = ''
  let stderr = ''
  child.stdout.on('data', data => { out += data })
  child.stderr.on('data', data => { stderr = (stderr + data).slice(-1000) })
  child.on('close', code => {
    if (code !== 0) return send(req, res, 422, { ok: false, error: 'probe_failed', detail: stderr })
    try {
      const data = JSON.parse(out)
      send(req, res, 200, {
        ok: true,
        streams: (data.streams || []).map(stream => ({
          type: stream.codec_type,
          codec: stream.codec_name,
          profile: stream.profile,
          width: stream.width,
          height: stream.height,
          channels: stream.channels
        })),
        format: {
          name: data.format?.format_name,
          duration: data.format?.duration,
          bitRate: data.format?.bit_rate
        }
      })
    } catch (_) {
      send(req, res, 500, { ok: false, error: 'probe_parse_failed' })
    }
  })
}

function sessionId(raw) {
  return crypto.createHash('sha256').update(raw).digest('hex').slice(0, 20)
}

function removeDir(dir) {
  try { fs.rmSync(dir, { recursive: true, force: true }) } catch (_) {}
}

async function handleTranscode(req, res, parsed) {
  if (!authorized(req, parsed)) return send(req, res, 401, { ok: false, error: 'unauthorized' })
  let target
  try { target = await safeTarget(parsed) } catch (error) {
    return send(req, res, 400, { ok: false, error: error.message })
  }
  const id = sessionId(target.toString())
  let session = sessions.get(id)
  if (!session && sessions.size >= MAX_TRANSCODES) {
    return send(req, res, 503, { ok: false, error: 'transcode_capacity_reached' })
  }

  fs.mkdirSync(HLS_ROOT, { recursive: true })
  const dir = path.join(HLS_ROOT, id)
  const playlist = path.join(dir, 'index.m3u8')
  if (!session || session.process.exitCode !== null || session.process.killed) {
    removeDir(dir)
    fs.mkdirSync(dir, { recursive: true })
    const args = [
      '-hide_banner', '-loglevel', 'warning',
      '-user_agent', FFMPEG_USER_AGENT,
      '-reconnect', '1', '-reconnect_streamed', '1', '-reconnect_delay_max', '5',
      '-i', target.toString(),
      '-map', '0:v:0?', '-map', '0:a:0?',
      '-c:v', 'libx264', '-preset', 'veryfast', '-tune', 'zerolatency',
      '-pix_fmt', 'yuv420p', '-profile:v', 'main', '-level', '4.0',
      '-c:a', 'aac', '-ac', '2', '-ar', '48000', '-b:a', '128k',
      '-f', 'hls', '-hls_time', '4', '-hls_list_size', '10',
      '-hls_flags', 'delete_segments+append_list+omit_endlist',
      '-hls_segment_filename', path.join(dir, 'seg_%05d.ts'),
      playlist
    ]
    const child = spawn('ffmpeg', args, { stdio: ['ignore', 'ignore', 'pipe'] })
    session = { process: child, startedAt: Date.now(), lastAccess: Date.now(), log: '' }
    child.stderr.on('data', data => { session.log = (session.log + data.toString()).slice(-4000) })
    child.on('close', () => {
      const current = sessions.get(id)
      if (current?.process === child) sessions.delete(id)
    })
    sessions.set(id, session)
  }
  session.lastAccess = Date.now()

  const startedAt = Date.now()
  const wait = () => {
    try {
      if (fs.existsSync(playlist) && /seg_\d+\.ts/.test(fs.readFileSync(playlist, 'utf8'))) {
        const location = withToken(`${publicBase(req)}/hls/${id}/index.m3u8?x=1`, parsed).replace('?x=1&', '?').replace('?x=1', '')
        res.writeHead(302, { ...commonHeaders(req), Location: location })
        return res.end()
      }
    } catch (_) {}
    if (Date.now() - startedAt > START_TIMEOUT) {
      return send(req, res, 504, { ok: false, error: 'transcode_start_timeout' })
    }
    setTimeout(wait, 350)
  }
  wait()
}

function handleHls(req, res, parsed) {
  if (!authorized(req, parsed)) return send(req, res, 401, { ok: false, error: 'unauthorized' })
  const parts = parsed.pathname.split('/').filter(Boolean)
  const id = parts[1]
  const file = parts.slice(2).join('/')
  if (!/^[a-f0-9]{20}$/.test(id || '') || !/^[a-zA-Z0-9_.-]+$/.test(file || '')) {
    return send(req, res, 404, { ok: false, error: 'not_found' })
  }
  const fullPath = path.join(HLS_ROOT, id, file)
  if (!fs.existsSync(fullPath)) return send(req, res, 404, { ok: false, error: 'not_found' })
  const session = sessions.get(id)
  if (session) session.lastAccess = Date.now()
  res.writeHead(200, {
    ...commonHeaders(req),
    'Content-Type': file.endsWith('.m3u8') ? 'application/vnd.apple.mpegurl' : 'video/mp2t'
  })
  fs.createReadStream(fullPath).pipe(res)
}

function vpnStatus() {
  const interfaces = os.networkInterfaces()
  const addresses = interfaces[VPN_INTERFACE] || []
  return {
    enabled: addresses.length > 0,
    interface: VPN_INTERFACE,
    label: VPN_LABEL,
    addresses: addresses.map(item => item.address)
  }
}

setInterval(() => {
  const now = Date.now()
  for (const [id, session] of sessions) {
    if (now - session.lastAccess > SESSION_TTL) {
      try { session.process.kill('SIGTERM') } catch (_) {}
      sessions.delete(id)
      removeDir(path.join(HLS_ROOT, id))
    }
  }
  for (const [key, bucket] of rateBuckets) {
    if (now - bucket.startedAt > 120000) rateBuckets.delete(key)
  }
}, 60000).unref()

const server = http.createServer(async (req, res) => {
  let parsed
  try { parsed = new URL(req.url, `http://${req.headers.host || 'localhost'}`) } catch (_) {
    return send(req, res, 400, { ok: false, error: 'bad_request' })
  }
  if (req.method === 'OPTIONS') return send(req, res, 204, '')
  if (!rateAllowed(req)) return send(req, res, 429, { ok: false, error: 'rate_limited' })
  if (parsed.pathname === '/health') {
    return send(req, res, 200, {
      ok: true,
      service: 'iz-play-media-gateway',
      version: VERSION,
      sessions: sessions.size,
      maxTranscodes: MAX_TRANSCODES,
      vpn: vpnStatus()
    })
  }
  if (parsed.pathname === '/route/health') return send(req, res, 200, { ok: true, vpn: vpnStatus() })
  if (parsed.pathname === '/proxy') return handleProxy(req, res, parsed)
  if (parsed.pathname === '/probe') return handleProbe(req, res, parsed)
  if (parsed.pathname === '/transcode') return handleTranscode(req, res, parsed)
  if (parsed.pathname.startsWith('/hls/')) return handleHls(req, res, parsed)
  return send(req, res, 404, { ok: false, error: 'not_found' })
})

server.listen(PORT, '127.0.0.1', () => {
  console.log(`[gateway] IZ Play ${VERSION} em 127.0.0.1:${PORT}`)
})
