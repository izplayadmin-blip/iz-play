param(
  [string]$GatewayHost = "209.50.254.197",
  [string]$User = "root",
  [string]$StreamId = "1"
)

$ErrorActionPreference = "Stop"

function Require-Command($Name) {
  if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
    throw "Comando '$Name' não encontrado. Instale/ative o OpenSSH Client do Windows."
  }
}

function BashSingleQuote($Value) {
  $s = [string]$Value
  return "'" + $s.Replace("'", "'\''") + "'"
}

Require-Command "ssh"
Require-Command "scp"

$XtreamUser = Read-Host "Xtream usuário"
$SecurePass = Read-Host "Xtream senha" -AsSecureString
$Ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecurePass)
try {
  $XtreamPass = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($Ptr)
} finally {
  if ($Ptr -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($Ptr) }
}

if ([string]::IsNullOrWhiteSpace($XtreamUser) -or [string]::IsNullOrWhiteSpace($XtreamPass)) {
  throw "Usuário/senha Xtream são obrigatórios para testar o provedor."
}

$Remote = "$User@$GatewayHost"
$TempDir = Join-Path $env:TEMP ("izplay-gateway-diag-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force -Path $TempDir | Out-Null
$RemoteScript = Join-Path $TempDir "diagnose-gateway-provider.sh"

$ScriptBody = @'
#!/usr/bin/env bash
set -euo pipefail

XTU=__XTU__
XTP=__XTP__
SID=__SID__

echo "===== PM2 LIST ====="
pm2 list || true

echo
echo "===== PROVEDOR DIRETO ====="
curl -sS -o /dev/null -w 'cxst   : %{http_code}  %{time_total}s\n' "http://cxst.shop/live/$XTU/$XTP/$SID.ts" || true
curl -sS -o /dev/null -w 'sopvrt : %{http_code}  %{time_total}s\n' "http://sopvrt.shop/live/$XTU/$XTP/$SID.ts" || true

echo
echo "===== TRANSCODE LOCAL ====="
curl -sS -o /dev/null -w 'transcode cxst  : %{http_code}  %{time_total}s\n' "http://127.0.0.1:4100/transcode?url=http://cxst.shop/live/$XTU/$XTP/$SID.ts" || true
curl -sS -o /dev/null -w 'transcode sopvrt: %{http_code}  %{time_total}s\n' "http://127.0.0.1:4100/transcode?url=http://sopvrt.shop/live/$XTU/$XTP/$SID.ts" || true

echo
echo "===== LOGS IZ-GATEWAY ====="
pm2 logs iz-gateway --lines 60 --nostream || true

echo
echo "===== SUPER/PEER/NODE NO GATEWAY ====="
pm2 list | grep -iE 'super|peer|node' || true
'@

$ScriptBody = $ScriptBody.Replace("__XTU__", (BashSingleQuote $XtreamUser))
$ScriptBody = $ScriptBody.Replace("__XTP__", (BashSingleQuote $XtreamPass))
$ScriptBody = $ScriptBody.Replace("__SID__", (BashSingleQuote $StreamId))
$ScriptBody = $ScriptBody.Replace("`r`n", "`n")
$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($RemoteScript, $ScriptBody, $Utf8NoBom)

Write-Host "Diagnóstico Gateway/provedor" -ForegroundColor Cyan
Write-Host "Gateway: $Remote"
Write-Host "Stream ID de teste: $StreamId"
Write-Host "A senha SSH será pedida pelo OpenSSH, se não houver chave configurada." -ForegroundColor Yellow
Write-Host ""

$RemotePath = "/root/diagnose-gateway-provider.sh"
scp "$RemoteScript" "${Remote}:$RemotePath"
if ($LASTEXITCODE -ne 0) {
  throw "Falha no scp ao enviar o diagnóstico para ${Remote}:$RemotePath"
}

ssh $Remote "ls -l /root/diagnose-gateway-provider.sh && chmod +x /root/diagnose-gateway-provider.sh && bash /root/diagnose-gateway-provider.sh"
if ($LASTEXITCODE -ne 0) {
  throw "Diagnóstico remoto terminou com erro. Veja a saída acima."
}
