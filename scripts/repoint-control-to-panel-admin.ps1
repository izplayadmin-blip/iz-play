param(
  [string]$WebHost = "208.122.17.45",
  [string]$GatewayHost = "209.50.254.197",
  [string]$User = "root",
  [string]$PanelUrl = "http://38.46.142.234",
  [switch]$SkipGateway
)

$ErrorActionPreference = "Stop"

function Require-Command($Name) {
  if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
    throw "Comando '$Name' não encontrado. Instale/ative o OpenSSH Client do Windows."
  }
}

Require-Command "ssh"

$WebRemote = "$User@$WebHost"
$GatewayRemote = "$User@$GatewayHost"

Write-Host "Reapontando /control/ para o Painel Admin novo" -ForegroundColor Cyan
Write-Host "Painel: $PanelUrl"
Write-Host "As senhas serão pedidas pelo OpenSSH, se não houver chave configurada." -ForegroundColor Yellow
Write-Host ""

$WebCommand = @"
set -e
conf=/etc/nginx/sites-available/izplay-web
test -f "`$conf"
bak="`$conf.bak-`$(date +%F-%H%M%S)"
cp -a "`$conf" "`$bak"
python3 - <<'PY'
from pathlib import Path
p = Path('/etc/nginx/sites-available/izplay-web')
s = p.read_text()
old_values = [
  'http://130.250.189.142:3001',
  'http://209.14.85.55:3000',
  'http://209.14.85.55'
]
for old in old_values:
    s = s.replace(old, '$PanelUrl')
p.write_text(s)
PY
nginx -t
systemctl reload nginx
echo "OK_WEB_CONTROL backup=`$bak"
"@

ssh $WebRemote $WebCommand

if (-not $SkipGateway) {
  $GatewayCommand = @"
set -e
if command -v pm2 >/dev/null 2>&1; then
  pm2 restart iz-catalog-gateway --update-env >/dev/null 2>&1 || true
  pm2 save >/dev/null 2>&1 || true
fi
echo "OK_GATEWAY_CHECK"
"@
  ssh $GatewayRemote $GatewayCommand
}

try {
  $r = Invoke-WebRequest -Uri "https://web.izplay.tv/control/api/client/config" -UseBasicParsing -TimeoutSec 20
  Write-Host "Verificação web.izplay.tv/control: HTTP $($r.StatusCode)" -ForegroundColor Green
} catch {
  Write-Host "Aviso: não consegui validar /control/api/client/config publicamente." -ForegroundColor Yellow
  Write-Host $_.Exception.Message -ForegroundColor DarkYellow
}
