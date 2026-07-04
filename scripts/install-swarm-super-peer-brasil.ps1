param(
  [string]$HostName = "209.14.85.55",
  [string]$User = "root",
  [int]$Port = 8080,
  [string]$AccessToken = ""
)

$ErrorActionPreference = "Stop"

function Require-Command($Name) {
  if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
    throw "Comando '$Name' não encontrado. Instale/ative o OpenSSH Client do Windows."
  }
}

Require-Command "scp"
Require-Command "ssh"

$Remote = "$User@$HostName"
$TempDir = Join-Path $env:TEMP ("izplay-super-peer-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $TempDir | Out-Null
$RemoteScriptLocal = Join-Path $TempDir "install-swarm-super-peer-brasil.sh"

$AccessTokenLine = ""
if (-not [string]::IsNullOrWhiteSpace($AccessToken)) {
  $EscapedToken = $AccessToken.Replace("'", "'\''")
  $AccessTokenLine = "echo 'accessToken=$EscapedToken' >> /root/super-peer/.env"
}

@"
#!/usr/bin/env bash
set -euo pipefail

export DEBIAN_FRONTEND=noninteractive
PORT="$Port"

echo "[super-peer] Instalando dependências base..."
echo "[super-peer] Aguardando locks do apt/dpkg, se houver..."
for i in `$(seq 1 60); do
  if fuser /var/lib/dpkg/lock-frontend >/dev/null 2>&1 || fuser /var/lib/apt/lists/lock >/dev/null 2>&1 || fuser /var/cache/apt/archives/lock >/dev/null 2>&1; then
    sleep 5
  else
    break
  fi
done
dpkg --configure -a || true
apt-get update
apt-get install -y curl wget ca-certificates ufw

echo "[super-peer] Instalando/atualizando Super Peer SwarmCloud na porta `$PORT..."
cd /root
wget -qN https://cdn.swarmcloud.net/super-peer.sh
bash super-peer.sh --port "`$PORT"

if [ -d /root/super-peer ]; then
  cd /root/super-peer
  if ! grep -q '^listenPort=' .env 2>/dev/null; then
    echo "listenPort=`$PORT" >> .env
  fi
  $AccessTokenLine
fi

echo "[super-peer] Liberando firewall..."
if command -v ufw >/dev/null 2>&1; then
  ufw allow "`$PORT/tcp" || true
fi

echo "[super-peer] Persistência PM2..."
if command -v pm2 >/dev/null 2>&1; then
  pm2 restart super-peer >/dev/null 2>&1 || true
  pm2 save || true
  pm2 startup systemd -u root --hp /root >/tmp/izplay-super-peer-pm2-startup.log 2>&1 || true
fi

echo
echo "===== PM2 ====="
pm2 list || true
echo
echo "===== TESTE LOCAL /ping ====="
curl -fsS -X POST "http://127.0.0.1:`$PORT/ping" -H 'Content-Type: application/json' -d '{"bandwidth":200}' || true
echo
echo
echo "===== TESTE LOCAL /stats ====="
curl -fsS "http://127.0.0.1:`$PORT/stats" | head -c 1000 || true
echo
echo
echo "OK SUPER_PEER_BRASIL url=http://${HostName}:`$PORT"
"@ | Set-Content -LiteralPath $RemoteScriptLocal -Encoding ASCII

Write-Host "Instalação Super Peer Brasil" -ForegroundColor Cyan
Write-Host "Destino: ${Remote}"
Write-Host "URL pública esperada: http://${HostName}:$Port"
Write-Host "A senha será pedida pelo OpenSSH, se não houver chave configurada." -ForegroundColor Yellow

scp "$RemoteScriptLocal" "${Remote}:/tmp/install-swarm-super-peer-brasil.sh"
if ($LASTEXITCODE -ne 0) {
  throw "Falha ao enviar instalador para a VPS."
}

ssh $Remote "chmod +x /tmp/install-swarm-super-peer-brasil.sh && bash /tmp/install-swarm-super-peer-brasil.sh"
if ($LASTEXITCODE -ne 0) {
  throw "Falha durante instalação do Super Peer."
}

try {
  $Response = Invoke-WebRequest -Uri "http://${HostName}:$Port/ping" -UseBasicParsing -Method Post -Body '{"bandwidth":200}' -ContentType 'application/json' -TimeoutSec 15
  Write-Host "Ping público Super Peer: HTTP $($Response.StatusCode)" -ForegroundColor Green
} catch {
  Write-Host "Aviso: não consegui validar /ping publicamente. Confira firewall/provedor e cadastro no dashboard." -ForegroundColor Yellow
  Write-Host $_.Exception.Message -ForegroundColor DarkYellow
}

Remove-Item -LiteralPath $TempDir -Recurse -Force -ErrorAction SilentlyContinue
