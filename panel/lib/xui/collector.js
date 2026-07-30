const fs = require('fs')
const path = require('path')
const { loadXuiConfig, publicConfig, requestAction } = require('./client')
const { normalizeConnections, normalizeStreams, normalizeServers, normalizeErrors } = require('./normalizer')

const NORMALIZERS = {
  connections: normalizeConnections,
  streams: normalizeStreams,
  servers: normalizeServers,
  streamErrors: normalizeErrors
}

function createCollector({ dataDir, intervalMs = 60000 }) {
  const stateFile = path.join(dataDir, 'xui-state.json')
  let state = loadState()
  let timer = null
  let running = false

  function emptyState() {
    return {
      status: 'not_configured',
      lastAttemptAt: null,
      lastSuccessAt: null,
      lastError: '',
      capabilities: {},
      data: { connections: [], streams: [], servers: [], streamErrors: [] }
    }
  }

  function loadState() {
    try {
      return fs.existsSync(stateFile)
        ? { ...emptyState(), ...JSON.parse(fs.readFileSync(stateFile, 'utf8')) }
        : emptyState()
    } catch {
      return emptyState()
    }
  }

  function persist() {
    const temporary = `${stateFile}.tmp`
    fs.writeFileSync(temporary, JSON.stringify(state, null, 2))
    fs.renameSync(temporary, stateFile)
  }

  async function sync() {
    if (running) return { ...state, skipped: 'already_running' }
    running = true
    const config = loadXuiConfig()
    state.lastAttemptAt = new Date().toISOString()
    try {
      if (!publicConfig(config).configured) {
        state.status = 'not_configured'
        state.lastError = ''
        persist()
        return state
      }
      const configured = Object.entries(config.actions).filter(([, action]) => action)
      if (!configured.length) {
        state.status = 'actions_not_configured'
        state.lastError = ''
        persist()
        return state
      }
      const results = await Promise.all(configured.map(async ([key, action]) => {
        try {
          const payload = await requestAction(config, action)
          return { key, ok: true, rows: NORMALIZERS[key](payload) }
        } catch (error) {
          return { key, ok: false, error: error.message }
        }
      }))
      for (const result of results) {
        state.capabilities[result.key] = {
          configured: true,
          available: result.ok,
          checkedAt: state.lastAttemptAt,
          error: result.ok ? '' : result.error
        }
        if (result.ok) state.data[result.key] = result.rows
      }
      const successes = results.filter(result => result.ok).length
      state.status = successes === results.length ? 'online' : successes ? 'partial' : 'error'
      state.lastError = successes ? '' : results.map(result => result.error).filter(Boolean).join(', ')
      if (successes) state.lastSuccessAt = state.lastAttemptAt
      persist()
      return state
    } finally {
      running = false
    }
  }

  function overview() {
    const connections = state.data.connections || []
    const streams = state.data.streams || []
    const servers = state.data.servers || []
    const errors = state.data.streamErrors || []
    const viewersByStream = new Map()
    for (const connection of connections) {
      viewersByStream.set(connection.streamKey, (viewersByStream.get(connection.streamKey) || 0) + 1)
    }
    const topStreams = streams.map(stream => ({
      ...stream,
      viewers: Math.max(Number(stream.viewers || 0), viewersByStream.get(stream.streamKey) || 0)
    })).sort((a, b) => b.viewers - a.viewers).slice(0, 20)
    return {
      status: state.status,
      lastAttemptAt: state.lastAttemptAt,
      lastSuccessAt: state.lastSuccessAt,
      lastError: state.lastError,
      totals: {
        connections: connections.length,
        streams: streams.length,
        streamsOnline: streams.filter(stream => stream.online).length,
        servers: servers.length,
        serversOnline: servers.filter(server => server.online).length,
        streamErrors: errors.length
      },
      topStreams
    }
  }

  function start() {
    if (timer) return
    setTimeout(() => sync().catch(error => console.error('[xui] sync:', error.message)), 1500)
    timer = setInterval(() => sync().catch(error => console.error('[xui] sync:', error.message)), intervalMs)
    timer.unref()
  }

  return {
    sync,
    start,
    getState: () => state,
    overview,
    getPublicConfig: () => publicConfig(loadXuiConfig())
  }
}

module.exports = { createCollector }
