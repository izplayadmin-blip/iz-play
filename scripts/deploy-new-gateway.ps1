param(
  [Parameter(Mandatory=$true)][string]$HostName,
  [string]$User = "root",
  [int]$Port = 22,
  [string]$RemoteRoot = "/opt/izplay-v2.1",
  [string]$CatalogUpstreams = "http://cxst.shop,http://sopvrt.shop",
  [string]$MediaGatewayToken = "",
  [string]$Password = ""
)

$ErrorActionPreference = "Stop"
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$TempDir = Join-Path $env:TEMP ("izplay-new-gateway-" + [guid]::NewGuid().ToString("N"))
$Bundle = Join-Path $TempDir "izplay-gateway-bundle.tar.gz"
$RemoteBundle = "/tmp/izplay-gateway-bundle.tar.gz"
$RemoteScript = "/tmp/izplay-gateway-bootstrap.sh"

New-Item -ItemType Directory -Path $TempDir | Out-Null

function Invoke-Tar {
  Push-Location $RepoRoot
  try {
    tar -czf $Bundle gateway catalog-gateway deploy/izplay-gateway.service deploy/izplay-catalog-gateway.service
  } finally {
    Pop-Location
  }
}

function PuttyArgs {
  $args = @("-P", "$Port")
  if ($Password) { $args += @("-pw", $Password) }
  return $args
}

try {
  Invoke-Tar

  $bootstrap = @"
set -euo pipefail

REMOTE_ROOT="$RemoteRoot"
CATALOG_UPSTREAMS="$CatalogUpstreams"
MEDIA_GATEWAY_TOKEN="$MediaGatewayToken"

export DEBIAN_FRONTEND=noninteractive

apt-get update
apt-get install -y ca-certificates curl tar nodejs npm ffmpeg
npm install -g pm2

mkdir -p "`$REMOTE_ROOT"
tar -xzf "$RemoteBundle" -C "`$REMOTE_ROOT"

mkdir -p /etc/izplay /tmp/iz-gateway-hls
cat >/etc/izplay/catalog-gateway.env <<EOF
PORT=3001
CATALOG_GATEWAY_PORT=3001
CATALOG_UPSTREAMS=`$CATALOG_UPSTREAMS
CATALOG_ALLOWED_ORIGINS=https://izplay.tv,https://www.izplay.tv,https://web.izplay.tv
EOF

cat >/etc/izplay/gateway.env <<EOF
PORT=4100
GATEWAY_PORT=4100
GATEWAY_HLS_ROOT=/tmp/iz-gateway-hls
GATEWAY_TOKEN=`$MEDIA_GATEWAY_TOKEN
EOF

cd "`$REMOTE_ROOT/catalog-gateway"
npm install --omit=dev
node --check server.js

cd "`$REMOTE_ROOT/gateway"
npm install --omit=dev
node --check server.js

pm2 delete iz-catalog-gateway >/dev/null 2>&1 || true
pm2 delete iz-gateway >/dev/null 2>&1 || true

cd "`$REMOTE_ROOT/catalog-gateway"
set -a
. /etc/izplay/catalog-gateway.env
set +a
pm2 start server.js --name iz-catalog-gateway --update-env

cd "`$REMOTE_ROOT/gateway"
set -a
. /etc/izplay/gateway.env
set +a
pm2 start server.js --name iz-gateway --update-env

pm2 save
pm2 startup systemd -u root --hp /root >/tmp/izplay-pm2-startup.txt || true

curl -fsS http://127.0.0.1:3001/health
echo
curl -fsS http://127.0.0.1:4100/health
echo
pm2 status
"@

  $BootstrapLocal = Join-Path $TempDir "izplay-gateway-bootstrap.sh"
  Set-Content -Path $BootstrapLocal -Value $bootstrap -NoNewline -Encoding ASCII

  $putty = PuttyArgs
  & pscp @putty $Bundle "${User}@${HostName}:$RemoteBundle"
  if ($LASTEXITCODE -ne 0) { throw "Falha ao enviar bundle para $HostName" }

  & pscp @putty $BootstrapLocal "${User}@${HostName}:$RemoteScript"
  if ($LASTEXITCODE -ne 0) { throw "Falha ao enviar bootstrap para $HostName" }

  & plink @putty -batch "${User}@${HostName}" "chmod +x $RemoteScript && bash $RemoteScript"
  if ($LASTEXITCODE -ne 0) { throw "Bootstrap remoto falhou em $HostName" }
} finally {
  Remove-Item -Recurse -Force -LiteralPath $TempDir -ErrorAction SilentlyContinue
}
