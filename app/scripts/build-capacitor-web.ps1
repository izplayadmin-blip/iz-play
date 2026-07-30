$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$out = Join-Path $root 'mobile-dist'
$webPlayerRoot = Join-Path (Split-Path -Parent $root) 'web-player'

if (Test-Path $out) {
  Remove-Item -LiteralPath $out -Recurse -Force
}
New-Item -ItemType Directory -Path $out | Out-Null

Copy-Item -LiteralPath (Join-Path $root 'index.html') -Destination (Join-Path $out 'index.html')

foreach ($dir in @('assets', 'vendor')) {
  $source = Join-Path $root $dir
  if (Test-Path $source) {
    Copy-Item -LiteralPath $source -Destination (Join-Path $out $dir) -Recurse
  }
}

$iconsSource = Join-Path $root 'icons'
if (!(Test-Path $iconsSource)) {
  $iconsSource = Join-Path $webPlayerRoot 'icons'
}
if (Test-Path $iconsSource) {
  Copy-Item -LiteralPath $iconsSource -Destination (Join-Path $out 'icons') -Recurse
}

$htmlPath = Join-Path $out 'index.html'
$html = Get-Content -LiteralPath $htmlPath -Raw
$html = $html.Replace(
  "const ipc=(typeof require!=='undefined')?require('electron').ipcRenderer:null`r`nconst IS_WEB_PLAYER=!ipc",
  "const ipc=(typeof require!=='undefined')?require('electron').ipcRenderer:null`r`nconst IS_CAPACITOR=!!window.Capacitor||location.hostname==='izplay.local'`r`nconst IS_WEB_PLAYER=!ipc&&!IS_CAPACITOR"
)
$html = $html.Replace(
  "if('serviceWorker' in navigator&&IS_WEB_PLAYER){",
  "if('serviceWorker' in navigator&&IS_WEB_PLAYER){"
)
Set-Content -LiteralPath $htmlPath -Value $html -NoNewline

@'
{
  "name": "IZ Play",
  "short_name": "IZ Play",
  "description": "IZ Play Android",
  "id": ".",
  "start_url": ".",
  "scope": ".",
  "display": "standalone",
  "orientation": "landscape",
  "background_color": "#000000",
  "theme_color": "#cc0000",
  "categories": ["entertainment", "video"],
  "icons": [
    {
      "src": "icons/icon-192.svg",
      "sizes": "192x192",
      "type": "image/svg+xml",
      "purpose": "any maskable"
    },
    {
      "src": "icons/icon-512.svg",
      "sizes": "512x512",
      "type": "image/svg+xml",
      "purpose": "any maskable"
    }
  ]
}
'@ | Set-Content -LiteralPath (Join-Path $out 'manifest.json') -NoNewline

Write-Host "Capacitor web build created at $out"
