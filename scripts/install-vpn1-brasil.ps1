param(
  [string]$HostName = "209.14.85.55",
  [string]$User = "root",
  [string]$VpnSubnet = "10.66.0.0/24",
  [string]$ServerVpnIp = "10.66.0.1/24",
  [string]$ClientVpnIp = "10.66.0.2/32",
  [int]$ListenPort = 51820
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
$TempDir = Join-Path $env:TEMP ("izplay-vpn1-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $TempDir | Out-Null
$RemoteScriptLocal = Join-Path $TempDir "install-vpn1-brasil.sh"

@"
#!/usr/bin/env bash
set -euo pipefail

export DEBIAN_FRONTEND=noninteractive

HOST_IP="$HostName"
VPN_SUBNET="$VpnSubnet"
SERVER_VPN_IP="$ServerVpnIp"
CLIENT_VPN_IP="$ClientVpnIp"
LISTEN_PORT="$ListenPort"

echo "[vpn1] Atualizando pacotes..."
apt-get update
apt-get install -y wireguard qrencode iproute2 iptables ca-certificates

echo "[vpn1] Ativando encaminhamento IPv4..."
cat >/etc/sysctl.d/99-izplay-vpn.conf <<SYSCTL
net.ipv4.ip_forward=1
SYSCTL
sysctl --system >/dev/null

WAN_IF=`$(ip route show default | awk '{print `$5; exit}')
if [ -z "`$WAN_IF" ]; then
  echo "Não consegui detectar a interface WAN padrão." >&2
  exit 1
fi

install -d -m 700 /etc/wireguard
umask 077

if [ ! -f /etc/wireguard/server_private.key ]; then
  wg genkey | tee /etc/wireguard/server_private.key | wg pubkey >/etc/wireguard/server_public.key
fi
if [ ! -f /etc/wireguard/client_izplay_private.key ]; then
  wg genkey | tee /etc/wireguard/client_izplay_private.key | wg pubkey >/etc/wireguard/client_izplay_public.key
fi

SERVER_PRIVATE=`$(cat /etc/wireguard/server_private.key)
SERVER_PUBLIC=`$(cat /etc/wireguard/server_public.key)
CLIENT_PRIVATE=`$(cat /etc/wireguard/client_izplay_private.key)
CLIENT_PUBLIC=`$(cat /etc/wireguard/client_izplay_public.key)

cat >/etc/wireguard/wg0.conf <<WGCONF
[Interface]
Address = `$SERVER_VPN_IP
ListenPort = `$LISTEN_PORT
PrivateKey = `$SERVER_PRIVATE
PostUp = iptables -A FORWARD -i wg0 -j ACCEPT; iptables -A FORWARD -o wg0 -j ACCEPT; iptables -t nat -A POSTROUTING -s `$VPN_SUBNET -o `$WAN_IF -j MASQUERADE
PostDown = iptables -D FORWARD -i wg0 -j ACCEPT; iptables -D FORWARD -o wg0 -j ACCEPT; iptables -t nat -D POSTROUTING -s `$VPN_SUBNET -o `$WAN_IF -j MASQUERADE

[Peer]
PublicKey = `$CLIENT_PUBLIC
AllowedIPs = `$CLIENT_VPN_IP
WGCONF

cat >/root/izplay-vpn1-client.conf <<CLIENTCONF
[Interface]
PrivateKey = `$CLIENT_PRIVATE
Address = `$CLIENT_VPN_IP
DNS = 1.1.1.1, 8.8.8.8

[Peer]
PublicKey = `$SERVER_PUBLIC
Endpoint = `$HOST_IP:`$LISTEN_PORT
AllowedIPs = 0.0.0.0/0
PersistentKeepalive = 25
CLIENTCONF
chmod 600 /root/izplay-vpn1-client.conf /etc/wireguard/wg0.conf

if command -v ufw >/dev/null 2>&1; then
  ufw allow "`$LISTEN_PORT/udp" || true
fi

echo "[vpn1] Subindo wg0..."
systemctl enable --now wg-quick@wg0
systemctl restart wg-quick@wg0

echo
echo "===== STATUS WG0 ====="
wg show wg0
echo
echo "===== CLIENTE SALVO EM /root/izplay-vpn1-client.conf ====="
cat /root/izplay-vpn1-client.conf
echo
echo "===== QR CODE DO CLIENTE ====="
qrencode -t ansiutf8 </root/izplay-vpn1-client.conf || true
echo
echo "OK VPN1_BRASIL"
"@ | Set-Content -LiteralPath $RemoteScriptLocal -Encoding ASCII

Write-Host "Instalação VPN1 Brasil" -ForegroundColor Cyan
Write-Host "Destino: ${Remote}"
Write-Host "A senha será pedida pelo OpenSSH, se não houver chave configurada." -ForegroundColor Yellow

scp "$RemoteScriptLocal" "${Remote}:/tmp/install-vpn1-brasil.sh"
ssh $Remote "chmod +x /tmp/install-vpn1-brasil.sh && bash /tmp/install-vpn1-brasil.sh"

Remove-Item -LiteralPath $TempDir -Recurse -Force -ErrorAction SilentlyContinue
