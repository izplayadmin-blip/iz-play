# Porting Guide

# Web Player para Android TV

Versão 1.0

---

# Objetivo

Este documento orienta a conversão da interface oficial do Web Player para Android TV usando Jetpack Compose.

Android não é uma nova interpretação.

Android é um port.

---

# Equivalências básicas

HTML/CSS para Compose:

```text
div -> Box, Row ou Column
display flex row -> Row
display flex column -> Column
position absolute -> Box com align/offset
padding -> Modifier.padding()
margin -> Arrangement.spacedBy() ou padding externo
border-radius -> RoundedCornerShape
background -> Modifier.background()
width/height -> Modifier.width() / Modifier.height()
font-size -> sp
font-weight -> FontWeight
transition -> animate*AsState
hover -> focus/interaction state
box-shadow -> evitar ou usar sombra discreta
```

---

# Unidades

CSS px não deve ser convertido cegamente para dp.

A conversão deve respeitar proporção visual em TV.

Regra prática:

- medidas estruturais grandes podem ir para dp próximo
- tipografia deve usar sp
- espaçamentos devem usar tokens
- radius deve usar tokens

---

# Cores

As cores devem vir dos tokens oficiais.

Nunca criar cor inline sem justificativa.

Tokens principais:

- background
- surface
- surfaceHover
- primaryRed
- textPrimary
- textSecondary
- border

---

# Sidebar

O comportamento visual deve copiar o Web Player:

- esquerda
- recolhida
- expandida
- ícones
- item ativo
- texto aparece quando expandida
- transição curta
- foco visível no Android TV

Nunca mover a Sidebar para o topo.

Nunca transformar em Bottom Navigation.

---

# Home

A Home deve seguir a ordem oficial:

1. Hero
2. Continuar assistindo
3. Seções horizontais
4. Recomendações
5. Filmes
6. Séries

Nunca transformar a Home em grid único.

---

# Hero

O Hero deve manter:

- destaque superior
- imagem/backdrop
- overlay escuro
- título forte
- descrição curta
- CTA principal
- transição para fileiras abaixo

---

# Cards

Cards devem manter:

- proporção oficial
- radius oficial
- imagem cover
- título legível
- foco vermelho
- escala leve
- scroll horizontal nas fileiras

---

# Foco

No Android TV, foco é tão importante quanto layout.

Todo item clicável precisa ser focável.

O foco deve:

- ser visível
- usar vermelho IZ
- ter escala leve
- não quebrar layout
- respeitar ordem visual

---

# D-Pad

O D-Pad deve seguir o Behavior System:

- esquerda abre/foca Sidebar quando no primeiro item
- direita volta ao conteúdo
- cima/baixo navega linhas
- OK executa
- BACK retorna estado anterior

---

# Player

O player deve manter:

- controles inferiores
- overlay escuro
- fullscreen previsível
- auto-hide
- reconectar
- foco nos controles
- BACK para sair de fullscreen antes de sair da tela

---

# Erros comuns da IA

Evitar:

- usar Scaffold padrão do Material
- criar TopAppBar
- colocar menu inferior
- centralizar tudo
- usar cards genéricos
- ignorar scroll horizontal
- esquecer foco
- resetar foco ao voltar
- mudar a ordem das telas
- misturar lógica e layout em arquivo gigante

---

# Processo correto de portabilidade

Para cada tela:

1. Abrir implementação do Web Player.
2. Identificar blocos principais.
3. Mapear para Compose.
4. Criar componente equivalente.
5. Aplicar tokens.
6. Aplicar foco.
7. Aplicar D-Pad.
8. Comparar visualmente.
9. Corrigir diferenças.
10. Só depois seguir para próxima tela.

---

# Regra Final

Se o Android ficar bonito, mas diferente do Web Player, está errado.

Se o Android ficar fiel ao Web Player, está certo.
