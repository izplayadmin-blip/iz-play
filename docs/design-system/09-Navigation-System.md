# Navigation System

Versão 1.0

---

# Objetivo

Padronizar a navegação entre telas, listas, cards, player e menus.

---

# Telas principais

- Home
- Favoritos
- Canais
- Filmes
- Séries
- Configurações
- Notificações
- Usuário

---

# Navegação Global

A Sidebar é a navegação global.

Sempre disponível.

Sempre à esquerda.

---

# Pilha de Navegação

O app deve manter histórico interno.

Exemplo:

```text
Home > Filme > Player
```

Voltar:

```text
Player > Filme > Home
```

---

# Rotas Oficiais

## Home

Rota:

```text
home
```

## Favoritos

Rota:

```text
favorites
```

## Canais

Rota:

```text
live
```

## Filmes

Rota:

```text
vod
```

## Séries

Rota:

```text
series
```

## Configurações

Rota:

```text
config
```

## Notificações

Rota:

```text
notifications
```

## Usuário

Rota:

```text
user
```

---

# Regra de Rotas

Nunca criar rota duplicada.

Nunca criar tela paralela com função parecida.

---

# Android TV

A navegação deve funcionar sem mouse.

Se qualquer tela exigir mouse, ela está errada.

---

# Regra Final

A navegação deve ser previsível, curta e sem becos sem saída.
