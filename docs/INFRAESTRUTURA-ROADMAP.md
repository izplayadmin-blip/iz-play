# IZ Play — Roadmap de Consolidação da Infraestrutura

> Fase de **estabilização, integração e observabilidade** antes de novas funcionalidades.
> Regra de ouro (definida pelo produto): **seguir a ordem de prioridade e não iniciar um
> estágio antes do anterior estar funcionando e validado.** Nada de dados simulados — toda
> informação vem da infraestrutura real e é validada antes de exibir.

## Ordem de prioridade (obrigatória)

1. **Infraestrutura** → 2. **Comunicação entre serviços** → 3. **Auditoria** →
4. **Monitoramento** → 5. **Funcionalidades** → 6. **Interface** → 7. **Otimizações**

## Mapa: as 14 seções do brief nos 7 estágios

| Estágio | Seções do brief | Entrega |
|---|---|---|
| 1. Infraestrutura | §5 (VPN), §14 (Super Peer oficial) | Serviços de base no ar |
| 2. Comunicação (**keystone**) | §4, §6 | Padrão único: cada serviço expõe métricas reais + manda heartbeat/logs ao Painel |
| 3. Auditoria | §3 | Rotina que valida sessões/logins e sincronização entre serviços; relatório de inconsistências |
| 4. Monitoramento | §1 (Super Desktop admin), §2 (área Infraestrutura no Painel), §11 (observabilidade/histórico), §13 (central de logs) | Consumidores da telemetria do estágio 2 |
| 5. Funcionalidades | §7 (últimos adicionados), §8 (recomendações + IMDb), §9 (Top 100) | Nos apps + Painel |
| 6. Interface | §12 (loading por etapas reais) | No app |
| 7. Otimizações | §10 (gerar prompt da próxima IA p/ VPN) | Fechamento |

## O keystone: camada de telemetria (estágio 2 destrava quase tudo)

Repare que **§1, §2, §3, §11, §13** são todos **consumidores** de uma mesma coisa: métricas e
logs **reais** de cada serviço. Logo, o passo que destrava o resto é padronizar **como cada
serviço reporta seu estado**. Sem isso, o resto viraria "dado simulado" — o que o brief proíbe.

**Arquitetura da telemetria (proposta):**

```
Cada host/serviço  →  IZ Infra Agent (coleta real)  →  POST /api/infra/*  →  Painel Admin (armazena)
   Gateway, Proxy, VPN, Super Peer, apps                                        │
                                                                                ├─► UI "Infraestrutura" (Painel)
                                                                                └─► Super Desktop Player (admin)
```

- **IZ Infra Agent** — processo leve (Node) em cada VPS que coleta o que é **real** do host:
  - comum: CPU, RAM, uptime, latência, versão, último heartbeat (via `os`/`process`/`/proc`);
  - por serviço: Gateway (nº requisições, última sync), Proxy (banda, conexões, cache — `iptv-proxy`),
    VPN (usuários conectados, tráfego — `wg show`), Super Peer (workers, peers, up/down, canais — API do SwarmCloud).
- **Endpoints no Painel** (a criar, todos autenticados): `POST /api/infra/heartbeat`,
  `POST /api/infra/metrics`, `POST /api/infra/logs`; leitura: `GET /api/infra/status`,
  `GET /api/infra/logs`. **Registrar cada API criada** (ver `docs/APIS.md`, a criar).
- **Padrão único de comunicação**: mesmo formato de payload (serviço, ts, tipo, dados) para todos
  → o Painel reflete exatamente o estado real.

> Serviços que não são Node (Proxy em Go, WireGuard) recebem um **agente sidecar** (Node/shell)
> que lê os comandos nativos (`wg show`, `systemctl`, `/proc`) e reporta no mesmo padrão.

## Restrição importante (como isto será construído e validado)

- **Eu escrevo o código** (agentes, endpoints do Painel, UI de Infraestrutura, Super Desktop admin,
  loading real, últimos adicionados, ranking, central de logs) **no repositório**.
- **Eu não faço deploy/SSH nas VPS** (política de credenciais) e **não invento métricas**. Então a
  **validação de cada estágio** depende de: (a) subir os agentes/serviços nas VPS (você ou a IA de
  deploy), e (b) os dados **reais** chegarem ao Painel. Sem isso, não há o que validar — e o brief
  proíbe simular.
- **Dependências externas** (SwarmCloud): as métricas do Super Peer (workers/peers/up/down/canais)
  vêm da **API do SwarmCloud** — documentar o que a API entrega **antes** de construir a UI que a
  consome (ver §14). Se algo não for exposto pela API, fica documentado como limitação.

## Estágio 1 — Infraestrutura (em andamento)

- **Super Peer oficial (SwarmCloud)** — Node, sem Chromium (§14). Precisa de `signal1`/`signal2`/
  `accessToken` do dashboard. Ver `docs/DEPLOY.md`. Provedor de semeadura: `cxst.shop`
  (conta 547069, 100 conexões).
- **VPN** — instalar/configurar na VPS **`190.102.43.177`** (§5). Integrar ao Gateway, Painel e
  monitoramento; registrar métricas e logs (isso já cai no estágio 2).

### Bug de auditoria já relatado (§3) — para investigar no estágio 3
"Logado no Web Player com usuário X, mas o Painel mostra logins diferentes." Hipótese a confirmar:
divergência entre a **fonte de sessão** (o que o app envia na telemetria/ping) e o que o Painel
agrega (`deviceMap`/`makeDeviceKey`). Investigar o fluxo `sendTelemetryPing` → `POST /api/telemetry/ping`
→ agregação no Painel. Não exibir nada sem validar a origem.

## Princípios (do brief)

- Sem funcionalidades simuladas · tudo da infra real · tudo validado antes de exibir.
- Estabilidade antes de features · evitar código duplicado · documentar toda alteração ·
  registrar todas as APIs criadas · validar cada estágio antes do próximo.

## Registro de progresso

- **2026-07-02** — Roadmap criado. Estágio 1 em andamento (Super Peer + VPN `190.102.43.177`).
