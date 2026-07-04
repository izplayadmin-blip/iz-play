# IZ Play — Super Node P2P

Peer **permanente** da malha SwarmCloud. Mantém os **canais mais assistidos** sempre
conectados e redistribui seus segmentos para **acelerar a abertura dos canais**, reduzir
a carga no servidor de origem e **acelerar a formação da malha P2P**.

> **Não** transcodifica e **não** é um CDN. A única função é ser um peer estável do swarm.

## ⭐ Decisão de arquitetura: usar o "Super Peer" oficial do SwarmCloud (recomendado)

Antes de fechar na abordagem Chromium/Puppeteer, foi verificado se o SwarmCloud oferece um
peer **nativo em Node.js, sem navegador**. **Oferece** — é o produto oficial
**"Server as Peer" / "Super Peer"** ([doc](https://www.swarmcloud.net/guides/super-peer)):

- **Node.js** (v18.15.0+), arquitetura **master/worker**, **sem navegador**.
- Instalação: `wget -qN https://cdn.swarmcloud.net/super-peer.sh && bash super-peer.sh --port 8080`
  (ou baixar `super-peer.zip` + `pm2 start index.js -n super-peer`).
- Config via `.env`: `listenPort`, `accessToken`, `signal1`/`signal2` (endereços de sinalização
  vindos do dashboard).
- **Semeadura por canais quentes é automática** (o tracker escolhe pelos rankings de audiência),
  ou manual pela **API REST `/seed`**. → cobre o "Top-N" sem precisar do nosso loop.
- Suporta **live e VOD** (cobre filmes/séries no futuro).
- Recursos **por worker**: ~200 espectadores, ~1 core, **~400 MB RAM**, ~200 Mbps.
- Tem **API REST** de stats e controle de workers.

**Recomendação:** adotar o Super Peer oficial em vez do Chromium/Puppeteer — é oficial, mais
leve (400 MB/worker vs. uma aba Chromium por canal), já faz auto-seed por popularidade **e** VOD,
e traz API de monitoramento. Registro: dashboard → **P2P Setting → Server as Peer → +Super Peer**
(informar `http://ip:porta` e a banda).

> **O que NÃO muda com o Super Peer:** o limite de **conexões simultâneas do provedor**
> (`max_connections`) continua valendo — cada canal semeado puxa 1 stream de origem. E a
> sinalização/registro dependem do seu dashboard SwarmCloud.

### E este `supernode/` (Puppeteer)?
Fica como **implementação de referência / fallback** caso o Super Peer oficial não sirva (ex.:
sem acesso ao dashboard, ou querer controle 100% próprio do Top-N). Se adotarmos o oficial, este
serviço pode virar um **orquestrador/monitor fino** na frente dele: empurra o nosso Top-N do
painel para a API `/seed` e expõe o `/status` unificado (abaixo).

## Endpoint de status (monitoramento)

O serviço sobe um HTTP em `SUPERNODE_STATUS_PORT` (padrão `9099`):

- `GET /status` — JSON com: **canais ativos**, **peers**, **upload**, **download** (por canal e
  totais) e **CPU/RAM** (processo + host). Exemplo:

```json
{
  "service": "izplay-supernode",
  "activeChannels": 3,
  "totals": { "peers": 41, "uploadKB": 128400, "downloadKB": 90210 },
  "channels": [
    { "channel_id": "152", "name": "Globo HD", "peers": 22, "p2pUpKB": 80100, "p2pDownKB": 12000, "httpDownKB": 30000, "uptimeSec": 640 }
  ],
  "system": { "cpuPercentProc": 18, "procRssMB": 220, "hostTotalMB": 4096, "hostFreeMB": 900, "loadavg": [1.2, 1.0, 0.8] }
}
```

- `GET /healthz` — `{"ok":true}` para health check.

> Nota: `cpuPercentProc`/`procRssMB` são do processo Node. As abas do Chromium são processos
> filhos e não entram nessa conta — use `hostFreeMB`/`loadavg` para o total do host. Se
> migrarmos para o Super Peer oficial, o `/status` passa a agregar a **API de stats dele** +
> métricas do host (mesma estrutura).

## Arquitetura

```
Painel Admin → Gateway → Super Node → SwarmCloud → Clientes IZ Play
```

- Serviço **independente** (processo/container próprio), roda na mesma VPS do Gateway na
  1ª fase, mas pode ir para outra VPS/região depois sem mexer no resto.
- A cada `POLL_MS` consulta `SUPERNODE_TOP_CHANNELS_URL` para saber os canais mais vistos.
- Mantém **1 aba de Chromium headless por canal do Top-N** (via Puppeteer). Cada aba roda
  `hls.js` + SDK SwarmCloud como um **peer real** (mesmo `appId`/`token`/zona e mesmo
  `channelId` dos clientes), semeando os segmentos.
- Canal fora do Top-N por mais de `DROP_GRACE_MS` (padrão 5 min) tem a aba fechada.

## ⚠️ Pré-requisitos (leia antes de rodar)

1. **Conexões simultâneas do provedor.** Segurar N canais = N conexões simultâneas no
   provedor. A conta atual do `cxst.shop` tem `max_connections = 2` — **insuficiente**.
   Use uma **conta dedicada** que permita `≥ SUPERNODE_TOP_N` conexões (ou várias contas).
   O `SUPERNODE_MAX_CONCURRENT` é um teto de segurança para não estourar esse limite.
2. **HLS, não `.ts` puro.** O P2P do SwarmCloud compartilha **segmentos** — use a variante
   **`.m3u8`** (`SUPERNODE_STREAM_EXT=m3u8`). Canal só-`.ts` não gera P2P.
3. **Recursos.** Cada aba de Chromium reproduzindo live pesa. Em VPS de **4GB**, comece com
   `SUPERNODE_TOP_N=3–5` e `shm_size≥1gb`. Escale conforme a RAM.
4. **`channelId` precisa bater com o dos clientes** (ver seção abaixo).

## Configuração

Copie `.env.example` para `.env` e ajuste. Principais variáveis:

| Var | Padrão | O que é |
|---|---|---|
| `SUPERNODE_TOP_N` | 10 | quantos canais manter ativos |
| `SUPERNODE_POLL_MS` | 60000 | intervalo de consulta ao top-channels |
| `SUPERNODE_DROP_GRACE_MS` | 300000 | tempo fora do Top-N antes de liberar |
| `SUPERNODE_MAX_CONCURRENT` | 10 | teto de conexões simultâneas |
| `SUPERNODE_TOP_CHANNELS_URL` | — | fonte dos canais mais vistos |
| `XTREAM_HOST/USER/PASS` | — | conta dedicada p/ abrir os streams |
| `SWARM_APP_ID/TOKEN/TRACKER_ZONE` | web.izplay.tv / DkwaY5bvR / us | iguais aos clientes |

## Rodar

**Docker (recomendado — serviço isolado):**
```bash
cd supernode
cp .env.example .env   # edite XTREAM_USER/PASS e a conta dedicada
docker build -t izplay/supernode .
docker run --rm --env-file .env --shm-size=1g izplay/supernode
# ou via docker-compose.example.yml
```

**Local (dev):**
```bash
cd supernode && npm install && cp .env.example .env
node index.js
```

## Fonte do Top-N (`top-channels`)

O spec usa `GET /gateway/top-channels` devolvendo `[{channel_id, name, viewers}]`. O serviço
**também aceita** o formato do painel (`/api/client/home` → `{live:[{contentId,contentName,views}]}`),
então dá para apontar direto ao painel enquanto o endpoint do gateway não existe.

Endpoint sugerido no **catalog-gateway** (deriva do painel; a implementar/confirmar):

```js
// GET /top-channels  ->  [{channel_id, name, viewers}]
// deriva do ranking de audiencia do painel (/api/client/home -> live)
if (parsed.pathname === '/top-channels') {
  const r = await fetch(`${PANEL_URL}/api/client/home`)
  const home = await r.json()
  const list = (home.live || []).map(x => ({
    channel_id: x.contentId, name: x.contentName, viewers: x.views || 0
  }))
  return send(req, res, 200, list)
}
```

> Nota: hoje o painel agrega **views acumuladas**, não espectadores simultâneos. Para
> "viewers em tempo real" dá para evoluir usando os pings de telemetria (`/api/telemetry/ping`,
> campo `watching`). Fica como melhoria da fonte de dados — não muda o Super Node.

## Nota sobre `channelId` (importante)

Para o Super Node **parear com os clientes**, os dois lados precisam usar o **mesmo
`channelId`** no SwarmCloud. O Super Node usa `channelId = channel_id` (fixo).
No cliente, o `channelId` é derivado da URL por `swarmChannelId()`. Se as URLs divergirem
(ex.: cliente tocando via `/video-gateway/transcode`), os `channelId` podem não bater e o
pareamento não acontece.

**Recomendação:** padronizar o `channelId = channel_id` **nos dois lados** (uma pequena
mudança no cliente para setar o `channelId` explícito com o id do canal). Assim o super node
e os clientes ficam garantidamente no mesmo swarm. (Fica como passo de integração.)

## Escalabilidade

- `SUPERNODE_TOP_N` controla o alcance: 10 → 50 → 100.
- Vários Super Nodes: suba múltiplas instâncias (regiões diferentes), cada uma com sua conta
  e seu teto de conexões.
- Filmes/séries mais assistidos: mesma mecânica, trocando a fonte do top e o padrão de URL
  (`/movie/…`, `/series/…`) — o `streamUrl()` já é o único ponto a ajustar.
