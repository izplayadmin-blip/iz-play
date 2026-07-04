param(
  [string]$WebHost = "208.122.17.45",
  [string]$User = "root",
  [string]$PanelUrl = "http://38.46.142.234"
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
$TempDir = Join-Path $env:TEMP ("izplay-control-fix-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force -Path $TempDir | Out-Null
$RemoteScript = Join-Path $TempDir "fix-web-control-api.sh"

$ScriptBody = @'
#!/usr/bin/env bash
set -euo pipefail

PANEL_URL="__PANEL_URL__"
CONF="/etc/nginx/sites-available/izplay-web"
test -f "$CONF"

BAK="$CONF.bak-control-$(date +%F-%H%M%S)"
cp -a "$CONF" "$BAK"

python3 - <<'PY'
from pathlib import Path
import os
import re

panel = os.environ["PANEL_URL"].rstrip("/")
p = Path("/etc/nginx/sites-available/izplay-web")
s = p.read_text()

control_block = "\n".join([
    "    location ^~ /control/ {",
    "        proxy_http_version 1.1;",
    "        proxy_set_header Host admin.izplay.tv;",
    "        proxy_set_header X-Real-IP $remote_addr;",
    "        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;",
    "        proxy_set_header X-Forwarded-Proto $scheme;",
    f"        proxy_pass {panel}/;",
    "    }",
])

pattern = re.compile(r"\n\s*location\s+(?:\^~\s+)?/control/?\s*\{.*?\n\s*\}", re.S)
if pattern.search(s):
    s = pattern.sub("\n" + control_block, s, count=1)
else:
    last = s.rfind("\n}")
    if last == -1:
        raise SystemExit("não encontrei fechamento do server block no nginx")
    s = s[:last] + "\n" + control_block + "\n" + s[last:]

p.write_text(s)
PY

nginx -t
systemctl reload nginx
echo "OK_WEB_CONTROL backup=$BAK"
curl -fsS https://web.izplay.tv/control/api/client/config | head -c 500
echo
'@

$ScriptBody = $ScriptBody.Replace("__PANEL_URL__", $PanelUrl.TrimEnd("/")).Replace("`r`n", "`n")
$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($RemoteScript, $ScriptBody, $Utf8NoBom)

Write-Host "Correção segura do /control/api do Web Player" -ForegroundColor Cyan
Write-Host "Web VPS : $Remote"
Write-Host "Painel  : $PanelUrl"
Write-Host "A senha será pedida pelo OpenSSH, se não houver chave configurada." -ForegroundColor Yellow
Write-Host ""

scp "$RemoteScript" "${Remote}:/tmp/fix-web-control-api.sh"
ssh $Remote "chmod +x /tmp/fix-web-control-api.sh && PANEL_URL='$($PanelUrl.TrimEnd("/"))' bash /tmp/fix-web-control-api.sh"

try {
  $r = Invoke-WebRequest -Uri "https://web.izplay.tv/control/api/client/config" -UseBasicParsing -TimeoutSec 20
  Write-Host "Verificação pública: HTTP $($r.StatusCode)" -ForegroundColor Green
  Write-Host ($r.Content.Substring(0, [Math]::Min(500, $r.Content.Length)))
} catch {
  Write-Host "Aviso: não consegui validar /control/api/client/config publicamente." -ForegroundColor Yellow
  Write-Host $_.Exception.Message -ForegroundColor DarkYellow
}
