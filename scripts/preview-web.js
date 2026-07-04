'use strict'

const http = require('http')
const fs = require('fs')
const path = require('path')
const { URL } = require('url')

const PORT = Number(process.env.PORT || 4174)
const ROOT = path.resolve(__dirname, '..', 'web-player')

const movies = Array.from({ length: 18 }, (_, index) => ({
  stream_id: 1000 + index,
  name: ['Horizonte Vermelho', 'Cidade Noturna', 'Depois do Sinal', 'Orbita Final', 'Linha de Fogo', 'A Ultima Chave'][index % 6] + (index > 5 ? ` ${index + 1}` : ''),
  category_id: index % 2 ? '12' : '11',
  stream_icon: `/preview/poster/${index}.svg`,
  year: String(2025 - (index % 5)),
  rating: String((7.2 + (index % 8) / 3).toFixed(1)),
  plot: 'Uma história intensa sobre escolhas, coragem e as consequências de atravessar uma linha sem volta.',
  genre: index % 2 ? 'Drama, Crime' : 'Ação, Suspense',
  duration_secs: 6240 + index * 60,
  container_extension: 'mp4'
}))

const series = Array.from({ length: 16 }, (_, index) => ({
  series_id: 2000 + index,
  name: ['Distrito Zero', 'Codigo Aurora', 'Os Invisiveis', 'Ponto de Ruptura', 'Sete Noites'][index % 5] + (index > 4 ? ` ${index + 1}` : ''),
  category_id: index % 2 ? '22' : '21',
  cover: `/preview/poster/${index + 30}.svg`,
  year: String(2026 - (index % 6)),
  rating: String((7.5 + (index % 7) / 4).toFixed(1))
}))

const channels = Array.from({ length: 20 }, (_, index) => ({
  stream_id: 3000 + index,
  name: ['IZ News', 'Cinema Max', 'Sport Prime', 'Series 24h', 'Kids Play'][index % 5] + (index > 4 ? ` ${index + 1}` : ''),
  category_id: index % 2 ? '2' : '1',
  stream_icon: `/preview/channel/${index}.svg`
}))

function json(res, body) {
  res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' })
  res.end(JSON.stringify(body))
}

function svg(res, id, wide = false) {
  const hues = [4, 28, 207, 256, 330, 178]
  const hue = hues[Number(id) % hues.length]
  const width = wide ? 640 : 420
  const height = wide ? 360 : 630
  const label = wide ? 'IZ PLAY AO VIVO' : ['ORIGINAL', 'PREMIERE', 'IZ PLAY', 'DESTAQUE'][Number(id) % 4]
  const body = `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">
    <defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop stop-color="hsl(${hue} 82% 48%)"/><stop offset=".55" stop-color="hsl(${(hue + 34) % 360} 64% 19%)"/><stop offset="1" stop-color="#03070d"/></linearGradient></defs>
    <rect width="100%" height="100%" fill="url(#g)"/>
    <circle cx="${width * .78}" cy="${height * .24}" r="${width * .24}" fill="rgba(255,255,255,.10)"/>
    <path d="M0 ${height * .74} Q ${width * .45} ${height * .48} ${width} ${height * .7} V${height}H0Z" fill="rgba(0,0,0,.34)"/>
    <text x="${width * .08}" y="${height * .76}" fill="#fff" font-family="Arial" font-size="${wide ? 38 : 34}" font-weight="900">${label}</text>
    <text x="${width * .08}" y="${height * .84}" fill="rgba(255,255,255,.75)" font-family="Arial" font-size="${wide ? 20 : 22}">CONTEUDO ${Number(id) + 1}</text>
  </svg>`
  res.writeHead(200, { 'Content-Type': 'image/svg+xml', 'Cache-Control': 'public, max-age=3600' })
  res.end(body)
}

function serveFile(res, pathname) {
  const relative = pathname === '/player/' ? 'index.html' : pathname.replace(/^\/player\//, '')
  const file = path.resolve(ROOT, relative)
  if (!file.startsWith(ROOT) || !fs.existsSync(file) || fs.statSync(file).isDirectory()) {
    res.writeHead(404)
    return res.end('Not found')
  }
  const ext = path.extname(file).toLowerCase()
  const types = { '.html': 'text/html; charset=utf-8', '.js': 'text/javascript', '.json': 'application/json', '.svg': 'image/svg+xml', '.png': 'image/png' }
  res.writeHead(200, { 'Content-Type': types[ext] || 'application/octet-stream', 'Cache-Control': 'no-store' })
  fs.createReadStream(file).pipe(res)
}

http.createServer((req, res) => {
  const parsed = new URL(req.url, `http://${req.headers.host}`)
  if (parsed.pathname === '/') {
    res.writeHead(302, { Location: '/player/' })
    return res.end()
  }
  if (parsed.pathname.startsWith('/preview/poster/')) return svg(res, parsed.pathname.match(/(\d+)/)?.[1] || 0)
  if (parsed.pathname.startsWith('/preview/channel/')) return svg(res, parsed.pathname.match(/(\d+)/)?.[1] || 0, true)
  if (parsed.pathname === '/control/api/client/config') {
    return json(res, {
      defaultDns: '/gateway',
      dnsServers: ['/gateway'],
      telemetryUrl: '/control/api',
      gatewayUrl: '/gateway',
      videoGatewayUrl: '/video-gateway',
      swarmCloud: {
        enabled: true,
        sdkUrl: '/player/vendor/swarmcloud-hls.min.js',
        token: '',
        appId: 'izplay-preview'
      },
      appVersion: '2.1.0'
    })
  }
  if (parsed.pathname === '/control/api/client/home') {
    const rank = (item, contentType, index) => ({
      contentType,
      contentId: String(item.stream_id || item.series_id),
      contentName: item.name,
      views: 240 - index * 7,
      score: 2400 - index * 70
    })
    return json(res, {
      hot: [...movies.slice(0, 5).map((item, i) => rank(item, 'vod', i)), ...series.slice(0, 5).map((item, i) => rank(item, 'series', i))],
      movies: movies.slice(0, 10).map((item, i) => rank(item, 'vod', i)),
      series: series.slice(0, 10).map((item, i) => rank(item, 'series', i)),
      live: channels.slice(0, 10).map((item, i) => rank(item, 'live', i))
    })
  }
  if (parsed.pathname.startsWith('/control/api/telemetry/')) return json(res, { ok: true })
  if (parsed.pathname === '/control/api/client/commands') return json(res, { commands: [] })
  if (parsed.pathname === '/gateway/player_api.php') {
    const action = parsed.searchParams.get('action')
    if (!action) return json(res, { user_info: { auth: 1, username: parsed.searchParams.get('username') } })
    if (action === 'get_live_categories') return json(res, [{ category_id: '1', category_name: 'Noticias' }, { category_id: '2', category_name: 'Entretenimento' }])
    if (action === 'get_live_streams') return json(res, channels)
    if (action === 'get_vod_categories') return json(res, [{ category_id: '11', category_name: 'Lancamentos' }, { category_id: '12', category_name: 'Cinema' }])
    if (action === 'get_vod_streams') return json(res, movies)
    if (action === 'get_vod_info') {
      const movie = movies.find(item => String(item.stream_id) === String(parsed.searchParams.get('vod_id'))) || movies[0]
      return json(res, {
        info: {
          ...movie,
          movie_image: movie.stream_icon,
          backdrop_path: [`/preview/poster/${(movie.stream_id - 1000 + 50)}.svg`],
          director: 'Direção IZ Studios',
          cast: 'Marina Costa, Rafael Nunes, Bianca Alves'
        },
        movie_data: movie
      })
    }
    if (action === 'get_series_categories') return json(res, [{ category_id: '21', category_name: 'Originais' }, { category_id: '22', category_name: 'Drama' }])
    if (action === 'get_series') return json(res, series)
    return json(res, [])
  }
  if (parsed.pathname.startsWith('/player/')) return serveFile(res, parsed.pathname)
  res.writeHead(404)
  res.end('Not found')
}).listen(PORT, '127.0.0.1', () => {
  console.log(`IZ Play Web preview em http://127.0.0.1:${PORT}/player/`)
})
