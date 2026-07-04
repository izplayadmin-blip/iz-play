param(
  [string]$HostName = "208.122.17.45",
  [string]$User = "root",
  [string]$RemoteDir = "/var/www/izplay-web",
  [string]$Source = "$PSScriptRoot\..\web-player\index.html",
  [string]$ServiceWorkerSource = "$PSScriptRoot\..\web-player\service-worker.js",
  [string]$PublicUrl = "https://web.izplay.tv",
  [switch]$SkipVerify
)

$ErrorActionPreference = "Stop"

function Require-Command($Name) {
  if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
    throw "Comando '$Name' não encontrado. Instale/ative o OpenSSH Client do Windows."
  }
}

Require-Command "scp"
Require-Command "ssh"

$ResolvedSource = Resolve-Path -LiteralPath $Source
$ResolvedServiceWorkerSource = Resolve-Path -LiteralPath $ServiceWorkerSource
$Remote = "$User@$HostName"
$TmpFile = "/tmp/izplay-index.html.$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds()).new"
$TmpServiceWorkerFile = "/tmp/izplay-service-worker.js.$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds()).new"

Write-Host "Deploy do Web Player" -ForegroundColor Cyan
Write-Host "Index origem : $ResolvedSource"
Write-Host "SW origem    : $ResolvedServiceWorkerSource"
Write-Host "Destino     : ${Remote}:$RemoteDir/"
Write-Host ""
Write-Host "Este script não armazena nem recebe senha. Use chave SSH ou digite a senha no prompt do OpenSSH." -ForegroundColor Yellow
Write-Host ""

Write-Host "1/3 Enviando arquivos temporários..." -ForegroundColor Cyan
scp "$ResolvedSource" "${Remote}:$TmpFile"
if ($LASTEXITCODE -ne 0) {
  throw "Falha no upload do index.html via scp. O servidor não foi alterado."
}
scp "$ResolvedServiceWorkerSource" "${Remote}:$TmpServiceWorkerFile"
if ($LASTEXITCODE -ne 0) {
  throw "Falha no upload do service-worker.js via scp. O servidor não foi alterado."
}

$RemoteCommand = @"
set -e
test -f '$TmpFile'
test -f '$TmpServiceWorkerFile'
test -f '$RemoteDir/index.html'
stamp=`$(date +%F-%H%M%S)
backup='$RemoteDir/index.html.bak-'"`$stamp"
swbackup='$RemoteDir/service-worker.js.bak-'"`$stamp"
cp -a '$RemoteDir/index.html' "`$backup"
if [ -f '$RemoteDir/service-worker.js' ]; then cp -a '$RemoteDir/service-worker.js' "`$swbackup"; fi
mv '$TmpFile' '$RemoteDir/index.html'
mv '$TmpServiceWorkerFile' '$RemoteDir/service-worker.js'
chown www-data:www-data '$RemoteDir/index.html' '$RemoteDir/service-worker.js'
chmod 0644 '$RemoteDir/index.html' '$RemoteDir/service-worker.js'
echo "OK index_backup=`$backup service_worker_backup=`$swbackup"
"@

Write-Host "2/3 Fazendo backup remoto e ativando index.html + service-worker.js..." -ForegroundColor Cyan
ssh $Remote $RemoteCommand
if ($LASTEXITCODE -ne 0) {
  throw "Falha ao ativar arquivos no servidor. Verifique se os uploads temporários existem e se a senha SSH está correta."
}

if (-not $SkipVerify) {
  Write-Host "3/3 Verificando URL pública..." -ForegroundColor Cyan
  try {
    $Response = Invoke-WebRequest -Uri $PublicUrl -UseBasicParsing -Method Head -TimeoutSec 20
    Write-Host "HTTP $($Response.StatusCode) em $PublicUrl" -ForegroundColor Green
    $Index = Invoke-WebRequest -Uri "$PublicUrl/player/index.html" -UseBasicParsing -TimeoutSec 20
    $Sw = Invoke-WebRequest -Uri "$PublicUrl/player/service-worker.js" -UseBasicParsing -TimeoutSec 20
    if ($Index.Content -notmatch "SWARM_P2P_DEFAULT=false") { throw "P2P off por padrão não encontrado no index público." }
    if ($Index.Content -notmatch "__swarmChannelId='live-") { throw "channelId live não encontrado no index público." }
    if ($Sw.Content -notmatch "iz-play-web-v2\.1\.3") { throw "Cache v2.1.3 não encontrado no service-worker público." }
    Write-Host "Marcadores P2P/SW verificados no público." -ForegroundColor Green
  } catch {
    Write-Host "Aviso: não consegui validar $PublicUrl via HEAD. Abra em janela anônima e teste login/canal." -ForegroundColor Yellow
    Write-Host $_.Exception.Message -ForegroundColor DarkYellow
  }
} else {
  Write-Host "Verificação pública ignorada por -SkipVerify." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Deploy concluído. Teste em janela anônima: login, canais ao vivo, filmes/séries e telemetria." -ForegroundColor Green
