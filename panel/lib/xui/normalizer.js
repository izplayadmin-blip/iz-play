const crypto = require('crypto')

function rowsFrom(payload) {
  if (Array.isArray(payload)) return payload
  if (!payload || typeof payload !== 'object') return []
  for (const key of ['data', 'result', 'results', 'items', 'rows']) {
    if (Array.isArray(payload[key])) return payload[key]
  }
  return []
}

function first(row, keys, fallback = '') {
  for (const key of keys) {
    if (row?.[key] !== undefined && row[key] !== null) return row[key]
  }
  return fallback
}

function opaqueId(type, value) {
  return crypto.createHash('sha256').update(`${type}:${String(value || '')}`).digest('hex').slice(0, 24)
}

function normalizeConnections(payload) {
  return rowsFrom(payload).map(row => {
    const streamId = first(row, ['stream_id', 'streamId', 'channel_id', 'channelId'])
    return {
      id: opaqueId('connection', first(row, ['activity_id', 'connection_id', 'id'], JSON.stringify(row))),
      streamId: streamId ? String(streamId) : '',
      streamKey: opaqueId('stream', streamId),
      serverId: String(first(row, ['server_id', 'serverId', 'lb_id'], '')),
      startedAt: first(row, ['date_start', 'started_at', 'start_time'], null),
      container: String(first(row, ['container', 'container_extension'], '')),
      clientType: String(first(row, ['user_agent', 'client_type', 'device'], '')).slice(0, 120)
    }
  })
}

function normalizeStreams(payload) {
  return rowsFrom(payload).map(row => {
    const id = first(row, ['stream_id', 'id', 'channel_id'])
    return {
      id: String(id || ''),
      streamKey: opaqueId('stream', id),
      name: String(first(row, ['stream_display_name', 'name', 'title'], '')).slice(0, 180),
      type: String(first(row, ['stream_type', 'type', 'category_type'], '')).toLowerCase().slice(0, 40),
      categoryId: String(first(row, ['category_id', 'categoryId'], '')),
      serverId: String(first(row, ['server_id', 'serverId'], '')),
      online: ['0', 'online', 'running'].includes(String(first(row, ['stream_status'], '')).toLowerCase())
        || Boolean(Number(first(row, ['online', 'status'], 0)) || first(row, ['online'], false) === true),
      viewers: Number(first(row, ['clients', 'viewers', 'connections'], 0)) || 0
    }
  })
}

function normalizeServers(payload) {
  return rowsFrom(payload).map(row => ({
    id: String(first(row, ['server_id', 'id'], '')),
    name: String(first(row, ['server_name', 'name'], '')).slice(0, 160),
    online: Boolean(Number(first(row, ['server_online', 'online', 'enabled', 'status'], 0))
      || first(row, ['server_online', 'online'], false) === true),
    cpu: Number(first(row, ['cpu', 'cpu_usage'], 0)) || 0,
    memory: Number(first(row, ['memory', 'ram', 'memory_usage'], 0)) || 0,
    networkIn: Number(first(row, ['network_in', 'bytes_in'], 0)) || 0,
    networkOut: Number(first(row, ['network_out', 'bytes_out'], 0)) || 0
  }))
}

function normalizeErrors(payload) {
  return rowsFrom(payload).map(row => ({
    id: opaqueId('error', first(row, ['id'], JSON.stringify(row))),
    streamId: String(first(row, ['stream_id', 'streamId'], '')),
    serverId: String(first(row, ['server_id', 'serverId'], '')),
    code: String(first(row, ['code', 'error_code', 'type'], '')).slice(0, 80),
    message: String(first(row, ['message', 'error', 'reason'], '')).slice(0, 500),
    occurredAt: first(row, ['created_at', 'date', 'timestamp'], null)
  }))
}

module.exports = { normalizeConnections, normalizeStreams, normalizeServers, normalizeErrors }
