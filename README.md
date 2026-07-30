# IZ Play v2.1

Pasta oficial e consolidada do projeto. As versoes antigas ficam apenas para
consulta em `C:\Users\deivi\Desktop\Versões Anteriores`.

## Componentes

- `app`: Desktop Player Electron com MPV, FFmpeg e fallback pelo gateway.
- `web-player`: Web Player/PWA com HLS, gateway e integracao SwarmCloud.
- `android`: aplicativo Android TV/TV Box oficial, com logo preta/vermelha, pacote `com.izplay.tv`.
- `panel`: painel administrativo, configuracao e telemetria.
- `gateway`: proxy, probe e transcode de midia.
- `deploy`: configuracoes de Nginx, systemd e WireGuard.
- `docs`: arquitetura e procedimentos de deploy.
- `releases`: artefatos prontos, como o APK Android.
- `scripts`: validacoes locais.

## Comandos

```powershell
# Desktop Player
npm run app

# Painel
npm run panel

# Gateway
npm run gateway

# Validar a estrutura
npm run check

# Gerar APK Android TV debug (logo preta/vermelha, pacote com.izplay.tv)
npm run android:debug
```

O Desktop Player tambem pode ser aberto por `Abrir Player.bat`.

## Regra de trabalho

Toda nova melhoria deve ser feita primeiro nesta pasta `iz-play-v2.1`. As
pastas arquivadas nao devem voltar a ser usadas como base de desenvolvimento.
O Android oficial e o pacote `com.izplay.tv`, gerado pela pasta `android/`.
O pacote Capacitor `com.izplay.player` fica fora do fluxo do TV Box.
