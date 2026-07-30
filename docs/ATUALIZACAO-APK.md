# Atualização de APK do IZ Play

O painel oferece três canais independentes:

- `internal`: testes da equipe;
- `reseller`: validação com revendas;
- `production`: clientes finais.

Plataformas aceitas: `android-mobile` e `android-tv`.

## Consulta pública usada pelo aplicativo

```http
GET /api/client/updates/android-mobile?channel=internal&versionCode=2
```

O endpoint informa se existe uma versão superior, se ela é obrigatória, notas,
tamanho, SHA-256 e endereço HTTPS. Ele não expõe credenciais administrativas.

## Publicação

Primeiro obtenha uma sessão em `POST /api/login`. Depois envie o APK:

```http
POST /api/updates/android-mobile/internal/apk
  ?versionCode=3
  &versionName=3.0.2
  &minimumVersionCode=2
  &mandatory=false
  &releaseNotes=Correções%20e%20melhorias
Content-Type: application/vnd.android.package-archive
X-Session-Token: SESSAO_DO_PAINEL

<bytes do APK>
```

O servidor calcula o SHA-256, grava o arquivo fora da pasta pública comum,
publica os metadados e disponibiliza o download em `/downloads/`.

## Regras de lançamento

1. Nunca reutilizar ou diminuir `versionCode`.
2. Assinar todos os APKs de produção com a mesma chave.
3. Publicar primeiro em `internal`, depois `reseller` e por último `production`.
4. Usar `mandatory=true` somente para falhas críticas ou versões incompatíveis.
5. Fazer backup de `panel/data/updates.json`, `panel/data/apks/` e da chave de assinatura.
