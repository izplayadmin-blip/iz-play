param(
  [string]$HostName = "38.46.142.234",
  [string]$User = "root",
  [string]$RemoteRoot = "/opt/izplay-v2.1",
  [string]$ProcessName = "izplay-panel"
)

$ErrorActionPreference = "Stop"

function Require-Command($Name) {
  if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
    throw "Comando '$Name' não encontrado. Instale/ative o OpenSSH Client do Windows."
  }
}

Require-Command "ssh"
Require-Command "scp"

$RepoRoot = Split-Path -Parent $PSScriptRoot
$PanelDir = Join-Path $RepoRoot "panel"
$Remote = "$User@$HostName"

$Files = @(
  @{ Local = Join-Path $PanelDir "server.js"; Remote = "/tmp/izplay-panel-server.js.new" },
  @{ Local = Join-Path $PanelDir "index.html"; Remote = "/tmp/izplay-panel-index.html.new" },
  @{ Local = Join-Path $PanelDir "package.json"; Remote = "/tmp/izplay-panel-package.json.new" },
  @{ Local = Join-Path $PanelDir "package-lock.json"; Remote = "/tmp/izplay-panel-package-lock.json.new" },
  @{ Local = Join-Path $PanelDir "data\config.json"; Remote = "/tmp/izplay-panel-config.json.new" }
)

Write-Host "Deploy código/config do Painel Admin" -ForegroundColor Cyan
Write-Host "Destino: ${Remote}:${RemoteRoot}/panel"
Write-Host "Este script preserva reports.json, devices.json, p2p.json e commands.json." -ForegroundColor Yellow
Write-Host "A senha root será pedida pelo OpenSSH, se não houver chave configurada."
Write-Host ""

foreach ($f in $Files) {
  if (-not (Test-Path $f.Local)) { throw "Arquivo não encontrado: $($f.Local)" }
  scp $f.Local "${Remote}:$($f.Remote)"
}

$Command = @"
set -e
panel_dir="$RemoteRoot/panel"
test -d "`$panel_dir"
stamp=`$(date +%F-%H%M%S)
mkdir -p "`$panel_dir/data"
for f in server.js index.html package.json package-lock.json data/config.json; do
  if [ -f "`$panel_dir/`$f" ]; then
    cp -a "`$panel_dir/`$f" "`$panel_dir/`$f.bak-`$stamp"
  fi
done
mv /tmp/izplay-panel-server.js.new "`$panel_dir/server.js"
mv /tmp/izplay-panel-index.html.new "`$panel_dir/index.html"
mv /tmp/izplay-panel-package.json.new "`$panel_dir/package.json"
mv /tmp/izplay-panel-package-lock.json.new "`$panel_dir/package-lock.json"
mv /tmp/izplay-panel-config.json.new "`$panel_dir/data/config.json"
cd "`$panel_dir"
if [ -f package-lock.json ]; then npm ci --omit=dev; else npm install --omit=dev; fi
pm2 restart "$ProcessName" --update-env
pm2 save >/dev/null 2>&1 || true
echo "OK_PANEL_CODE_CONFIG backup_stamp=`$stamp"
curl -fsS "http://127.0.0.1:3000/api/client/config" | head -c 500
echo
"@

ssh $Remote $Command

try {
  $r = Invoke-WebRequest -Uri "http://admin.izplay.tv/api/client/config" -UseBasicParsing -TimeoutSec 20
  Write-Host "Verificação pública admin: HTTP $($r.StatusCode)" -ForegroundColor Green
  Write-Host ($r.Content.Substring(0, [Math]::Min(500, $r.Content.Length)))
} catch {
  Write-Host "Aviso: não consegui validar admin.izplay.tv/api/client/config publicamente." -ForegroundColor Yellow
  Write-Host $_.Exception.Message -ForegroundColor DarkYellow
}
