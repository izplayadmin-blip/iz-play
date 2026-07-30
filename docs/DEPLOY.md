# Deploy da v2.1

> Atualizado em 2026-07-02 para a topologia real com Super Peer Brasil ativo e VPN futura isolada.
> deploy com backup, teste e rollback claro.

## Mapa de servidores

| VPS | IP | Serviços | Gerenciador |
|-----|----|----------|-------------|
| Web Player (`web.izplay.tv`) | 208.122.17.45 | nginx + TLS, estáticos em `/var/www/izplay-web` | nginx |
| Gateway | 209.50.254.197 | catálogo `127.0.0.1:3001`, mídia `127.0.0.1:4100` | pm2 |
| Proxy | 130.250.189.142 | `iptv-proxy :8080`, legado/rollback | systemd |
| Painel Admin (`admin.izplay.tv`) | 38.46.142.234 | nginx → node `:3000` | pm2 `izplay-panel` |
| Super Peer Brasil | 209.14.85.55 | SwarmCloud Server as Peer :8080, dashboard Running | pm2 super-peer |
| VPN Brasil | 209.14.84.61 | WireGuard, rede 10.66.1.0/24 | systemd |

## Segurança

Se senhas root foram compartilhadas em texto puro, trate como comprometidas. Rotacione as senhas
e prefira chave SSH. Os scripts deste projeto não gravam senha root em arquivo; o OpenSSH pede a
credencial interativamente quando não há chave configurada.

## Atualizar o Web Player

No PowerShell, a partir da raiz do projeto:

```powershell
.\scripts\deploy-webplayer.ps1
```

O script envia `web-player/index.html`, cria backup remoto em
`/var/www/izplay-web/index.html.bak-YYYY-MM-DD-HHMMSS`, troca o arquivo, ajusta `www-data`
e valida `https://web.izplay.tv`.

> Quando o `service-worker.js` mudar (ex.: bump do `CACHE_NAME`, hoje `iz-play-web-v2.1.3`),
> subir também `web-player/service-worker.js` para o mesmo diretório — assim as PWAs já
> instaladas atualizam o app shell. O SW é network-first, então o `index.html` já vem novo no
> reload mesmo sem bump.

## Atualizar o Painel Admin

```powershell
.\scripts\deploy-painel-admin.ps1
```

Destino padrão: `38.46.142.234`, `admin.izplay.tv`, diretório `/opt/izplay-v2.1/panel`.
O script pede a senha administrativa do painel e depois usa SSH/SCP para subir os arquivos.

Verificação:

```powershell
Invoke-WebRequest http://38.46.142.234/api/client/config -UseBasicParsing
```

Quando o DNS/TLS estiver ativo:

```powershell
Invoke-WebRequest https://admin.izplay.tv/api/client/config -UseBasicParsing
```

## Instalar VPN Brasil (`209.14.84.61`)

A VPN WireGuard fica em etapa isolada. Não rotear Gateway, Painel, mídia ou serviços pela VPN nesta passagem.

```bash
# Rodar na VPS 209.14.84.61, com backup/validação manual
apt update && apt upgrade -y
apt install -y wireguard qrencode ufw
DEFIF=$(ip route | awk '/default/ {print $5; exit}')
sed -i 's/^#\?net.ipv4.ip_forward=.*/net.ipv4.ip_forward=1/' /etc/sysctl.conf && sysctl -p

cd /etc/wireguard && umask 077
wg genkey | tee server_private.key | wg pubkey > server_public.key
cat > /etc/wireguard/wg0.conf <<EOF
[Interface]
Address = 10.66.1.1/24
ListenPort = 51820
PrivateKey = $(cat server_private.key)
PostUp   = iptables -A FORWARD -i wg0 -j ACCEPT; iptables -t nat -A POSTROUTING -o $DEFIF -j MASQUERADE
PostDown = iptables -D FORWARD -i wg0 -j ACCEPT; iptables -t nat -D POSTROUTING -o $DEFIF -j MASQUERADE
EOF

ufw allow OpenSSH && ufw allow 51820/udp && ufw --force enable
systemctl enable --now wg-quick@wg0
wg show wg0
```

Gerar clientes em `/etc/wireguard` e manter `.conf`/chaves fora do Git.

## Reapontar `/control/` para o Painel Admin novo

Depois de subir o Painel Admin, atualize o Web Player oficial para enviar telemetria/config ao painel novo:

```powershell
.\scripts\fix-web-control-api.ps1
```

Esse script faz backup do Nginx do Web Player, altera somente o bloco `/control/`, testa `nginx -t`, recarrega Nginx e valida `https://web.izplay.tv/control/api/client/config`.

## Atualizar catálogo no Gateway

Serviço: `/opt/iz-catalog-gateway`, processo PM2 `iz-catalog-gateway`.

Variáveis principais:

```bash
PORT=3001
DEFAULT_UPSTREAM=http://cxst.shop
PANEL_URL=http://38.46.142.234
CATALOG_CACHE_TTL_MS=120000
REQUEST_TIMEOUT_MS=25000
```

Passos:

```bash
cp -a /opt/iz-catalog-gateway/server.js{,.bak-$(date +%F-%H%M%S)}
node --check /opt/iz-catalog-gateway/server.js
pm2 restart iz-catalog-gateway --update-env
pm2 save
```

## Regra crítica do Web Player: catálogo vs mídia

No Web Player, não confundir as rotas:

- catálogo/listagem: `/gateway`;
- mídia/transcode: `/video-gateway/transcode?url=<URL direta do provedor>`.

O `iz-gateway` **não** deve transcodificar `https://web.izplay.tv/gateway/live/...`. Essa URL já é proxy de catálogo/borda e pode retornar `403 Forbidden` quando o FFmpeg tenta ler playlist/segmentos. O transcode deve receber algo como:

```text
http://cxst.shop/live/USER/PASS/STREAM_ID.m3u8
```

Teste útil no Gateway quando canais do Web ficam em tela preta:

```bash
pm2 logs iz-gateway --lines 60 --nostream
# procurar por: transcode:start url=https://web.izplay.tv/gateway/live/...  -> ERRADO
# correto:      transcode:start url=http://cxst.shop/live/...
```

## Atualizar mídia no Gateway

Serviço: `iz-gateway`, porta local `127.0.0.1:4100`.

```bash
pm2 restart iz-gateway
pm2 save
```

## Super Peer Brasil (SwarmCloud)

**Produção atual:** o Super Peer oficial já está ativo em `209.14.85.55:8080`, processo PM2 `super-peer`, registrado no dashboard SwarmCloud como **SUPER PEER BRASIL** e visível no Painel Admin em **P2P / SwarmCloud**.

Verificações:

```bash
# Na VPS do Super Peer
pm2 status super-peer
curl -s http://127.0.0.1:8080/stats | head -c 500

# Público
curl -s http://209.14.85.55:8080/stats | head -c 500
```

**Não reinstalar por cima** enquanto estiver saudável. Se precisar atualizar/reinstalar, planejar janela, fazer backup e validar no dashboard SwarmCloud. Rodar o instalador oficial novamente sem necessidade pode criar processo concorrente ou derrubar o nó já registrado.

Alternativa/fallback de referência (não usada em produção agora): `supernode/` (Node + Chromium headless, controle próprio), ver `supernode/README.md`.

## Nginx

- Web VPS: catálogo/mídia para Gateway; `/control/` para Painel Admin.
- Gateway VPS: `/player_api.php`, `/get.php`, `/live/`, `/movie/`, `/series/`, `/asset`
  para catálogo; `/proxy`, `/transcode`, `/hls` para mídia.
- Painel Admin: `admin.izplay.tv`/porta 80 para `127.0.0.1:3000`.

Sempre fazer backup do arquivo, rodar `nginx -t` e só então `systemctl reload nginx`.

## Verificações rápidas

```bash
# Web
curl -I https://web.izplay.tv
curl -s -o /dev/null -w '%{http_code}\n' https://web.izplay.tv/control/api/client/config

# Painel Admin
curl -s http://127.0.0.1:3000/api/client/config

# Gateway
curl -s http://127.0.0.1:3001/health
curl -s http://127.0.0.1:4100/health

 # VPN futura, quando instalada
wg show wg0
```

## Rollback

- Web Player: restaurar `/var/www/izplay-web/index.html.bak-*`.
- Nginx: restaurar `.bak-*` do site correspondente e recarregar.
- Painel: restaurar `/opt/izplay-v2.1/panel.bak-*` se necessário.
- Proxy `130.250.189.142` permanece como ponto de rollback legado.
