param(
  [string]$HostName = "208.122.17.45",
  [string]$User = "root",
  [string]$RemoteDir = "/var/www/izplay-web"
)

$ErrorActionPreference = "Stop"
$Remote = "$User@$HostName"

$RemoteCommand = @'
set -e
cd "__REMOTE_DIR__"
latest_index="$(ls -t index.html.bak-* | head -1)"
latest_sw="$(ls -t service-worker.js.bak-* 2>/dev/null | head -1 || true)"
test -n "$latest_index"
cp -a "$latest_index" index.html
if [ -n "$latest_sw" ]; then
  cp -a "$latest_sw" service-worker.js
fi
chown www-data:www-data index.html service-worker.js 2>/dev/null || chown www-data:www-data index.html
echo "ROLLBACK_OK index=$latest_index sw=$latest_sw"
'@.Replace("__REMOTE_DIR__", $RemoteDir)

Write-Host "Rollback do Web Player para o último backup" -ForegroundColor Cyan
Write-Host "Destino: $Remote:$RemoteDir"
Write-Host "A senha será pedida pelo OpenSSH, se não houver chave configurada." -ForegroundColor Yellow
Write-Host ""

ssh $Remote $RemoteCommand

try {
  $r = Invoke-WebRequest -Uri "https://web.izplay.tv" -UseBasicParsing -TimeoutSec 20
  Write-Host "Verificação pública: HTTP $($r.StatusCode)" -ForegroundColor Green
} catch {
  Write-Host "Aviso: não consegui validar https://web.izplay.tv" -ForegroundColor Yellow
  Write-Host $_.Exception.Message -ForegroundColor DarkYellow
}
