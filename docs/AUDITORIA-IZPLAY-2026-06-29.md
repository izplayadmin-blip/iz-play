# Auditoria e Avaliação — IZ Play v2.1

**Data:** 2026-06-29
**Escopo:** revisão de código (segurança + qualidade), avaliação de layout/UX, estratégia
multiplataforma (proxy + P2P), desenho do sistema de atualização in-app e **auditoria de
produção das VPS**.
**Natureza da auditoria de servidores:** somente leitura — nenhuma alteração foi feita.

> ⚠️ **Aviso de segredos:** as senhas root das VPS foram compartilhadas em texto puro
> durante a sessão. Considere-as comprometidas e **troque todas**, migrando para
> autenticação por chave SSH. Este relatório **não** contém senhas.

---

## 1. Visão geral do projeto

Player IPTV multiplataforma, organizado em três blocos:

- **Clientes:** `app` (desktop Electron + MPV/FFmpeg), `web-player` (PWA HLS + P2P),
  `android` (Android TV/TV Box em Kotlin/Compose).
- **Backend:** `gateway` (proxy/probe/transcode de mídia), `panel` (admin + telemetria).
- **Infra:** `deploy` (Nginx/systemd/WireGuard), `docs`, `releases`, `scripts`.

A inteligência de rede (acesso, UA, codec, fallback) está corretamente concentrada no
**gateway**, que é a verdadeira camada "multiplataforma" do produto.

---

## 2. Revisão de código — segurança

### 2.1 Pontos fortes

- **Gateway (`gateway/server.js`, repo):** o melhor componente.
  - Proteção SSRF sólida: bloqueia IPs privados/loopback/link-local (IPv4 e IPv6),
    resolve e valida DNS antes de conectar, revalida em redirects, allowlist de host e de
    protocolo, rejeita `user:pass@` na URL.
  - Token *timing-safe*, rate limiting, escuta apenas em `127.0.0.1`.
  - Proteção contra path traversal em `/hls/` (regex estrita).
  - `ffmpeg`/`ffprobe` com `shell:false` e URL já validada — sem injeção de comando.
  - Limite de transcodes simultâneos e limpeza de sessões por TTL.
- **Painel (`panel/server.js`, repo):** login com hash + `timingSafeEqual` e bloqueio após
  5 tentativas; sanitização recursiva da telemetria com escape de HTML; headers de
  segurança (nosniff, X-Frame-Options, Referrer-Policy).
- **Web-player:** uso consistente de `esc()` (28 pontos) ao renderizar dados remotos.
- **Geral:** segredos fora do Git (`.gitignore` correto), token do gateway injetado pelo
  Nginx (nunca exposto ao JS), HSTS/TLS previstos no Nginx do repo.

### 2.2 Problemas — críticos

1. **App Desktop Electron com segurança desativada** (`app/main.js`):
   `nodeIntegration: true`, `contextIsolation: false`, `webSecurity: false`,
   `disable-web-security`, `ignore-certificate-errors`, `allowRunningInsecureContent`, e
   remoção de CSP/X-Frame de todas as respostas. Qualquer XSS no renderer vira execução
   de código no SO.
2. **Credenciais hardcoded no desktop:** proxy local `admin/iptv1234` e gateway fixo
   `http://209.50.254.197` (HTTP puro) — credenciais Xtream em claro no fallback.

### 2.3 Problemas — médios

3. **Endpoints de telemetria sem autenticação** (`/api/telemetry/report|ping|p2p`,
   `/api/client/commands`): permitem envenenar o ranking "HOT" e enumerar/drenar comandos
   de outro usuário (`?user=X`).
4. **Android — credenciais desprotegidas** (`AndroidManifest.xml`, `SettingsStore.kt`):
   `usesCleartextTraffic="true"`, `allowBackup="true"` e usuário/senha Xtream em texto puro
   no DataStore (deveria ser `EncryptedSharedPreferences`/Tink).
5. **Web-player — credenciais em `localStorage`** (texto puro); CSP do Nginx com
   `script-src 'unsafe-inline'`; PIN parental validado só no cliente.

### 2.4 Qualidade / manutenção

- Sem testes automatizados (só `scripts/check.js`, que valida estrutura) e sem CI/`npm audit`.
- `web-player/index.html` (~3.787 linhas) e `app/index.html` (~3.048 linhas) monolíticos e
  **duplicados** entre si.
- Binários FFmpeg versionados em `app/ffmpeg-master-latest-win64-gpl-shared` (peso morto —
  o desktop usa `@ffmpeg-installer` via npm).

---

## 3. Avaliação de layout / UX

### 3.1 Pontos fortes
- Identidade visual forte e consistente: vermelho (`#cc0000` / `#D81F26`) sobre quase-preto,
  logo "IZ PLAY", tipografia pesada. Reconhecível nas três plataformas.
- Web e desktop compartilham a mesma linguagem (sidebar colapsável no hover, *live view*
  56% player / 44% lista+EPG, overlay de categorias) — visual tipo Netflix, profissional.
- Android usa o caminho certo: Compose + Material3, ciente de TV (Leanback, landscape,
  banner), sidebar fixa de 76dp adequada a controle remoto.

### 3.2 A melhorar
- **UI duplicada, não compartilhada:** web e desktop são dois arquivos gigantes copiados;
  mudança visual precisa ser feita duas vezes.
- **Padrões de navegação divergentes:** a sidebar hover-expand (web/desktop) é de mouse e
  não funciona com D-pad/controle remoto; o Android resolve com sidebar fixa, mas a lógica
  de UI não é compartilhada.
- **Tokens de cor redefinidos em cada stack** (CSS vars + `Theme.kt`).
- Fontes pequenas no Android (labels de 8sp) e contraste baixo (`#888` sobre preto) para
  visualização a distância (10-foot UI).

---

## 4. Estratégia multiplataforma (proxy + P2P)

**Tensão central:** P2P (SwarmCloud/WebRTC) é fácil no navegador e razoável no Android
nativo, mas difícil em WebView de TV box; já o playback confiável em TV box barato é fácil
nativo (ExoPlayer/mpv) — justamente onde o WebRTC é mais difícil. Um único cliente que seja
ótimo nos dois é o problema caro.

**Recomendação pragmática:**
1. **Unificar web + desktop** agora (Electron carregando o mesmo código do web-player, ou
   migrar para **Tauri**) — ganho de manutenção fácil, mata a pior duplicação.
2. **Manter Android nativo** (ExoPlayer/Media3) e adicionar P2P pelo **SDK Android nativo**
   do SwarmCloud, sem forçar WebRTC em WebView.
3. **Gateway como backend único** (já é).
4. **Arquivo único de design tokens** gerado para CSS vars *e* `Theme.kt`.
5. **Validar P2P pela própria telemetria** (`p2pDown/total`): só compensa em ao-vivo com
   muitos espectadores simultâneos; para VOD/cauda longa o ganho é baixo.

---

## 5. Sistema de atualização in-app

A fundação existe: o painel já publica `appVersion` + `updateUrl`, mas **nenhum cliente
consome** ainda.

- **Fonte da verdade:** evoluir `/api/client/config` para versão **por plataforma**
  (`desktop`/`android`) com `version`, `url`, `mandatory`, `notes`; hospedar artefatos no
  Nginx (ex.: `/downloads/`).
- **Desktop:** `electron-updater` — checa o feed ao abrir, baixa em segundo plano, mostra
  banner *"reiniciar agora?"* e `quitAndInstall()` (silencioso). Requer empacotar com
  `electron-builder` e, idealmente, assinar o executável.
- **Android (APK sideload):** comparar `BuildConfig.VERSION_CODE`, baixar o APK in-app com
  barra de progresso, abrir o instalador via `FileProvider` + `Intent`. A **instalação final
  exige uma confirmação do sistema** (modelo de segurança do Android) — silêncio total só
  como *Device Owner* em frota gerenciada.
- **Web:** PWA já carrega sempre a última versão; só falta o service worker avisar
  "recarregar para atualizar".

---

## 6. Auditoria de produção (VPS)

Topologia real **difere** do `deploy/nginx-izplay-v2.1.conf` (que assume host único).
O gateway está espalhado em duas VPS e os serviços Node estão **defasados do repositório**.

### 6.1 Painel Admin — `209.14.85.55` (Ubuntu 22.04)
| Item | Esperado (repo) | Em produção |
|---|---|---|
| Local | — | `/var/www/izplay/panel/` ✅ |
| Versão | 2.1.0 (580 linhas) | **1.1.0 (435 linhas)** ❌ |
| Dados | 5 JSON | todos presentes ✅ |
| Processo | systemd | pm2 `izplay-panel` (19 restarts) ⚠️ |
| Segredos | `/etc/izplay/panel.env` | **não existe**; env inline; senha admin fraca ❌ |
| TLS/Nginx | 443 + HSTS + rate-limit | **só porta 80**, `server_name _`, sem TLS ❌ |

### 6.2 Gateway — `209.50.254.197` (Ubuntu 22.04)
| Item | Esperado | Em produção |
|---|---|---|
| Local | — | `/opt/iz-gateway/server.js` (127.0.0.1:4100) ✅ |
| Versão | 2.1.0 (409 linhas, SSRF+token) | **295 linhas** ❌ |
| SSRF | bloqueio de IP privado + DNS + allowlist | **ausente** (`getTargetUrl` só checa protocolo) ❌ |
| Auth | token obrigatório | **`GATEWAY_REQUIRE_TOKEN=0`** → autoriza tudo ❌ |
| Nginx | injeta token + TLS | porta 80 default → 4100, **sem token, sem TLS** ❌ |
| WireGuard | `wg0` | **não existe** (rota protegida não funcional) ❌ |
| ffmpeg/ffprobe | instalados | ✅ |
| Extra | — | serviço `/epg/` e Docker em `:5000` (não documentado) |

### 6.3 Proxy — `130.250.189.142` (Ubuntu 22.04)
| Item | Em produção |
|---|---|
| `iptv-proxy` | `0.0.0.0:8080` **público**, systemd, **sem flags de usuário/senha** ⚠️ |
| **2º gateway** | `/opt/izplay/gateway/server.js` em **`0.0.0.0:3001` público**, 291 linhas, **0 SSRF**, sem token ❌ |
| TLS | nenhum (sem Nginx) ❌ |

### 6.4 Web Player — `208.122.17.45` = **`izplay.tv` oficial** (Ubuntu 22.04)
| Item | Esperado (repo) | Em produção |
|---|---|---|
| Domínio/TLS | izplay.tv com 443 | **TLS via Certbot** ✅ (80 → 443 redirect) — único host com HTTPS |
| Local | `/var/www/izplay-player/` | **`/var/www/izplay-web/`** ⚠️ (caminho diferente do documentado) |
| `index.html` | 3.787 linhas | **3.896 linhas, editado hoje** ⚠️ (produção divergiu do repo) |
| Higiene | — | vários `index.html.bak-*` e **dois service workers** (`service-worker.js` + `sw-v23.js`) ⚠️ edição direta no servidor |
| Assets | manifest, icons, vendor SDK | ✅ `vendor/swarmcloud-hls.min.js`, icons, logo presentes |
| CSP | CSP forte para `/player/` | **ausente** ❌ (só X-Frame-Options + nosniff; Referrer-Policy mais fraca) |
| Backend | token injetado, TLS interno | proxia catálogo/stream para **`http://130.250.189.142:3001` em HTTP puro** ❌ |

### 6.5 Topologia real descoberta
- **Browser → `izplay.tv` (208.122.17.45):** HTTPS ✅.
- **`izplay.tv` → Proxy VPS (130.250.189.142:3001 e :80):** **HTTP puro** entre datacenters,
  carregando credenciais Xtream nas URLs (`player_api.php?username=...&password=...`,
  `/live/USER/PASS/...`). ❌
- O alvo `:3001` é o **gateway aberto** (sem token, sem SSRF), acessível tanto via
  `izplay.tv` quanto diretamente em `0.0.0.0:3001`.
- Difere do `deploy/nginx-izplay-v2.1.conf` (single-host): backend de mídia está na VPS de
  Proxy, não em `127.0.0.1`.

### 6.6 Conclusões críticas da auditoria
1. **Proxy aberto / SSRF em dois endpoints públicos** (`209.50.254.197:80` e
   `130.250.189.142:3001`): aceitam qualquer URL, sem token e sem bloqueio de IP privado →
   SSRF (metadados de nuvem, `localhost:5000`, rede interna), relay aberto (abuso de banda e
   exposição legal). As proteções do repo **nunca subiram** para produção.
2. **Credenciais Xtream em HTTP puro** — tanto no hop `izplay.tv → Proxy VPS` quanto no
   login do painel (sem TLS). Só o front-end `izplay.tv` tem HTTPS.
3. **Produção divergente do repositório nos dois sentidos** — painel e gateways estão
   **atrás** do repo (versões 1.1.0 / ~291-295 linhas); o web player está **à frente** e foi
   editado direto em produção (`.bak-*`, modificado hoje). A regra "melhoria primeiro na
   pasta `iz-play-v2.1`" não está sendo seguida em nenhum dos casos.
4. **CSP ausente** no web player público (estava prevista no repo).

---

## 7. Roadmap priorizado

### 🔴 Curto prazo (dias / 1–2 semanas)
1. **Fechar os gateways abertos:** ativar token e bind em `127.0.0.1` atrás do Nginx, ou
   subir a versão 2.1.0 do repo (já com SSRF + token).
2. **Blindar o Electron:** `contextIsolation: true`, `nodeIntegration: false`,
   `webSecurity: true` + `preload`/`contextBridge`.
3. **Remover credenciais hardcoded** do desktop (proxy e IP do gateway) → env + HTTPS.
4. **Android:** `allowBackup="false"`, restringir cleartext via `network_security_config`.
5. **Autenticar a telemetria** do painel.
6. `npm audit` + atualizar dependências.
7. **Trocar as senhas root** das VPS e migrar para chave SSH.

### 🟠 Médio prazo (1–2 meses)
8. **Padronizar deploy a partir do repo (2.1.0)** nos 3 serviços Node.
9. **TLS (Let's Encrypt) + config Nginx do repo** em todos os hosts.
10. **Mover segredos para `/etc/izplay/*.env`**; senha de painel forte.
11. **Criptografar credenciais no Android** (`EncryptedSharedPreferences`).
12. **Unificar web + desktop** (Tauri/Electron carregando o web-player).
13. **Design tokens únicos**; testes mínimos + CI.

### 🟡 Longo prazo (3+ meses)
14. Decidir a estratégia multiplataforma com base na telemetria de P2P.
15. Modularizar o web-player; adequar UI para 10-foot/TV.
16. Migrar persistência do painel (JSON → SQLite) conforme o volume crescer.
17. Configurar `wg0` se a rota protegida for entrar em produção.
18. Implantar o sistema de atualização in-app (seção 5).

---

## 8. Pendências
- Auditoria dos **4 servidores concluída** (Painel, Gateway, Proxy, Web Player).
- Decisão sobre como aplicar as correções críticas (relatório de comandos vs. execução).
- Versionar em Git as alterações feitas direto no web player de produção (`/var/www/izplay-web`)
  e remover os `index.html.bak-*` / service worker duplicado.
