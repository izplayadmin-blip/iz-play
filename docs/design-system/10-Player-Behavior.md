# Player Behavior

Versão 1.0

---

# Objetivo

Definir o comportamento do player de vídeo do IZ Play.

---

# Tipos de Player

- Live Player
- VOD Player
- Series Player
- Media Modal
- Fullscreen Player

---

# Live Player

Usado em canais ao vivo.

Deve exibir:

- Vídeo
- Badge AO VIVO
- Nome do canal
- Programa atual
- Progresso do programa
- Botões de ação

---

# VOD Player

Usado em filmes.

Deve exibir:

- Vídeo
- Controles
- Progresso
- Tempo atual
- Tempo total

---

# Series Player

Mesmo VOD Player, com contexto de episódio.

---

# Controles

Controles oficiais:

- Play/Pause
- Voltar canal
- Próximo canal
- Volume
- Favorito
- Tela cheia
- Reconectar
- Informações

---

# Auto Hide

Controles somem após inatividade.

Tempo recomendado:

```text
3 a 5 segundos
```

---

# Reconectar

O botão Reconectar deve:

1. Parar stream atual
2. Limpar player
3. Reabrir stream
4. Manter canal selecionado

---

# Erro de reprodução

Mostrar mensagem clara.

Opções:

- Tentar novamente
- Reconectar
- Usar rota alternativa
- Abrir player externo, quando existir

---

# Fullscreen

Ao entrar:

- Vídeo ocupa tela toda
- Controles aparecem por alguns segundos
- Sidebar desaparece
- Foco vai para player

Ao sair:

- Restaurar layout anterior
- Manter reprodução
- Restaurar foco

---

# Android TV

OK:

- Mostra controles.

BACK:

- Sai do fullscreen ou oculta controles.

Setas:

- Navegam entre controles.

---

# Regra

Player nunca deve travar a interface principal.

Se o stream falhar, a UI continua navegável.
