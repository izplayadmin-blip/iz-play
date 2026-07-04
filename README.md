# IZ Play v2.1

Pasta oficial e consolidada do projeto. As versoes antigas ficam apenas para
consulta em `C:\Users\deivi\Desktop\Versões Anteriores`.

## Componentes

- `app`: Desktop Player Electron com MPV, FFmpeg e fallback pelo gateway.
- `web-player`: Web Player/PWA com HLS, gateway e integracao SwarmCloud.
- `android`: aplicativo nativo para Android TV e TV Box, em Kotlin/Compose.
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

# Gerar APK Android debug
npm run android:debug
```

O Desktop Player tambem pode ser aberto por `Abrir Player.bat`.

## Regra de trabalho

Toda nova melhoria deve ser feita primeiro nesta pasta `iz-play-v2.1`. As
pastas arquivadas nao devem voltar a ser usadas como base de desenvolvimento.
