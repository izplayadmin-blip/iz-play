param(
  [string]$WebHost = "208.122.17.45",
  [string]$User = "root",
  [string]$GatewayUrl = "http://209.50.254.197"
)

$ErrorActionPreference = "Stop"

function Require-Command($Name) {
  if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
    throw "Comando '$Name' não encontrado. Instale/ative o OpenSSH Client do Windows."
  }
}

Require-Command "ssh"
Require-Command "scp"

$Remote = "$User@$WebHost"
$TempDir = Join-Path $env:TEMP ("izplay-video-gateway-fix-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force -Path $TempDir | Out-Null
$RemoteScript = Join-Path $TempDir "fix-web-video-gateway-nginx.sh"

$ScriptBody = @'
#!/usr/bin/env bash
set -euo pipefail

GATEWAY_URL="__GATEWAY_URL__"

echo "==== PASSO 1: localizar site e rotas atuais ===="
ls -l /etc/nginx/sites-enabled/ /etc/nginx/sites-available/ || true
if [ -e /etc/nginx/sites-enabled/izplay-web ]; then
  CONF="$(readlink -f /etc/nginx/sites-enabled/izplay-web)"
elif [ -f /etc/nginx/sites-available/izplay-web ]; then
  CONF="/etc/nginx/sites-available/izplay-web"
else
  CONF="$(find /etc/nginx/sites-available -maxdepth 1 -type f ! -name '*.bak-*' -exec grep -l "video-gateway\|izplay-web\|web.izplay.tv" {} \; 2>/dev/null | head -1 || true)"
fi
if [ -z "$CONF" ]; then
  echo "ERRO: não encontrei arquivo de site contendo video-gateway/izplay-web/web.izplay.tv" >&2
  exit 1
fi
echo "CONF=$CONF"
nginx -t

echo "==== locations relevantes atuais ===="
grep -nE "location|proxy_pass|video-gateway|/gateway|/control|/hls|/epg|209\.50\.254\.197|130\.250\.189\.142|38\.46\.142\.234" "$CONF" || true

echo "==== PASSO 2: comparar com backup recente ===="
ls -lt "$CONF".bak-* /etc/nginx/sites-available/*.bak-* /etc/nginx/*.bak-* 2>/dev/null | head -20 || true
BAK="$(ls -t "$CONF".bak-* 2>/dev/null | head -1 || true)"
echo "BAK=$BAK"
if [ -n "$BAK" ]; then
  echo "==== diff backup atual, primeiros 220 linhas ===="
  diff -u "$BAK" "$CONF" | sed -n '1,220p' || true
fi

echo "==== PASSO 3: avaliar /video-gateway/ ===="
python3 - <<'PY'
from pathlib import Path
import os
import re
import sys

conf = Path(os.environ["CONF"])
gateway = os.environ["GATEWAY_URL"].rstrip("/")
s = conf.read_text()

block = "\n".join([
    "    location ^~ /video-gateway/ {",
    f"        proxy_pass {gateway}/;",
    "        proxy_http_version 1.1;",
    "        proxy_set_header Host $host;",
    "        proxy_set_header X-Real-IP $remote_addr;",
    "        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;",
    "        proxy_set_header X-Forwarded-Proto $scheme;",
    "        proxy_set_header X-Forwarded-Host $host;",
    "        proxy_set_header X-Forwarded-Prefix /video-gateway;",
    "        proxy_buffering off;",
    "        proxy_read_timeout 3600s;",
    "        proxy_send_timeout 3600s;",
    "    }",
])

pattern = re.compile(r"\n\s*location\s+(?:\^~\s+)?/video-gateway/?\s*\{.*?\n\s*\}", re.S)
match = pattern.search(s)
current = match.group(0) if match else ""
needs_change = (not match) or ("209.50.254.197" not in current) or ("proxy_buffering off" not in current)

print("video_gateway_block_found=", bool(match))
if match:
    print("---- bloco /video-gateway atual ----")
    print(current.strip())
    print("---- fim bloco atual ----")

if not needs_change:
    print("NO_CHANGE_NEEDED: /video-gateway/ já aponta para 209.50.254.197 com streaming.")
    sys.exit(0)

backup = conf.with_name(conf.name + ".bak-video-gateway-" + __import__("datetime").datetime.now().strftime("%Y-%m-%d-%H%M%S"))
backup.write_text(s)
print(f"BACKUP_CREATED={backup}")

if match:
    s2 = pattern.sub("\n" + block, s, count=1)
else:
    # inserir antes da location /control/ se existir; senão antes do fechamento final do server
    control = re.search(r"\n\s*location\s+(?:\^~\s+)?/control/?\s*\{", s)
    if control:
        pos = control.start()
        s2 = s[:pos] + "\n" + block + "\n" + s[pos:]
    else:
        last = s.rfind("\n}")
        if last == -1:
            raise SystemExit("não encontrei fechamento do server block no nginx")
        s2 = s[:last] + "\n" + block + "\n" + s[last:]

conf.write_text(s2)
print("UPDATED_VIDEO_GATEWAY_BLOCK")
PY

echo "==== PASSO 4: testar nginx e recarregar ===="
nginx -t
systemctl reload nginx
echo "NGINX_OK"

echo "==== PASSO 5: validar pela borda ===="
curl -s -o /dev/null -w 'video-gateway health: %{http_code}\n' "https://web.izplay.tv/video-gateway/health"
curl -s -o /dev/null -w 'gateway health:       %{http_code}\n' "https://web.izplay.tv/gateway/health"
curl -s -o /dev/null -w 'control config:       %{http_code}\n' "https://web.izplay.tv/control/api/client/config"

echo "==== bloco /video-gateway final ===="
grep -nA16 -B2 "video-gateway" "$CONF" || true
'@

$ScriptBody = $ScriptBody.Replace("__GATEWAY_URL__", $GatewayUrl.TrimEnd("/")).Replace("`r`n", "`n")
$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($RemoteScript, $ScriptBody, $Utf8NoBom)

Write-Host "Correção segura da rota /video-gateway/ no Nginx da VPS Web" -ForegroundColor Cyan
Write-Host "Web VPS : $Remote"
Write-Host "Gateway : $GatewayUrl"
Write-Host "A senha será pedida pelo OpenSSH, se não houver chave configurada." -ForegroundColor Yellow
Write-Host ""

scp "$RemoteScript" "${Remote}:/tmp/fix-web-video-gateway-nginx.sh"
ssh $Remote "chmod +x /tmp/fix-web-video-gateway-nginx.sh && CONF='' GATEWAY_URL='$($GatewayUrl.TrimEnd("/"))' bash /tmp/fix-web-video-gateway-nginx.sh"
