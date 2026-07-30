# IZ Play Android TV e serviços — baseline do beta

Data: 2026-07-30

## Identidade do aplicativo

- Aplicativo: IZ Play Android TV
- Application ID: `com.izplay.tv`
- Versão: `2.1.45`
- Version code: `66`
- minSdk: `24`

## Validação executada

Android:

```text
gradlew clean testDebugUnitTest assembleDebug assembleRelease
```

Serviços:

```text
node --check panel/server.js
node --check gateway/server.js
node --check catalog-gateway/server.js
node --check supernode/index.js
```

Resultado:

- APK debug: gerado;
- APK release: gerado;
- lint vital do release: aprovado;
- módulo Android TV ainda não contém testes unitários (`NO-SOURCE`);
- arquivos Node.js: sintaxe aprovada;
- release assinado com certificado Android Debug e, portanto, inadequado para distribuição final.

## Artefatos locais de referência

Os APKs não são versionados no Git.

| Variante | Tamanho | SHA-256 |
|---|---:|---|
| Debug | 46.655.336 bytes | `D62CB8F234823298F59704C370C84A9952EA7B3592D71256D8A704FAE155760D` |
| Release atual | 27.646.355 bytes | `D6DE0837CCB6BEFBCB67F85790438749B92174E6890F3452EC8D6CB3AEBD638F` |

Certificado do release atual:

```text
DN: C=US, O=Android, CN=Android Debug
SHA-256: 92ecde2cf9dd9a24640092454df96dbd2254b9295dfdbbcaa8fbd93b69060055
```

## Piloto P2P congelado

- somente canais ao vivo;
- IDs locais atuais: `82755` e `1168916`;
- candidatos atuais: proxy local SwarmCloud, origem direta e gateway externo;
- métricas disponíveis: peers, bytes P2P recebidos/enviados, bytes HTTP e conexão com swarm.

## Bloqueios conhecidos

- substituir assinatura debug por chave release definitiva;
- retirar e rotacionar o token SwarmCloud já exposto no histórico;
- proteger endpoints públicos de telemetria;
- ativar e verificar HTTPS no painel;
- impedir exposição da origem no fluxo final;
- transferir a allowlist P2P para configuração remota;
- criar testes unitários para roteamento, fallback e sanitização;
- executar smoke test autenticado com conteúdo autorizado.
