# IZ Play — App Android nativo (Kotlin + Jetpack Compose)

Player de IPTV com a interface no estilo IZ Play: sidebar vermelha, coluna de
categorias, lista de canais e painel de player com busca e ações.

O app **não** vem com nenhum canal embutido. Todo o conteúdo vem do **seu provedor
licenciado**, configurado na primeira tela (Xtream Codes ou lista M3U).

## Como abrir

1. Abra a pasta no **Android Studio** (Hedgehog ou mais recente).
2. Aguarde o Gradle sincronizar (baixa as dependências: Compose, Media3, OkHttp, Coil).
3. Rode em um emulador ou dispositivo (`Run ▶`).

Requisitos: Android Studio com JDK 17, `compileSdk 34`, `minSdk 24`.

## Login e host (via Painel Admin)

A tela de login usa o **layout dividido do IZ Play** (logo à esquerda + formulário à direita,
igual ao web/desktop) e pede **apenas Usuário e Senha**. O **host/DNS do provedor NÃO é digitado**
pelo usuário: ele vem do **Painel Admin** (`RemoteConfigClient` → `GET /api/client/config`,
campos `defaultDns`/`dnsServers`). Assim o admin troca a DNS no painel e **todos os clientes
Android trocam sozinhos**, igual ao web/desktop.

- Fallback local se o painel não responder: `cxst.shop` (principal) e `sopvrt.shop` — em
  `RemoteConfigClient.FALLBACK_HOSTS`.
- Para o painel controlar de fato, ele precisa **servir `defaultDns`** (e opcionalmente
  `dnsServers`) no `/api/client/config` — hoje vem vazio; o admin deve setar a DNS lá.
- Fonte da config: `RemoteConfigClient.DEFAULT_URL` (`https://admin.izplay.tv/api/client/config`).
  Se o TLS do painel ainda não estiver ativo, apontar para `http://38.46.142.234/api/client/config`.
- Alternativa **Lista M3U**: link "Usar lista M3U" abre o campo de URL da playlist.

Credenciais (usuário/senha) ficam salvas localmente via DataStore. Fluxo Xtream: `player_api.php`
para catálogo e `/{live|movie|series}/user/pass/{id}` para stream.

## Ícone do app (corrigido p/ TV box Android 7)

O manifest usa `@mipmap/ic_launcher`. O ícone **adaptativo** (`mipmap-anydpi-v26/`) só vale a
partir do Android 8; nas TV boxes com **Android 7.x** (a maioria) o sistema mostrava o **robô
verde padrão** por não haver PNG de fallback. Agora há **PNGs do monograma iZ em todas as
densidades**:

```
res/mipmap-mdpi|hdpi|xhdpi|xxhdpi|xxxhdpi/ic_launcher.png       (48→192px)
res/mipmap-*/ic_launcher_round.png                              (versão redonda)
res/drawable/tv_banner.png                                      (320×180, launcher de Android TV)
```

Manifest: `android:icon` + `android:roundIcon` + `android:banner="@drawable/tv_banner"`.
Se a box **mantiver o ícone antigo em cache**, desinstale e reinstale o APK (o `versionCode`
subiu para 22 justamente para forçar atualização). Para o ícone metálico exato no futuro,
regenerar via **Android Studio → Image Asset** com o PNG quadrado.

## Navegação por controle remoto (D-pad) — padrão web/desktop

O app agora é **navegável 100% pelas setas do controle**, com foco visível igual ao web/desktop:

- **Sidebar retrátil:** colapsada mostra só os ícones; ao focar (seta) **expande** e revela os
  rótulos — mesmo comportamento do hover no desktop. O primeiro item recebe **foco automático**
  ao abrir. Seta pra direita entra no conteúdo; seta pra esquerda volta pra sidebar.
- **Foco visível:** cartões (Home, Filmes, Séries) ganham **borda branca + leve zoom**; linhas
  (canais, categorias, episódios) ganham **destaque vermelho**. Peça central reutilizável:
  `ui/components/TvFocus.kt` (`TvCard` para cartões, `rememberTvFocus()` para linhas).
- **Home rolável:** vira `LazyColumn` — as fileiras ("Mais assistidos", "Filmes em destaque"…)
  rolam e o item focado é trazido pra tela automaticamente.

Todas as abas (Início, Canais, Filmes, Séries, Favoritos, Config) compartilham o mesmo padrão de
layout e navegação — o "produto padrão" pedido, mesmo com backend nativo (Kotlin/Compose).

## Estrutura

```
data/model        → Channel, Category, ProviderConfig, EpgEntry
data/remote       → M3uParser, XtreamClient
data/repository   → ContentRepository, SettingsStore
player            → VideoPlayer (ExoPlayer/Media3)
ui/components     → Sidebar, CategoryColumn, ChannelColumn, PlayerPanel
ui/screens        → SetupScreen, HomeScreen, MoviesScreen, SeriesScreen, SettingsScreen
ui/theme          → cores e tipografia (vermelho/preto)
ui/MainViewModel  → estado da tela
```

## Configurações (tela CONFIG)

A aba **CONFIG** da sidebar abre a `SettingsScreen` (antes era um placeholder). Ela mostra,
**somente leitura**, o estado real do app:

- **Conta / Provedor:** modo (Xtream/M3U), servidor, usuário, senha mascarada e EPG.
- **Aplicativo:** nº de canais favoritos, canais carregados e a versão.
- **Trocar conta / Sair:** faz logout (`MainViewModel.logout()` → `SettingsStore.clearConfig()`),
  mantém os favoritos e volta para a tela de setup.

Peças envolvidas: `SettingsScreen.kt` (nova), `UiState.config` + `MainViewModel.logout()`,
`SettingsStore.clearConfig()`, e o roteamento do `NavItem.CONFIG` no `HomeScreen`.

## Gerar o APK de teste (linha de comando)

Com o JDK do Android Studio, no PowerShell:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
cd android
.\gradlew.bat assembleDebug
# APK: app\build\outputs\apk\debug\app-debug.apk
```

Instalar num dispositivo/TV box conectado por adb: `adb install -r app\build\outputs\apk\debug\app-debug.apk`.

## Próximos passos sugeridos

- **EPG**: fazer o parse do XMLTV apontado em `epgUrl` e preencher o guia abaixo
  do player (a UI já reserva o espaço conceitual).
- **Filmes / Séries (VOD)**: os endpoints `get_vod_streams` e `get_series` do
  Xtream alimentam as abas FILMES e SÉRIES da sidebar.
- **Tela cheia**: implementar a ação de fullscreen (Activity dedicada ou
  `WindowInsetsController`).
- **Suporte a controle remoto (Android TV)**: adicionar foco D-pad com
  `Modifier.focusable()` e ordenação de foco.

## Observação legal

Use o app apenas com fontes de conteúdo que você tem direito de transmitir
(provedor licenciado, seu próprio streaming, canais abertos). A redistribuição
de canais sem licenciamento das emissoras é ilegal.
