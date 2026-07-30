# Focus System

Versão 1.0

---

# Objetivo

Definir como o foco visual e funcional deve operar no IZ Play.

Este documento é especialmente importante para Android TV.

---

# O que é foco

Foco é o item atualmente selecionado pelo usuário via controle remoto, teclado ou navegação assistida.

---

# Regra Principal

Todo item interativo deve ter foco visível.

Nunca permitir foco invisível.

---

# Estados de Foco

## Normal

Item sem destaque.

## Focused

Item selecionado.

Deve exibir:

- Escala levemente maior
- Borda vermelha ou destaque luminoso
- Texto com contraste maior

## Pressed

Item sendo acionado.

Deve ter feedback rápido.

## Disabled

Item indisponível.

Opacidade reduzida.

Não pode receber foco.

---

# Escala

Card focado:

```text
1.05 até 1.08
```

Botão focado:

```text
1.03
```

Sidebar item focado:

```text
1.02
```

---

# Borda

Cor:

```text
#CC0000
```

Espessura:

```text
2dp
```

---

# Animação

Tempo:

```text
120ms a 180ms
```

Nunca maior que 250ms.

---

# Ordem de Foco

A ordem deve seguir a ordem visual.

Nunca pular componentes.

Nunca atravessar seções sem intenção.

---

# Foco Retornado

Ao fechar modal, player ou detalhe:

O foco deve retornar ao item que abriu aquela tela.

---

# Foco em Grid

## Direita

Próximo item da mesma linha.

## Esquerda

Item anterior da mesma linha.

## Baixo

Item mais próximo na linha abaixo.

## Cima

Item mais próximo na linha acima.

---

# Foco em Lista Vertical

## Baixo

Próximo item.

## Cima

Item anterior.

## Direita

Área de detalhe ou player.

## Esquerda

Sidebar.

---

# Foco em Player

Quando controles estão ocultos:

OK mostra controles.

Quando controles estão visíveis:

D-Pad navega entre controles.

---

# Foco em Modal

Modal cria um focus trap.

Foco nunca sai do modal enquanto ele estiver aberto.

---

# Regras

Nunca usar foco padrão invisível.

Nunca depender apenas de mudança de cor sutil.

Nunca usar foco azul padrão Android.

Nunca usar Material Design como referência visual principal.

Sempre usar foco IZ Play.
