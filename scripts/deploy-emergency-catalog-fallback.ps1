param(
  [string]$PanelHost = "38.46.142.234",
  [string]$WebHost = "208.122.17.45",
  [string]$User = "root",
  [string]$PanelRemoteRoot = "/opt/izplay-v2.1",
  [string]$WebRemoteDir = "/var/www/izplay-web",
  [string]$PanelProcessName = "izplay-panel",
  [string]$Password = ""
)

$ErrorActionPreference = "Stop"
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$PanelDir = Join-Path $RepoRoot "panel"
$WebDir = Join-Path $RepoRoot "web-player"

function PuttyArgs {
  $args = @()
  if ($Password) { $args += @("-pw", $Password) }
  return $args
}

function Copy-Remote($Local, $Remote) {
  $putty = PuttyArgs
  & pscp @putty $Local $Remote
  if ($LASTEXITCODE -ne 0) { throw "Falha no upload: $Local -> $Remote" }
}

function Run-Remote($HostName, $Command) {
  $putty = PuttyArgs
  & plink @putty -batch "${User}@${HostName}" $Command
  if ($LASTEXITCODE -ne 0) { throw "Comando remoto falhou em $HostName" }
}

Write-Host "1/4 Enviando Painel Admin com fallback de catálogo..." -ForegroundColor Cyan
Copy-Remote (Join-Path $PanelDir "server.js") "${User}@${PanelHost}:/tmp/izplay-panel-server.js.new"
Copy-Remote (Join-Path $PanelDir "package.json") "${User}@${PanelHost}:/tmp/izplay-panel-package.json.new"
Copy-Remote (Join-Path $PanelDir "package-lock.json") "${User}@${PanelHost}:/tmp/izplay-panel-package-lock.json.new"

$panelCommand = @"
set -e
panel_dir='$PanelRemoteRoot/panel'
test -d "`$panel_dir"
stamp=`$(date +%F-%H%M%S)
cp -a "`$panel_dir/server.js" "`$panel_dir/server.js.bak-emergency-catalog-`$stamp"
cp -a "`$panel_dir/package.json" "`$panel_dir/package.json.bak-emergency-catalog-`$stamp"
if [ -f "`$panel_dir/package-lock.json" ]; then cp -a "`$panel_dir/package-lock.json" "`$panel_dir/package-lock.json.bak-emergency-catalog-`$stamp"; fi
mv /tmp/izplay-panel-server.js.new "`$panel_dir/server.js"
mv /tmp/izplay-panel-package.json.new "`$panel_dir/package.json"
mv /tmp/izplay-panel-package-lock.json.new "`$panel_dir/package-lock.json"
cd "`$panel_dir"
node --check server.js
if [ -f package-lock.json ]; then npm ci --omit=dev; else npm install --omit=dev; fi
pm2 restart '$PanelProcessName' --update-env
pm2 save >/dev/null 2>&1 || true
curl -fsS "http://127.0.0.1:3000/api/client/config" | head -c 300
echo
echo "OK_PANEL_EMERGENCY_CATALOG backup_stamp=`$stamp"
"@
Run-Remote $PanelHost $panelCommand

Write-Host "2/4 Enviando Web Player com fallback para /control/api/client/catalog..." -ForegroundColor Cyan
Copy-Remote (Join-Path $WebDir "index.html") "${User}@${WebHost}:/tmp/izplay-index.html.emergency.new"
Copy-Remote (Join-Path $WebDir "service-worker.js") "${User}@${WebHost}:/tmp/izplay-service-worker.js.emergency.new"

$webCommand = @"
set -e
web_dir='$WebRemoteDir'
test -d "`$web_dir"
stamp=`$(date +%F-%H%M%S)
cp -a "`$web_dir/index.html" "`$web_dir/index.html.bak-emergency-catalog-`$stamp"
if [ -f "`$web_dir/service-worker.js" ]; then cp -a "`$web_dir/service-worker.js" "`$web_dir/service-worker.js.bak-emergency-catalog-`$stamp"; fi
mv /tmp/izplay-index.html.emergency.new "`$web_dir/index.html"
mv /tmp/izplay-service-worker.js.emergency.new "`$web_dir/service-worker.js"
chown www-data:www-data "`$web_dir/index.html" "`$web_dir/service-worker.js" || true
chmod 0644 "`$web_dir/index.html" "`$web_dir/service-worker.js"
echo "OK_WEB_EMERGENCY_CATALOG backup_stamp=`$stamp"
"@
Run-Remote $WebHost $webCommand

Write-Host "3/4 Validando endpoints públicos..." -ForegroundColor Cyan
curl.exe -k -sS -o NUL -w "control_config=%{http_code}`n" "https://web.izplay.tv/control/api/client/config"
curl.exe -k -sS -o NUL -w "emergency_catalog=%{http_code}`n" "https://web.izplay.tv/control/api/client/catalog/player_api.php?username=healthcheck&password=healthcheck"
curl.exe -k -sS -o NUL -w "web_index=%{http_code}`n" "https://web.izplay.tv/player/index.html"

Write-Host "4/4 Concluído. Abra em janela anônima e teste login." -ForegroundColor Green
