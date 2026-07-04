# Integração VPN (WireGuard) — Arquitetura

> Status: **planejamento** (2026-07-02). A VPS `209.14.85.55` agora é o **Super Peer Brasil** e não deve receber WireGuard.
> A VPN futura deve ser instalada de forma isolada em `190.102.43.177`, sem rotear Gateway, Painel ou mídia nesta passagem.
> Web player fica de fora do túnel (limitação do navegador — ver §6).

## 1. Dados da VPN futura

| Item | Valor |
|---|---|
| Tecnologia | WireGuard |
| Interface | `wg0` |
| Porta | UDP `51820` |
| Rede VPN | `10.66.1.0/24` |
| IP do servidor | `10.66.1.1` |
| 1º cliente gerado | `10.66.1.2` |
| Config cliente (no servidor) | `/root/izplay-vpn2-client1.conf` |

> ⚠️ O `.conf` do servidor contém **chave privada** — não versionar no Git, não colar em chat.
> O modelo abaixo prevê **um peer por dispositivo**, não compartilhar esse arquivo único.

## 2. Decisão e trade-off (split-tunnel)

**Decisão:** túnel **parcial (split-tunnel)** — só **catálogo/controle** passa pela VPN; o
**vídeo vai direto**. Objetivo declarado: privacidade do usuário.

**O que o split-tunnel realmente entrega:**

- ✅ **Bypass de bloqueios** do catálogo (chamadas `player_api.php`/`get.php` saem pelo IP da VPN).
- ✅ **Proteção do canal de controle** (telemetria/config) e das **credenciais Xtream** em trânsito.
- ✅ Privacidade **parcial**: esconde os metadados de navegação/API.
- ❌ **NÃO** esconde o IP do usuário no **vídeo** (o stream, que é o grosso do tráfego, sai direto).
- ✅ Banda baixa no `10.66.1.1` (o vídeo, que pesa, não passa por lá).

> Se no futuro for preciso **privacidade real também do vídeo**, aí exige **full-tunnel**
> (`AllowedIPs = 0.0.0.0/0`) e uma VPS de egresso com banda para todo o streaming — outra fase.

## 3. Como o split-tunnel é feito no WireGuard

O split é controlado pelo **`AllowedIPs` do cliente**: só os destinos listados entram no túnel;
o resto (vídeo) sai pela rede normal.

```
[Interface]
PrivateKey = <por-dispositivo>
Address    = 10.66.1.X/32
DNS        = 10.66.1.1          # DNS só p/ resolver os hosts de catálogo dentro do túnel

[Peer]
PublicKey  = <chave-do-servidor>
Endpoint   = <IP-publico-VPN>:51820
AllowedIPs = <IPs dos serviços de CATÁLOGO/CONTROLE>   # NÃO 0.0.0.0/0
PersistentKeepalive = 25
```

**Complicação real — Cloudflare.** Hoje o catálogo é acessado via `web.izplay.tv` e o provedor
`cxst.shop`, **ambos atrás de Cloudflare** (IPs rotativos). Fixar `AllowedIPs` por IP fica frágil.
Opções (a decidir na fase de implementação):

1. **Rotear pelo IP fixo do Gateway VPS** (`209.50.254.197`) em vez do domínio Cloudflare: o app
   nativo fala catálogo direto com o Gateway VPS (IP estável) → `AllowedIPs = 209.50.254.197/32`
   (+ Painel Admin `38.46.142.234/32`, se a fase de rotas decidir isso). Simples e estável. **Preferido.**
2. Incluir as faixas do Cloudflare no `AllowedIPs` — funciona, mas joga muito tráfego não
   relacionado pra dentro do túnel.
3. Túnel **por-app** no Android (§5) + `AllowedIPs` amplo, deixando o split pro nível de rota.

## 4. Provisionamento por dispositivo (Painel)

Não distribuir o `.conf` único. Cada dispositivo recebe **seu próprio peer**:

1. App gera um **par de chaves localmente** (a privada nunca sai do device).
2. App envia a **chave pública** + id do dispositivo pro Painel (`/api/vpn/register`).
3. Painel roda no servidor VPN: `wg set wg0 peer <pub> allowed-ips 10.66.1.X/32` e devolve
   `{address, serverPublicKey, endpoint, allowedIps, dns}`.
4. App monta o `[Interface]/[Peer]` com a **sua** chave privada + o que o painel devolveu.
5. Revogação: Painel roda `wg set wg0 peer <pub> remove`.

> `/24` ⇒ ~253 dispositivos. Se passar disso, ampliar a rede (`/23`, `10.66.1.0/23`) ou usar
> múltiplos servidores WG. O Painel já gerencia dispositivos/telemetria — encaixa bem aqui.

## 5. Desktop (Electron)

- **Túnel:** o processo *main* sobe/derruba o WireGuard via `child_process`.
  - **Windows:** WireGuard usa o serviço oficial — `wireguard.exe /installtunnelservice <conf>`
    e `/uninstalltunnelservice <nome>`. **Exige privilégio de administrador** (elevação UAC) —
    este é o ponto mais delicado do desktop. Alternativa userspace: `wireguard-go` + `wg`, mas
    ainda precisa do adaptador TUN/driver.
  - **macOS/Linux** (se houver build): `wg-quick up/down` com o `.conf`.
- **Split-tunnel:** vem pronto no `AllowedIPs` do `.conf` provisionado (§3/§4).
- **UI:** toggle em Configurações → *Privacidade / VPN* (Conectar, status Conectado/Desligado,
  "conectar ao abrir"). Renderer ↔ main por **IPC** (`vpn-connect`, `vpn-disconnect`, `vpn-status`).
- **Empacotamento:** embarcar o binário do WireGuard (ou detectar instalação) + `.conf` temporário
  com permissão restrita; nunca gravar a chave privada em texto acessível.
- **Pendência:** decidir elevação (instalar serviço 1x com admin vs. pedir UAC a cada conexão).

## 6. Android

- **Lib:** `com.wireguard.android:tunnel` (oficial) + `VpnService`.
- **Split-tunnel de dois níveis (recomendado):**
  - **Por-app:** `Builder.addAllowedApplication(<pacote do IZ Play>)` → só o tráfego do app
    entra na VPN (não afeta o resto do celular).
  - **Por-rota:** `AllowedIPs` = IP do Gateway/Painel (catálogo/controle); o player de vídeo
    usa URL direta do provedor, que fica **fora** do túnel.
- **Serviço:** VPN no Android exige **foreground service + notificação** persistente.
- **Provisionamento:** mesmo fluxo do §4 (par de chaves local, registro no Painel).
- **UI:** toggle nas configurações + primeira execução pede permissão de VPN do sistema
  (`VpnService.prepare()`), que **o usuário precisa aprovar** (modelo de segurança do Android).
- **Trabalho:** é código **Kotlin** no projeto `android/` (separado do web-player).

## 7. Web player — por que fica de fora

O navegador **não** sobe túnel WireGuard. No web, "VPN" só existe **server-side** (o Gateway sai
pela VPN), o que **não** esconde o IP do espectador (Cloudflare + a VPS web ainda o veem). Portanto,
para o objetivo de privacidade do usuário, **o web player não participa do túnel**. O que dá pra
fazer no web (fase futura, opcional): o **Gateway** usar a VPN como egresso pra buscar catálogo →
ajuda no bypass de bloqueio, mas é benefício de plataforma, não de privacidade do usuário. Isso
também ativaria a **"rota protegida"** já prevista em `docs/ARQUITETURA.md` (o `wg0` que hoje não
existe no caminho do Gateway).

## 8. VPN × P2P (SwarmCloud)

O P2P usa o SDK `@swarmcloud/hls` (2.18.0) sobre **WebRTC**, trocando segmentos de vídeo direto
entre espectadores (só ativa com `swarmCloud.enabled`, WebRTC disponível e domínio autorizado —
ver `docs/ARQUITETURA.md`).

**Boa notícia — o split-tunnel já é compatível.** O P2P é parte da **camada de vídeo/mídia**, que a
decisão de split-tunnel **já mantém fora do túnel**. Então vídeo HTTP e troca P2P seguem diretos,
juntos, sem conflito com o WireGuard; catálogo/controle continuam protegidos no túnel.

**Atenção — P2P expõe o IP do espectador por natureza.** WebRTC conecta os pares **direto (IP a
IP)** para trocar segmentos. Mesmo com a VPN em split-tunnel, o P2P revela o IP do usuário para
**outros espectadores** — é inerente ao WebRTC. Como o split-tunnel já abriu mão da privacidade do
vídeo, **não é um conflito novo**, mas exige uma **política**:

- **Manter P2P ligado** com a VPN → mais economia de banda, privacidade parcial (catálogo protegido,
  IP do vídeo exposto a pares); **ou**
- **Desligar P2P no "modo privacidade/VPN"** → mais privacidade, menos economia.

**Decisão default do IZ Play:** manter **P2P ligado** quando a VPN estiver em split-tunnel.
Quando o usuário ativar um futuro **"modo privacidade máxima"**, o player deve desligar o P2P
enquanto a VPN estiver ativa. Assim o modo padrão prioriza economia de banda, e o modo privacidade
prioriza menor exposição do IP do espectador.

**Full-tunnel quebra o P2P (nota p/ o futuro).** Se migrar pra full-tunnel, o WebRTC passa a ver só
o IP interno `10.66.1.x` (ou o IP público único do servidor VPN) → pares fora da VPN não se
alcançam e o P2P **degrada ou funila tudo pelo servidor VPN**, anulando o ganho. Ou seja,
**full-tunnel e P2P são mutuamente exclusivos**.

**Por plataforma:**
- **Web / Desktop (Chromium):** P2P via SDK JS/WebRTC — funciona no split-tunnel (vídeo direto).
- **Android nativo (ExoPlayer):** P2P exigiria o **SDK Android nativo** do SwarmCloud (não o JS em
  WebView) — decisão à parte, alinhada com a recomendação da auditoria.

## 9. Fases sugeridas

1. **Servidor/Painel:** endpoint de provisionamento de peer (`register`/`revoke`) + automação
   `wg set wg0 ...`. Base para desktop e android.
2. **Desktop (Electron):** IPC + toggle + subir/derrubar túnel (resolver a elevação no Windows).
3. **Android:** `VpnService` + lib WireGuard + per-app + foreground service.
4. **(Opcional) Web/Gateway:** egresso do Gateway pela VPN + ativar a rota protegida.

## 10. Decisões em aberto

- **P2P com a VPN ligada:** manter P2P (economia de banda, IP exposto a pares) vs. desligar P2P no
  "modo privacidade" (mais privado, menos economia). Ver §8.
- **Roteamento do catálogo** no app nativo: falar direto com o **Gateway VPS por IP** (preferido,
  §3.1) vs. manter `web.izplay.tv` (Cloudflare) no túnel.
- **Elevação no Windows** (serviço persistente vs. UAC por conexão).
- **Geração de chave:** no dispositivo (melhor) vs. no servidor.
- **Limite de escala:** manter `/24` ou já ampliar a rede.
- Reforço de segurança pendente da auditoria (trocar senhas root, chave SSH) antes de expor
  novos endpoints de provisionamento no Painel.
