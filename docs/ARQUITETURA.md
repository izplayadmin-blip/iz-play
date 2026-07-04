# Arquitetura v2.1

> Atualizado em 2026-07-02 para refletir a topologia real de produção com Web, Gateway, Proxy, Painel Admin e Super Peer.
> O exemplo `deploy/nginx-izplay-v2.1.conf` é referência histórica/single-host e não
> representa sozinho a produção atual.

## Visão geral de produção

```text
Navegador / Smart TV
  |
  v
web.izplay.tv  (208.122.17.45)
  | nginx + TLS + estáticos em /var/www/izplay-web
  |
  +-- /gateway/, /player_api.php, /get.php,
  |   /live/, /movie/, /series/, /asset  --->  GATEWAY 209.50.254.197
  |                                             catálogo :3001 com cache
  |
  +-- /video-gateway/, /hls/, /epg/      --->  GATEWAY 209.50.254.197
  |                                             mídia :4100 proxy/transcode
  |
  +-- /control/                          --->  PAINEL ADMIN 38.46.142.234
                                                admin.izplay.tv / telemetria

SUPER PEER BRASIL 209.14.85.55
  SwarmCloud Server as Peer :8080, PM2 super-peer, status Running

PROXY 130.250.189.142
  proxy legado e rollback operacional

VPN FUTURA 190.102.43.177
  WireGuard planejado, rede 10.66.1.0/24, ainda não integrado aos serviços
```

## Servidores e papéis

### 1. Web Player — `208.122.17.45` (`web.izplay.tv`)

- Porta pública do Web Player.
- Serve `/var/www/izplay-web`.
- Repassa catálogo/mídia para o Gateway.
- Repassa `/control/` para o Painel Admin.

### 2. Gateway — `209.50.254.197`

- Catálogo em `127.0.0.1:3001`, processo `iz-catalog-gateway`.
- Mídia em `127.0.0.1:4100`, processo `iz-gateway`.
- Fala com o provedor Xtream e entrega proxy/transcode/HLS para o Web Player.
- Mantém cache `serve-stale` para reduzir bloqueios/rate-limit do provedor.

### 3. Proxy — `130.250.189.142`

- Mantido como proxy legado e opção de rollback.
- Não deve ser o caminho principal do catálogo do Web Player.

### 4. Painel Admin — `38.46.142.234` (`admin.izplay.tv`)

- Nginx `:80` para Node `izplay-panel :3000`.
- PM2: `izplay-panel`.
- Configuração, telemetria, dispositivos e rankings ficam em `panel/data/*.json`.
- Segredo do painel fica em `/etc/izplay/panel.env`.

### 5. Super Peer Brasil — `209.14.85.55`

- SwarmCloud **Server as Peer** oficial.
- URL pública registrada no dashboard SwarmCloud: `http://209.14.85.55:8080`.
- Processo PM2: `super-peer`.
- Endpoint de métricas: `http://209.14.85.55:8080/stats`.
- Status atual: `Running`, integrado ao Painel Admin na aba **P2P / SwarmCloud**.
- **Não reinstalar por cima** enquanto estiver saudável; isso pode criar processo concorrente ou derrubar o nó já registrado.

### 6. VPN futura — `190.102.43.177`

- Próxima etapa isolada.
- WireGuard planejado em `wg0`.
- Porta UDP prevista: `51820`.
- Rede prevista: `10.66.1.0/24`.
- Nesta fase, **não** rotear Gateway, Painel ou mídia pela VPN.

## Fluxo de canais ao vivo

O Web Player usa duas rotas diferentes e elas **não podem ser misturadas**:

- **Catálogo/listagem:** passa por `/gateway`, que consulta o provedor e entrega listas de canais,
  filmes, séries, EPG e metadados.
- **Mídia/transcode:** passa por `/video-gateway/transcode?url=...`, mas o parâmetro `url` deve ser
  sempre a **URL direta do provedor**, por exemplo `http://cxst.shop/live/USER/PASS/STREAM_ID.m3u8`.

Regra crítica: o `iz-gateway` **não** deve receber `https://web.izplay.tv/gateway/live/...` para
transcode. Essa URL é uma rota proxificada de catálogo/borda; quando o FFmpeg tenta ler playlist ou
segmentos por ela, o Gateway pode receber `403 Forbidden` e o Web Player fica com tela preta nos
canais.

O fluxo correto em produção é:

```text
Web Player
  -> https://web.izplay.tv/video-gateway/transcode?url=http://cxst.shop/live/USER/PASS/STREAM_ID.m3u8
  -> Nginx Web /video-gateway/
  -> iz-gateway em 209.50.254.197
  -> provedor direto cxst.shop
```

Sintoma conhecido: Desktop toca canais, mas Web fica preto. Como o Desktop acessa o Gateway/origem
por caminho diferente, isso costuma indicar problema no caminho Web `video-gateway/transcode` ou na
URL enviada para o transcode. O log definitivo fica no Gateway:

```bash
pm2 logs iz-gateway --lines 60 --nostream
# ERRADO: transcode:start url=https://web.izplay.tv/gateway/live/...
# CERTO:  transcode:start url=http://cxst.shop/live/...
```

## P2P / Swarm Cloud

O SDK `@swarmcloud/hls` (2.18.0, build com WebRTC) é servido localmente em `/player/vendor/`.

**Ativação (cliente web/desktop):**
- **Desligado por padrão no Web Player** (`SWARM_P2P_DEFAULT=false`) para garantir que os canais
  abram por HTTP puro mesmo quando o domínio ainda não estiver autorizado no SwarmCloud.
- Pode ser ligado sob demanda com `?p2p=1` ou `settings.p2pEnabled=true`. Pode ser forçado desligado
  com `?p2p=0`.
- `appId = web.izplay.tv` — o domínio precisa estar **autorizado no console do Swarm Cloud**.
- `token = DkwaY5bvR`, `trackerZone = us` (fixados no cliente, não são segredos; o painel pode
  sobrescrever). Exige WebRTC + build P2P do SDK (ambos presentes).

**channelId padronizado** — `live-<id>`, `vod-<id>`, `series-<epId>`, **igual no cliente e no
Super Node**, para caírem no MESMO swarm. Fallback: derivado da URL.

**Monitor admin** — fica **oculto por padrão**, inclusive para conta admin, para não aparecer no Web Player/desktop do cliente. Abre somente sob demanda com `?swarmdebug=admin` ou **Ctrl+Alt+D** na máquina do operador. Mostra peers, canal, peer id, tráfego P2P/HTTP e economia. A telemetria P2P segue sendo enviada ao Painel Admin mesmo com a caixa visual fechada.

Sem Swarm, o canal ainda abre via HTTP normal. **P2P só existe em HLS segmentado (`.m3u8`)**, não
em `.ts` puro. Swarm melhora distribuição e estabilidade, mas não corrige URL HLS quebrada.

### Super Node / Super Peer

Peer **permanente** que ajuda a formar a malha P2P nos canais mais assistidos. Não transcodifica e não é CDN.
- **Produção atual:** **Server as Peer oficial do SwarmCloud** em `209.14.85.55:8080`, processo PM2 `super-peer`, registrado no dashboard como **SUPER PEER BRASIL**.
- **Regra operacional:** não rodar novamente o instalador do Super Peer nessa VPS sem necessidade. Para atualizar/reinstalar, fazer backup e planejar janela, pois é um componente já ativo.
- **Alternativa / referência:** `supernode/` (Node + Chromium headless via Puppeteer), com
  endpoint `/status` de métricas. Ver `supernode/README.md`.
- Restrição comum às duas: cada canal semeado consome **1 conexão do provedor** — respeitar o
  `max_connections` da conta usada.

## Home, perfis e telemetria

O Web Player salva histórico e progresso por perfil para:

- continuar assistindo;
- recomendar por gosto;
- separar favoritos por perfil;
- alimentar rankings do Painel Admin;
- entender aparelho, rede, travamentos e rota de reprodução.

## Rota protegida / VPN

VPN WireGuard será instalada em etapa isolada na VPS 190.102.43.177, rede 10.66.1.0/24.
Nesta passagem ela é somente VPN de usuário final/privacidade. **Não rotear serviços, Gateway,
Painel ou mídia pela VPN** até existir um plano específico de rotas e rollback.

## Segredos

- Não salvar senhas root no repositório.
- `PANEL_PASS` fica em `/etc/izplay/panel.env`.
- Credenciais compartilhadas em texto puro devem ser consideradas comprometidas e rotacionadas.
- Preferir chave SSH a login root por senha.

## Histórico

- **2026-06-30** — Catálogo migrado do Proxy para o Gateway com cache `serve-stale`.
- **2026-07-01** — Painel Admin migrado para `38.46.142.234` (`admin.izplay.tv`).
- **2026-07-02** — Web/desktop: novo layout (login dividido, splash, redesign de canais,
  favoritos de séries + "Minha lista", auto-hide em tela cheia, loading IZ, wifi dinâmico +
  painel de teste de conexão, banner de travamento). P2P Swarm Cloud integrado com `channelId`
  padronizado (`live-`/`vod-`/`series-`), monitor admin oculto por padrão e recomendação do Super
  Peer oficial.

- **2026-07-02** — `209.14.85.55` foi formatada/reaproveitada como **Super Peer Brasil** (`http://209.14.85.55:8080`) usando SwarmCloud Server as Peer oficial. Painel Admin atualizado para mostrar clientes online, telemetria P2P e status do Super Peer. `/control/api` do Web Player oficial reapontado para o Painel Admin novo. VPN movida para etapa futura em `190.102.43.177`.

- **2026-07-03** — Corrigido bug de tela preta nos canais do Web Player: o transcode estava recebendo URL proxificada `https://web.izplay.tv/gateway/live/...`, gerando `403 Forbidden` no `iz-gateway`. Regra documentada: catálogo usa `/gateway`, mas mídia/transcode usa URL direta do provedor (`http://cxst.shop/live/...`).
