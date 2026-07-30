param(
  [string]$HostName = "38.46.142.234",
  [string]$User = "root",
  [string]$RemoteRoot = "/opt/izplay-v2.1",
  [string]$PanelUser = "admin",
  [int]$PanelPort = 3000,
  [string]$ServerName = "admin.izplay.tv",
  [string]$XuiBaseUrl = "",
  [string]$XuiAccessCode = "",
  [string]$XuiApiKey = ""
)

$ErrorActionPreference = "Stop"

function Require-Command($Name) {
  if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
    throw "Comando '$Name' não encontrado. Instale/ative o OpenSSH Client do Windows."
  }
}

function ShellQuote([string]$Value) {
  return "'" + $Value.Replace("'", "'`"'" + '"' + "'`"'") + "'"
}

Require-Command "tar"
Require-Command "scp"
Require-Command "ssh"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$PanelDir = Join-Path $RepoRoot "panel"
if (-not (Test-Path (Join-Path $PanelDir "server.js"))) {
  throw "Não encontrei panel/server.js em $PanelDir"
}

$SecurePanelPass = Read-Host "Digite a senha ADMIN do painel IZ Play (não é a senha root da VPS)" -AsSecureString
$Ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecurePanelPass)
try {
  $PanelPass = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($Ptr)
} finally {
  [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($Ptr)
}
if ([string]::IsNullOrWhiteSpace($PanelPass)) {
  throw "PANEL_PASS não pode ficar vazio."
}

if ([string]::IsNullOrWhiteSpace($XuiBaseUrl)) {
  $XuiBaseUrl = Read-Host "URL base do XuiOne (deixe vazio para nao configurar)"
}
if (-not [string]::IsNullOrWhiteSpace($XuiBaseUrl) -and [string]::IsNullOrWhiteSpace($XuiAccessCode)) {
  $XuiAccessCode = Read-Host "Access code do XuiOne"
}
if (-not [string]::IsNullOrWhiteSpace($XuiBaseUrl) -and [string]::IsNullOrWhiteSpace($XuiApiKey)) {
  $SecureXuiApiKey = Read-Host "API key do XuiOne" -AsSecureString
  $XuiPtr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureXuiApiKey)
  try {
    $XuiApiKey = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($XuiPtr)
  } finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($XuiPtr)
  }
}

$Remote = "$User@$HostName"
$TempDir = Join-Path $env:TEMP ("izplay-panel-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $TempDir | Out-Null
$Archive = Join-Path $TempDir "izplay-panel.tgz"
$EnvFile = Join-Path $TempDir "panel.env"
$RemoteScriptLocal = Join-Path $TempDir "deploy-painel-admin.sh"

Write-Host "Empacotando painel sem node_modules..." -ForegroundColor Cyan
Push-Location $RepoRoot
try {
  tar -czf "$Archive" --exclude="./panel/node_modules" --exclude="./panel/.env" --exclude="./panel/data/*.bak" panel
} finally {
  Pop-Location
}

@"
PANEL_PORT=$PanelPort
PORT=$PanelPort
PANEL_USER=$(ShellQuote $PanelUser)
PANEL_PASS=$(ShellQuote $PanelPass)
PANEL_ALLOWED_ORIGINS='https://izplay.tv,https://www.izplay.tv,https://web.izplay.tv,https://admin.izplay.tv,http://admin.izplay.tv,http://38.46.142.234'
PANEL_TRUST_PROXY=1
XUI_BASE_URL=$(ShellQuote $XuiBaseUrl)
XUI_ACCESS_CODE=$(ShellQuote $XuiAccessCode)
XUI_API_KEY=$(ShellQuote $XuiApiKey)
XUI_ALLOW_INSECURE_HTTP=$($XuiBaseUrl.StartsWith('http://').ToString().ToLowerInvariant())
XUI_TIMEOUT_MS=8000
XUI_SYNC_INTERVAL_MS=60000
"@ | Set-Content -LiteralPath $EnvFile -Encoding ASCII

@"
#!/usr/bin/env bash
set -euo pipefail

export DEBIAN_FRONTEND=noninteractive
REMOTE_ROOT="$RemoteRoot"
PANEL_PORT="$PanelPort"
SERVER_NAME="$ServerName"

echo "[painel] Instalando base..."
apt-get update
apt-get install -y curl ca-certificates gnupg nginx tar

if ! command -v node >/dev/null 2>&1 || ! node -v | grep -Eq '^v(18|20|22)\.'; then
  echo "[painel] Instalando Node.js 20..."
  curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
  apt-get install -y nodejs
fi

if ! command -v pm2 >/dev/null 2>&1; then
  npm install -g pm2
fi

install -d -m 755 "`$REMOTE_ROOT"
install -d -m 700 /etc/izplay

if [ -d "`$REMOTE_ROOT/panel" ]; then
  cp -a "`$REMOTE_ROOT/panel" "`$REMOTE_ROOT/panel.bak-`$(date +%F-%H%M%S)"
  rm -rf /tmp/izplay-panel-data.keep
  mkdir -p /tmp/izplay-panel-data.keep
  for f in reports.json devices.json p2p.json commands.json updates.json; do
    if [ -f "`$REMOTE_ROOT/panel/data/`$f" ]; then
      cp -a "`$REMOTE_ROOT/panel/data/`$f" "/tmp/izplay-panel-data.keep/`$f"
    fi
  done
  if [ -d "`$REMOTE_ROOT/panel/data/apks" ]; then
    cp -a "`$REMOTE_ROOT/panel/data/apks" /tmp/izplay-panel-data.keep/apks
  fi
fi

tar -xzf /tmp/izplay-panel.tgz -C "`$REMOTE_ROOT"
if [ -d /tmp/izplay-panel-data.keep ]; then
  mkdir -p "`$REMOTE_ROOT/panel/data"
  cp -an /tmp/izplay-panel-data.keep/*.json "`$REMOTE_ROOT/panel/data/" 2>/dev/null || true
  if [ -d /tmp/izplay-panel-data.keep/apks ]; then
    cp -a /tmp/izplay-panel-data.keep/apks "`$REMOTE_ROOT/panel/data/apks"
  fi
fi
mv /tmp/panel.env.new /etc/izplay/panel.env
chmod 600 /etc/izplay/panel.env

cd "`$REMOTE_ROOT/panel"
if [ -f package-lock.json ]; then
  npm ci --omit=dev
else
  npm install --omit=dev
fi

set -a
. /etc/izplay/panel.env
set +a

pm2 delete izplay-panel >/dev/null 2>&1 || true
pm2 start server.js --name izplay-panel --time --update-env
pm2 save
pm2 startup systemd -u root --hp /root >/tmp/izplay-pm2-startup.log 2>&1 || true

cat >/etc/nginx/sites-available/izplay-panel <<NGINX
server {
    listen 80;
    server_name `$SERVER_NAME;

    client_max_body_size 150m;

    location / {
        proxy_pass http://127.0.0.1:`$PANEL_PORT;
        proxy_http_version 1.1;
        proxy_set_header Host \`$host;
        proxy_set_header X-Real-IP \`$remote_addr;
        proxy_set_header X-Forwarded-For \`$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \`$scheme;
    }
}
NGINX

ln -sf /etc/nginx/sites-available/izplay-panel /etc/nginx/sites-enabled/izplay-panel
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl enable --now nginx
systemctl reload nginx

echo
echo "===== PM2 ====="
pm2 list
echo
echo "===== HEALTH LOCAL ====="
curl -fsS "http://127.0.0.1:`$PANEL_PORT/api/client/config" | head -c 300 || true
echo
echo "OK PAINEL_ADMIN"
"@ | Set-Content -LiteralPath $RemoteScriptLocal -Encoding ASCII

Write-Host "Deploy Painel Admin" -ForegroundColor Cyan
Write-Host "Destino: ${Remote}:${RemoteRoot}/panel"
Write-Host "A senha root da VPS será pedida pelo OpenSSH, se não houver chave configurada." -ForegroundColor Yellow

scp "$Archive" "${Remote}:/tmp/izplay-panel.tgz"
scp "$EnvFile" "${Remote}:/tmp/panel.env.new"
scp "$RemoteScriptLocal" "${Remote}:/tmp/deploy-painel-admin.sh"
ssh $Remote "chmod +x /tmp/deploy-painel-admin.sh && bash /tmp/deploy-painel-admin.sh"

Remove-Item -LiteralPath $TempDir -Recurse -Force -ErrorAction SilentlyContinue
