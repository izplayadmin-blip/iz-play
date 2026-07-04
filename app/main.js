const { app, BrowserWindow, session, ipcMain } = require('electron')
const { spawn } = require('child_process')
const path = require('path')
const fs = require('fs')
const http = require('http')
const os = require('os')

// ─── Binários FFmpeg via npm (automático, sem precisar copiar arquivos) ───────
let FFMPEG_PATH  = null
let FFPROBE_PATH = null
try {
  FFMPEG_PATH  = require('@ffmpeg-installer/ffmpeg').path
  FFPROBE_PATH = require('@ffprobe-installer/ffprobe').path
  console.log('[ffmpeg] path:', FFMPEG_PATH)
  console.log('[ffprobe] path:', FFPROBE_PATH)
} catch(e) {
  console.warn('[ffmpeg] @ffmpeg-installer não encontrado — transcodificação desativada')
}

app.commandLine.appendSwitch('disable-web-security')
app.commandLine.appendSwitch('ignore-certificate-errors')
app.commandLine.appendSwitch('autoplay-policy', 'no-user-gesture-required')
app.commandLine.appendSwitch('ignore-certificate-errors-spki-list', '')
app.commandLine.appendSwitch('allow-insecure-localhost', 'true')

let proxyProcess   = null
let mpvProcess     = null
let ffmpegProcess  = null
let hlsServer      = null
let mainWindow     = null
let nvencAvailable = null

const PROXY_PORT     = 8080
const PROXY_USER     = 'admin'
const PROXY_PASS     = 'iptv1234'
const TRANSCODE_PORT = 9191

// Gateway na VPS — usado APENAS como fallback quando a reprodução nativa falha.
// O desktop sempre tenta tocar direto/mpv primeiro (melhor qualidade, zero banda
// da VPS). Só recorre ao gateway em caso de falha de ACESSO (403/redirect/bloqueio),
// para proteger a banda da VPS. Pode ser sobrescrito por env IZ_GATEWAY.
const GATEWAY_BASE   = (process.env.IZ_GATEWAY || 'http://209.50.254.197').replace(/\/+$/,'')
const GATEWAY_UA     = 'VLC/3.0.20 LibVLC/3.0.20'

const MPV_PATH   = path.join(__dirname, 'mpv.exe')
const PROXY_PATH = path.join(__dirname, 'iptv-proxy.exe')

function getPrimaryNetwork() {
  const nets = os.networkInterfaces()
  for (const [name, iface] of Object.entries(nets)) {
    for (const net of iface || []) {
      if (net.family === 'IPv4' && !net.internal) {
        return { address: net.address, mac: net.mac && net.mac !== '00:00:00:00:00:00' ? net.mac : '', iface: name }
      }
    }
  }
  return { address: '127.0.0.1', mac: '', iface: 'loopback' }
}

function getLocalIPv4() {
  return getPrimaryNetwork().address
}

function isPortOpen(port, host = '127.0.0.1') {
  return new Promise(resolve => {
    const req = http.get({ host, port, path: '/', timeout: 700 }, res => {
      res.resume()
      resolve(true)
    })
    req.on('timeout', () => { req.destroy(); resolve(false) })
    req.on('error', () => resolve(false))
  })
}

// ─── Helpers ─────────────────────────────────────────────────────────────────
function parseXtream(m3uUrl) {
  try {
    const u = new URL(m3uUrl)
    const username = u.searchParams.get('username')
    const password = u.searchParams.get('password')
    const baseUrl  = u.origin
    if (username && password) return { username, password, baseUrl }
  } catch(e) {}
  return null
}

function normalizeXtreamUrl(m3uUrl) {
  try {
    const u = new URL(m3uUrl)
    if (u.searchParams.get('username') && u.searchParams.get('password')) {
      u.searchParams.set('type', 'm3u_plus')
      u.searchParams.set('output', 'm3u8')
      return u.toString()
    }
  } catch(e) {}
  return m3uUrl
}

// ─── Proxy IPTV ──────────────────────────────────────────────────────────────
async function startProxy(m3uUrl) {
  m3uUrl = normalizeXtreamUrl(m3uUrl)
  if (proxyProcess) { try { proxyProcess.kill() } catch(e) {} proxyProcess = null }
  if (!fs.existsSync(PROXY_PATH)) return false
  if (await isPortOpen(PROXY_PORT)) return true

  const xtream = parseXtream(m3uUrl)
  const args = [
    '--m3u-url', m3uUrl,
    '--port',     String(PROXY_PORT),
    '--hostname', 'localhost',
    '--user',     PROXY_USER,
    '--password', PROXY_PASS,
  ]
  if (xtream) {
    args.push('--xtream-user',     xtream.username)
    args.push('--xtream-password', xtream.password)
    args.push('--xtream-base-url', xtream.baseUrl)
  }

  proxyProcess = spawn(PROXY_PATH, args, {
    cwd: __dirname, windowsHide: true, shell: false,
    env: { ...process.env, GIN_MODE: 'release' },
  })
  proxyProcess.stdout.on('data', d => console.log('[proxy]', d.toString().trim()))
  proxyProcess.stderr.on('data', d => console.log('[proxy err]', d.toString().trim()))
  proxyProcess.on('error', err => console.error('[proxy error]', err))
  proxyProcess.on('exit', code => {
    console.log('[proxy] saiu:', code)
    proxyProcess = null
  })
  return true
}

// ─── Transcoder: detecta codec via FFprobe ───────────────────────────────────
function detectCodec(streamUrl) {
  return new Promise((resolve) => {
    if (!FFPROBE_PATH) { resolve('h264'); return }

    const probe = spawn(FFPROBE_PATH, [
      '-v', 'quiet', '-print_format', 'json',
      '-show_streams', '-select_streams', 'v:0',
      '-analyzeduration', '5000000', '-probesize', '5000000',
      streamUrl
    ])

    let output = ''
    probe.stdout.on('data', d => { output += d.toString() })

    const timeout = setTimeout(() => {
      probe.kill()
      console.log('[transcoder] FFprobe timeout — assumindo H.264')
      resolve('h264')
    }, 8000)

    probe.on('close', () => {
      clearTimeout(timeout)
      try {
        const data = JSON.parse(output)
        const codec = (data.streams && data.streams[0] && data.streams[0].codec_name) || 'h264'
        console.log('[transcoder] Codec detectado:', codec)
        resolve(codec)
      } catch { resolve('h264') }
    })
    probe.on('error', () => { clearTimeout(timeout); resolve('h264') })
  })
}

// ─── Transcoder: verifica NVENC ──────────────────────────────────────────────
function checkNvenc() {
  return new Promise((resolve) => {
    if (nvencAvailable !== null) { resolve(nvencAvailable); return }
    if (!FFMPEG_PATH) { nvencAvailable = false; resolve(false); return }

    const p = spawn(FFMPEG_PATH, [
      '-f', 'lavfi', '-i', 'nullsrc=s=128x128:d=1',
      '-c:v', 'h264_nvenc', '-f', 'null', '-'
    ])
    p.on('close', code => {
      nvencAvailable = (code === 0)
      console.log('[transcoder] NVENC disponível:', nvencAvailable)
      resolve(nvencAvailable)
    })
    p.on('error', () => { nvencAvailable = false; resolve(false) })
  })
}

// ─── Transcoder: para processo ativo ─────────────────────────────────────────
function stopTranscoding() {
  if (ffmpegProcess) { try { ffmpegProcess.kill('SIGKILL') } catch(e) {} ffmpegProcess = null }
  if (hlsServer)     { try { hlsServer.close() }             catch(e) {} hlsServer     = null }
}

// ─── Transcoder: inicia HLS local ────────────────────────────────────────────
function startTranscoding(streamUrl, useNvenc) {
  return new Promise((resolve, reject) => {
    stopTranscoding()

    const tmpDir = path.join(os.tmpdir(), 'izplay_hls')
    if (!fs.existsSync(tmpDir)) fs.mkdirSync(tmpDir, { recursive: true })
    fs.readdirSync(tmpDir).forEach(f => { try { fs.unlinkSync(path.join(tmpDir, f)) } catch {} })

    const playlistPath = path.join(tmpDir, 'stream.m3u8')
    const videoEncoder = useNvenc ? 'h264_nvenc' : 'libx264'
    const encoderOpts  = useNvenc
      ? ['-preset','p2','-tune','ll','-rc','vbr','-cq','28','-b:v','4M','-maxrate','6M','-bufsize','8M']
      : ['-preset','ultrafast','-crf','28','-b:v','4M']

    const args = [
      '-re', '-i', streamUrl,
      '-map', '0:v:0', '-map', '0:a:0',
      '-c:v', videoEncoder, ...encoderOpts,
      '-c:a', 'aac', '-ac', '2', '-ar', '48000',
      '-f', 'hls',
      '-hls_time', '2',
      '-hls_list_size', '6',
      '-hls_flags', 'delete_segments+independent_segments',
      '-hls_segment_filename', path.join(tmpDir, 'seg%03d.ts'),
      playlistPath
    ]

    console.log(`[transcoder] Iniciando FFmpeg (${useNvenc ? 'NVENC' : 'CPU'})...`)
    ffmpegProcess = spawn(FFMPEG_PATH, args)
    ffmpegProcess.on('error', err => { console.error('[ffmpeg error]', err.message); reject(err) })
    ffmpegProcess.on('close', code => {
      if (code && code !== 0) console.warn('[ffmpeg] saiu com código:', code)
    })

    // Servidor HTTP pra servir os segmentos HLS pro Electron
    hlsServer = http.createServer((req, res) => {
      const filePath = path.join(tmpDir, path.basename(req.url.split('?')[0]))
      fs.readFile(filePath, (err, data) => {
        if (err) { res.writeHead(404); res.end(); return }
        res.writeHead(200, {
          'Content-Type': filePath.endsWith('.m3u8')
            ? 'application/vnd.apple.mpegurl'
            : 'video/mp2t',
          'Access-Control-Allow-Origin': '*',
          'Cache-Control': 'no-cache',
        })
        res.end(data)
      })
    })
    hlsServer.listen(TRANSCODE_PORT, '127.0.0.1', () => {
      console.log(`[transcoder] Servidor HLS em http://127.0.0.1:${TRANSCODE_PORT}`)
    })

    // Aguarda playlist ser criada pelo FFmpeg (até 15s)
    let attempts = 0
    const wait = setInterval(() => {
      attempts++
      if (fs.existsSync(playlistPath)) {
        clearInterval(wait)
        const hlsUrl = `http://127.0.0.1:${TRANSCODE_PORT}/stream.m3u8`
        console.log('[transcoder] Playlist pronta:', hlsUrl)
        resolve(hlsUrl)
      } else if (attempts >= 30) {
        clearInterval(wait)
        reject(new Error('FFmpeg não gerou a playlist no tempo esperado'))
      }
    }, 500)
  })
}

// ─── Fallback via gateway (só quando o acesso direto falha) ──────────────────
// Testa rapidamente se a URL responde direto. Se der bloqueio (403) ou erro de
// rede, devolve a URL roteada pelo gateway (/proxy resolve header/UA; /smart
// resolve codec). Mantém o uso de banda da VPS restrito a casos de falha real.
function quickAccessCheck(streamUrl) {
  return new Promise((resolve) => {
    let mod
    try { mod = streamUrl.startsWith('https') ? require('https') : require('http') }
    catch(e) { resolve({ ok: false, status: 0 }); return }
    let done = false
    const finish = (r) => { if (!done) { done = true; resolve(r) } }
    try {
      const req = mod.get(streamUrl, {
        headers: { 'User-Agent': GATEWAY_UA, 'Accept': '*/*' },
        timeout: 6000
      }, res => {
        const status = res.statusCode || 0
        res.destroy() // só queremos o status, não o corpo
        // 2xx e 3xx (redirect) contam como acessível; 401/403/404/5xx = bloqueio
        finish({ ok: status >= 200 && status < 400, status })
      })
      req.on('timeout', () => { req.destroy(); finish({ ok: false, status: 0 }) })
      req.on('error', () => finish({ ok: false, status: 0 }))
    } catch(e) { finish({ ok: false, status: 0 }) }
  })
}

function gatewayProxyUrl(streamUrl) {
  return `${GATEWAY_BASE}/proxy?url=${encodeURIComponent(streamUrl)}`
}
// /transcode entrega HLS diretamente (não JSON), mais simples de tocar no player.
function gatewayTranscodeUrl(streamUrl) {
  return `${GATEWAY_BASE}/transcode?url=${encodeURIComponent(streamUrl)}`
}

function isGatewayPlaybackUrl(streamUrl) {
  const raw = String(streamUrl || '')
  const low = raw.toLowerCase()
  const base = GATEWAY_BASE.replace(/\/+$/, '').toLowerCase()
  return /\/(?:video-gateway\/)?(?:proxy|transcode)\?/i.test(raw) ||
    low.startsWith(`${base}/proxy?`) ||
    low.startsWith(`${base}/transcode?`)
}

// ─── Transcoder: função principal ────────────────────────────────────────────
async function resolveStream(streamUrl) {
  if (isGatewayPlaybackUrl(streamUrl)) {
    const isTranscode = /\/transcode\?/i.test(String(streamUrl || ''))
    console.log('[transcoder] URL ja resolvida pelo gateway - usando direto')
    return { url: streamUrl, transcoded: isTranscode, viaGateway: isTranscode ? 'transcode' : 'proxy' }
  }

  // Sem binários instalados: ainda tenta resgatar acesso bloqueado via gateway.
  if (!FFMPEG_PATH || !FFPROBE_PATH) {
    const access = await quickAccessCheck(streamUrl)
    if (!access.ok) {
      console.log(`[fallback] FFmpeg indisponivel e acesso direto falhou (status ${access.status}) -> gateway proxy`)
      return { url: gatewayProxyUrl(streamUrl), transcoded: false, viaGateway: 'proxy' }
    }
    console.log('[transcoder] FFmpeg indisponivel - stream direto')
    return { url: streamUrl, transcoded: false }
  }

  const codec = await detectCodec(streamUrl)
  const nativeCodecs = ['h264', 'avc', 'avc1', 'vp8', 'vp9', 'av1']
  const isNative = nativeCodecs.some(c => codec.toLowerCase().includes(c))

  if (isNative) {
    stopTranscoding()
    // Codec ok, mas o acesso pode estar bloqueado (403/redirect/header).
    // Verifica rápido; se direto funciona, usa direto (zero banda da VPS).
    const access = await quickAccessCheck(streamUrl)
    if (access.ok) {
      return { url: streamUrl, transcoded: false, codec }
    }
    // Acesso bloqueado direto → fallback pelo gateway (proxy resolve UA/header).
    console.log(`[fallback] acesso direto falhou (status ${access.status}) → gateway proxy`)
    return { url: gatewayProxyUrl(streamUrl), transcoded: false, codec, viaGateway: 'proxy' }
  }

  console.log(`[transcoder] Codec não nativo (${codec}) → transcode LOCAL primeiro`)
  const useNvenc = await checkNvenc()

  try {
    const hlsUrl = await startTranscoding(streamUrl, useNvenc)
    return {
      url: hlsUrl,
      transcoded: true,
      codec,
      encoder: useNvenc ? 'NVENC' : 'CPU'
    }
  } catch(err) {
    // Transcode local falhou → último recurso: gateway transcode (HLS pronto).
    console.error('[transcoder] transcode local falhou:', err.message, '→ gateway transcode')
    return { url: gatewayTranscodeUrl(streamUrl), transcoded: true, codec, viaGateway: 'transcode' }
  }
}

// ─── Janela principal ─────────────────────────────────────────────────────────
function createWindow() {
  session.defaultSession.webRequest.onBeforeSendHeaders((details, callback) => {
    const h = { ...details.requestHeaders }
    delete h['Origin']; delete h['Referer']
    h['User-Agent'] = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
    callback({ requestHeaders: h })
  })
  session.defaultSession.webRequest.onHeadersReceived((details, callback) => {
    const h = { ...details.responseHeaders }
    h['Access-Control-Allow-Origin']  = ['*']
    h['Access-Control-Allow-Methods'] = ['*']
    h['Access-Control-Allow-Headers'] = ['*']
    delete h['x-frame-options']; delete h['X-Frame-Options']
    delete h['content-security-policy']; delete h['Content-Security-Policy']
    callback({ responseHeaders: h })
  })

  mainWindow = new BrowserWindow({
    width: 1280, height: 760,
    minWidth: 900, minHeight: 600,
    title: 'IZ Play',
    backgroundColor: '#0a0a0a',
    webPreferences: {
      nodeIntegration: true,
      contextIsolation: false,
      webSecurity: false,
      allowRunningInsecureContent: true,
    }
  })

  mainWindow.loadFile('index.html')
  mainWindow.setMenuBarVisibility(false)
}

// ─── IPC handlers ─────────────────────────────────────────────────────────────

ipcMain.on('start-proxy', async (event, m3uUrl) => {
  const ok = await startProxy(m3uUrl)
  if (!ok) { event.reply('proxy-result', { error: 'iptv-proxy.exe não encontrado!' }); return }
  setTimeout(() => {
    event.reply('proxy-result', {
      url: `http://localhost:${PROXY_PORT}/iptv.m3u?username=${PROXY_USER}&password=${PROXY_PASS}`
    })
  }, 2000)
})

ipcMain.on('stop-proxy', () => {
  if (proxyProcess) { try { proxyProcess.kill() } catch(e) {} proxyProcess = null }
})

ipcMain.handle('clear-cache', async () => {
  try {
    await session.defaultSession.clearCache()
    return { ok: true }
  } catch (err) {
    return { error: err.message }
  }
})

ipcMain.handle('network-info', async () => {
  const primary = getPrimaryNetwork()
  const localIp = primary.address
  return {
    localIp,
    macAddress: primary.mac,
    networkInterface: primary.iface,
    proxyPort: PROXY_PORT,
    proxyRunning: !!proxyProcess,
    proxyLocalUrl: `http://localhost:${PROXY_PORT}`,
    proxyLanUrl: `http://${localIp}:${PROXY_PORT}`
  }
})

ipcMain.on('play-external', (event, streamUrl) => {
  if (!fs.existsSync(MPV_PATH)) {
    event.reply('external-result', { error: 'mpv.exe não encontrado!' })
    return
  }
  if (mpvProcess) { try { mpvProcess.kill() } catch(e) {} mpvProcess = null }

  const launchMpv = (url, isFallback) => {
    const startedAt = Date.now()
    const proc = spawn(MPV_PATH, [
      url, '--force-window=yes', '--title=IZ Play', '--really-quiet', '--no-terminal',
    ], { cwd: __dirname, windowsHide: false, shell: false })
    mpvProcess = proc

    proc.on('error', err => {
      if (!isFallback) { tryFallback() }
      else event.reply('external-result', { error: err.message })
    })
    proc.on('exit', () => {
      const lived = Date.now() - startedAt
      mpvProcess = null
      // Saída muito rápida (< 4s) = mpv não conseguiu tocar o stream direto.
      // Só nesse caso recorre ao gateway (protege banda da VPS).
      if (!isFallback && lived < 4000) { tryFallback() }
    })
    if (!isFallback) event.reply('external-result', { ok: true })
  }

  const tryFallback = () => {
    if (isGatewayPlaybackUrl(streamUrl)) {
      console.log('[fallback] mpv falhou em URL ja resolvida pelo gateway - sem novo proxy')
      event.reply('external-result', { error: 'Falha ao abrir pelo gateway' })
      return
    }
    console.log('[fallback] mpv falhou no direto → tentando via gateway proxy')
    event.reply('external-result', { ok: true, viaGateway: 'proxy' })
    launchMpv(gatewayProxyUrl(streamUrl), true)
  }

  launchMpv(streamUrl, false)
})

// Resolve stream com detecção automática de codec + transcodificação
ipcMain.handle('play-stream', async (event, url) => {
  console.log('[main] play-stream:', url)
  stopTranscoding()
  try {
    const result = await resolveStream(url)
    console.log('[main] resultado:', JSON.stringify(result))
    return { success: true, ...result }
  } catch(err) {
    console.error('[main] erro ao resolver stream:', err.message)
    return { success: false, url, error: err.message }
  }
})

// ─── Lifecycle ────────────────────────────────────────────────────────────────
app.whenReady().then(createWindow)

app.on('window-all-closed', () => {
  if (proxyProcess) try { proxyProcess.kill() } catch(e) {}
  if (mpvProcess)   try { mpvProcess.kill() }   catch(e) {}
  stopTranscoding()
  if (process.platform !== 'darwin') app.quit()
})

app.on('before-quit', () => { stopTranscoding() })
