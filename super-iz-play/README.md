# Super IZ Play — Monitor Admin (Desktop)

App **separado** do player do cliente (pasta própria). Serve só para **você administrar/monitorar**
a infraestrutura P2P do IZ Play em tempo real. **Não exibe nada simulado** — tudo vem das APIs
reais do **Painel Admin**.

## O que mostra
- **Super Peer (P2P):** ligado/offline, peers, workers, uplink atual/médio, memória, uptime, restarts, versão.
- **Rede P2P:** sessões atuais, eventos hoje, P2P baixado, HTTP baixado, **economia P2P (%)**.
- **Canais distribuídos:** top canais com tráfego P2P/sessões.
- **Host do Painel:** RAM, disco, uptime/load.
- **Clientes conectados:** quem está online, o que está assistindo, versão e IP.

## De onde vêm os dados (APIs reais do Painel)
| Card | Endpoint |
|---|---|
| Super Peer | `GET /api/p2p/super-peer` |
| Rede P2P + canais | `GET /api/telemetry/p2p` |
| Host | `GET /api/system` |
| Clientes | `GET /api/devices` |
| Login | `POST /api/login` → token em `x-session-token` |

> O Painel busca o Super Peer em `panelConfig.superPeerUrl` (hoje `http://209.14.85.55:8080`) via
> `/stats`. Se o Super Peer estiver `Running`, o card "Super Peer" mostra ligado.

## Rodar
Precisa de Electron. **Sem instalar nada novo**, dá pra reusar o Electron do app cliente:
```powershell
# a partir da raiz do projeto
& ".\app\node_modules\.bin\electron.cmd" ".\super-iz-play"
```
Ou instalar o próprio:
```bash
cd super-iz-play && npm install && npm start
```

Na 1ª abertura, informe: **URL do Painel** (ex.: `http://38.46.142.234`), **usuário** e **senha**
admin, e o intervalo de atualização. Fica salvo só nesta máquina (`localStorage`).

## Segurança
- Ferramenta **interna de admin** — a senha do painel fica salva localmente nesta máquina; não
  distribua este app para clientes.
- Só faz **leitura** (GET) + login. Não altera nada na infraestrutura.
