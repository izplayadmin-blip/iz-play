'use strict'

const http = require('http')
const https = require('https')
const { URL } = require('url')

const VERSION = '2.1.0'
const PORT = Number(process.env.CATALOG_GATEWAY_PORT || process.env.PORT || 3001)
const RAW_UPSTREAMS = String(
  process.env.CATALOG_UPSTREAMS ||
  process.env.XTREAM_BASE_URL ||
  process.env.DEFAULT_DNS ||
  'http://ortyu.online'
)
const UPSTREAMS = [...new Set(RAW_UPSTREAMS
  .split(',')
  .map(value => value.trim().replace(/\/+$/, ''))
  .filter(Boolean))]
const ALLOWED_ORIGINS = String(process.env.CATALOG_ALLOWED_ORIGINS || 'https://izplay.tv,https://www.izplay.tv,https://web.izplay.tv')
  .split(',')
  .map(value => value.trim())
  .filter(Boolean)

function originFor(req) {
  const origin = String(req.headers.origin || '')
  return ALLOWED_ORIGINS.includes(origin) ? origin : ALLOWED_ORIGINS[0] || '*'
}

function headers(req, extra = {}) {
  return {
    'Access-Control-Allow-Origin': originFor(req),
    'Access-Control-Allow-Methods': 'GET,OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type,Range',
    'Vary': 'Origin',
    'Cache-Control': 'no-store',
    'X-Content-Type-Options': 'nosniff',
    ...extra
  }
}

function send(req, res, status, body, extra = {}) {
  const payload = typeof body === 'string' ? body : JSON.stringify(body)
  res.writeHead(status, headers(req, {
    'Content-Type': typeof body === 'string' ? 'text/plain; charset=utf-8' : 'application/json; charset=utf-8',
    ...extra
  }))
  res.end(payload)
}

function allowedPath(pathname) {
  if (/^\/(?:live|movie|series)\//i.test(pathname)) return true
  return [
    '/player_api.php',
    '/get.php',
    '/xmltv.php',
    '/iptv.m3u'
  ].includes(pathname)
}

function targetUrl(upstream, parsed) {
  const target = new URL(upstream + parsed.pathname)
  parsed.searchParams.forEach((value, key) => target.searchParams.append(key, value))
  return target
}

function pipeUpstream(req, res, target, attemptNext) {
  const client = target.protocol === 'https:' ? https : http
  const upstreamReq = client.request(target, {
    method: 'GET',
    timeout: 18000,
    headers: {
      'User-Agent': 'Mozilla/5.0 IZPlay-Web/2.1',
      'Accept': req.headers.accept || '*/*'
    }
  }, upstreamRes => {
    if ([403, 404, 429, 500, 502, 503, 504].includes(upstreamRes.statusCode || 0)) {
      upstreamRes.resume()
      return attemptNext(new Error('upstream_unavailable'))
    }
    res.writeHead(upstreamRes.statusCode || 200, headers(req, {
      'Content-Type': upstreamRes.headers['content-type'] || 'application/json; charset=utf-8'
    }))
    upstreamRes.pipe(res)
  })
  upstreamReq.on('timeout', () => upstreamReq.destroy(new Error('upstream_timeout')))
  upstreamReq.on('error', attemptNext)
  req.on('close', () => upstreamReq.destroy())
  upstreamReq.end()
}

function proxyCatalog(req, res, parsed) {
  let index = 0
  const next = error => {
    if (res.headersSent || res.destroyed) return
    const upstream = UPSTREAMS[index++]
    if (!upstream) {
      return send(req, res, 502, {
        ok: false,
        error: 'catalog_gateway_unavailable',
        message: 'Gateway de catálogo indisponível',
        detail: error ? error.message : ''
      })
    }
    let target
    try {
      target = targetUrl(upstream, parsed)
    } catch (err) {
      return next(err)
    }
    pipeUpstream(req, res, target, next)
  }
  next()
}

http.createServer((req, res) => {
  const parsed = new URL(req.url, `http://${req.headers.host}`)
  if (req.method === 'OPTIONS') {
    res.writeHead(204, headers(req))
    return res.end()
  }
  if (parsed.pathname === '/health') {
    return send(req, res, 200, {
      ok: true,
      service: 'iz-play-catalog-gateway',
      version: VERSION,
      upstreams: UPSTREAMS.length
    })
  }
  if (req.method !== 'GET' || !allowedPath(parsed.pathname)) {
    return send(req, res, 404, { ok: false, error: 'not_found' })
  }
  proxyCatalog(req, res, parsed)
}).listen(PORT, '127.0.0.1', () => {
  console.log(`[catalog-gateway] IZ Play ${VERSION} em 127.0.0.1:${PORT}`)
})
