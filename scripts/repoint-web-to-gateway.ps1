param(
  [string]$WebHost = "208.122.17.45",
  [string]$User = "root",
  [string]$GatewayHost = "38.46.142.232"
)

$ErrorActionPreference = "Stop"

function Require-Command($Name) {
  if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
    throw "Comando '$Name' nao encontrado. Instale/ative o OpenSSH Client do Windows."
  }
}

Require-Command "ssh"

$Remote = "$User@$WebHost"
$Command = @"
set -euo pipefail
gateway='$GatewayHost'
conf=`$(find /etc/nginx/sites-available /etc/nginx/sites-enabled -maxdepth 1 -type f 2>/dev/null | xargs grep -l 'web.izplay.tv\\|izplay-web\\|/video-gateway/\\|/gateway/' | head -1)
if [ -z "`$conf" ]; then
  echo "ERRO: nao encontrei config Nginx do Web Player" >&2
  exit 1
fi
stamp=`$(date +%F-%H%M%S)
cp -a "`$conf" "`$conf.bak-repoint-gateway-`$stamp"
python3 - "`$conf" "`$gateway" <<'PY'
import re, sys
from pathlib import Path
path = Path(sys.argv[1])
gateway = sys.argv[2]
text = path.read_text()

catalog_locations = (
    '= /gateway/player_api.php',
    '/gateway/',
    '= /player_api.php',
    '= /get.php',
    '/live/',
    '/movie/',
    '/series/',
    '/asset',
)
media_locations = (
    '/video-gateway/',
    '/hls/',
    '/epg/',
)

def rewrite_block(match):
    block = match.group(0)
    header = match.group(1).strip()
    if any(header.startswith(loc) for loc in catalog_locations):
        block = re.sub(r'proxy_pass\s+http://[^;]*(/player_api\.php\$is_args\$args);',
                       f'proxy_pass http://{gateway}/gateway\\1;', block)
        block = re.sub(r'proxy_pass\s+http://[^;]*(/get\.php\$is_args\$args);',
                       f'proxy_pass http://{gateway}/gateway\\1;', block)
        block = re.sub(r'proxy_pass\s+http://[^;]*(/asset\$is_args\$args);',
                       f'proxy_pass http://{gateway}/gateway\\1;', block)
        block = re.sub(r'proxy_pass\s+http://[^;]+/?;',
                       f'proxy_pass http://{gateway}/gateway/;', block)
        block = re.sub(r'proxy_set_header Host [^;]+;',
                       f'proxy_set_header Host {gateway};', block)
        return block
    if any(header.startswith(loc) for loc in media_locations):
        if header.startswith('/hls/'):
            block = re.sub(r'proxy_pass\s+http://[^;]+/hls/;',
                           f'proxy_pass http://{gateway}/hls/;', block)
        else:
            block = re.sub(r'proxy_pass\s+http://[^;]+/?;',
                           f'proxy_pass http://{gateway}/;', block)
        block = re.sub(r'proxy_set_header Host [^;]+;',
                       f'proxy_set_header Host {gateway};', block)
        block = block.replace('http://209.50.254.197/proxy?url=', f'http://{gateway}/proxy?url=')
        return block
    return block

text = re.sub(r'location\s+([^{]+)\{.*?\n\s*\}', rewrite_block, text, flags=re.S)
path.write_text(text)
PY
nginx -t
systemctl reload nginx
curl -s -o /dev/null -w 'gateway health: %{http_code}\n' "https://web.izplay.tv/gateway/health"
curl -s -o /dev/null -w 'video health:   %{http_code}\n' "https://web.izplay.tv/video-gateway/health"
echo "OK_WEB_REPOINT_GATEWAY backup=`$conf.bak-repoint-gateway-`$stamp"
"@

ssh $Remote $Command
