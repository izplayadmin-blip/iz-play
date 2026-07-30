const http = require('http')
const https = require('https')

const READ_ACTION_KEYS = ['connections', 'streams', 'servers', 'streamErrors']
const DEFAULT_ACTIONS = {
  connections: 'live_connections',
  streams: 'get_streams',
  servers: 'get_servers',
  streamErrors: 'stream_errors'
}

function loadXuiConfig(env = process.env) {
  const actions = {
    connections: String(env.XUI_ACTION_CONNECTIONS || DEFAULT_ACTIONS.connections).trim(),
    streams: String(env.XUI_ACTION_STREAMS || DEFAULT_ACTIONS.streams).trim(),
    servers: String(env.XUI_ACTION_SERVERS || DEFAULT_ACTIONS.servers).trim(),
    streamErrors: String(env.XUI_ACTION_STREAM_ERRORS || DEFAULT_ACTIONS.streamErrors).trim()
  }
  return {
    baseUrl: String(env.XUI_BASE_URL || '').trim().replace(/\/+$/, ''),
    accessCode: String(env.XUI_ACCESS_CODE || '').trim(),
    apiKey: String(env.XUI_API_KEY || '').trim(),
    allowInsecureHttp: env.XUI_ALLOW_INSECURE_HTTP === 'true',
    timeoutMs: Math.max(1000, Math.min(30000, Number(env.XUI_TIMEOUT_MS || 8000))),
    actions
  }
}

function publicConfig(config) {
  return {
    configured: Boolean(config.baseUrl && config.accessCode && config.apiKey),
    baseUrl: config.baseUrl ? new URL(config.baseUrl).origin : '',
    transport: config.baseUrl ? new URL(config.baseUrl).protocol.replace(':', '') : '',
    allowInsecureHttp: config.allowInsecureHttp,
    actions: Object.fromEntries(
      READ_ACTION_KEYS.map(key => [key, Boolean(config.actions[key])])
    )
  }
}

function validateConfig(config) {
  if (!config.baseUrl || !config.accessCode || !config.apiKey) {
    throw new Error('xui_not_configured')
  }
  const base = new URL(config.baseUrl)
  if (!['http:', 'https:'].includes(base.protocol)) throw new Error('xui_invalid_protocol')
  if (base.protocol !== 'https:' && !config.allowInsecureHttp) {
    throw new Error('xui_https_required')
  }
}

function requestAction(config, action) {
  validateConfig(config)
  if (!action || !Object.values(config.actions).includes(action)) {
    return Promise.reject(new Error('xui_action_not_allowed'))
  }
  const target = new URL(`${config.baseUrl}/${encodeURIComponent(config.accessCode)}/`)
  target.searchParams.set('api_key', config.apiKey)
  target.searchParams.set('action', action)
  // XuiOne's monitoring endpoints are paginated. Keep a bounded snapshot so a
  // busy main server cannot exhaust the panel process with an unbounded result.
  if (action !== config.actions.servers) {
    target.searchParams.set('start', '0')
    target.searchParams.set('limit', '1000')
  }
  const transport = target.protocol === 'https:' ? https : http

  return new Promise((resolve, reject) => {
    const req = transport.request(target, {
      method: 'GET',
      timeout: config.timeoutMs,
      headers: {
        Accept: 'application/json',
        'User-Agent': 'IZPlay-XUI-Collector/1.0'
      }
    }, response => {
      let body = ''
      response.setEncoding('utf8')
      response.on('data', chunk => {
        body += chunk
        if (body.length > 8 * 1024 * 1024) req.destroy(new Error('xui_response_too_large'))
      })
      response.on('end', () => {
        if (response.statusCode < 200 || response.statusCode >= 300) {
          return reject(new Error(`xui_http_${response.statusCode}`))
        }
        try {
          resolve(body ? JSON.parse(body) : {})
        } catch {
          reject(new Error('xui_invalid_json'))
        }
      })
    })
    req.on('timeout', () => req.destroy(new Error('xui_timeout')))
    req.on('error', reject)
    req.end()
  })
}

module.exports = { READ_ACTION_KEYS, loadXuiConfig, publicConfig, requestAction }
