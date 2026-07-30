# IZ Play Design System v1.0

Documento unificado dos Volumes 1, 2, 3 e 4.

---



---

<!-- FILE: README.md -->


# IZ Play Design System v1.0

Documentação oficial de identidade visual, componentes, comportamento e guia de IA do IZ Play.

Regra central:

**Web Player é a fonte de verdade. Desktop e Android são ports. Não redesenhar. Portar, corrigir e validar.**

## Estrutura

```text
.ai/
design-system/
  tokens/
  components/
  screens/
```

## Volumes

1. Fundamentos
2. Componentes
3. Behavior System
4. AI Guide / Porting Guide



---

<!-- FILE: design-system/01-Principles.md -->


# IZ Play Design System

# Volume 1

# Princípios

Versão 1.0

---

# Objetivo

O Design System do IZ Play define a identidade visual, os componentes, os comportamentos e as regras utilizadas em todas as plataformas do projeto.

Ele existe para garantir consistência entre:

- Web Player
- Desktop Player
- Android TV
- Painel Administrativo
- Futuras plataformas

Todo novo componente deve seguir este documento.

---

# Fonte Oficial

A implementação oficial do layout é sempre o Web Player.

Hierarquia:

1. Web Player
2. Desktop Player
3. Android TV

Nenhuma plataforma pode modificar a identidade visual.

Caso exista diferença entre plataformas, o Web Player possui prioridade.

---

# Filosofia

O IZ Play foi desenvolvido para desaparecer.

O usuário deve perceber o conteúdo.

Nunca a interface.

A interface deve ser:

- limpa
- rápida
- previsível
- elegante
- minimalista

---

# Princípios

## Conteúdo em primeiro lugar

Filmes, séries e canais são o foco.

A interface apenas facilita o acesso.

Nunca competir visualmente com o conteúdo.

---

## Consistência

Toda tela deve seguir o mesmo padrão.

Mesmo espaçamento.

Mesmo comportamento.

Mesma linguagem visual.

Mesmo posicionamento.

---

## Simplicidade

Evitar excesso de elementos.

Toda informação deve possuir uma finalidade.

---

## Performance

A interface deve parecer leve.

Animações curtas.

Poucas sombras.

Poucos efeitos.

Priorizar FPS.

---

## Navegação

A navegação deve ser possível utilizando:

- Controle remoto
- Mouse
- Touch
- Teclado

O foco principal do projeto é Android TV.

---

# Regra de Ouro

Nunca redesenhar componentes.

Sempre reutilizar.

Sempre portar.

Nunca reinterpretar.



---

<!-- FILE: design-system/02-Colors.md -->


# Colors

Versão 1.0

---

# Primary

Cor principal do projeto.

HEX:

```text
#CC0000
```

Utilização:

- Botões
- Hover
- Seleção
- Barra ativa
- Foco
- Elementos de ação

---

# Background

HEX:

```text
#000000
```

Toda interface utiliza fundo preto.

Nunca utilizar branco como fundo principal.

---

# Surface

HEX:

```text
#111111
```

Utilizado em:

- Cards
- Menus
- Sidebar
- Painéis
- Modais

---

# Surface Hover

HEX:

```text
#1B1B1B
```

Utilizado em hover e estados ativos discretos.

---

# Border

HEX:

```text
#2A2A2A
```

Separadores discretos.

Nunca utilizar bordas pesadas.

---

# Texto Principal

HEX:

```text
#FFFFFF
```

---

# Texto Secundário

HEX:

```text
#A8A8A8
```

---

# Sucesso

HEX:

```text
#2ECC71
```

---

# Atenção

HEX:

```text
#F39C12
```

---

# Erro

HEX:

```text
#E74C3C
```

---

# Regras

Nunca utilizar novas cores sem registrar no Design System.

Toda nova cor deve ser adicionada neste documento e nos tokens.

Não utilizar cores padrão do Material Design como identidade principal.



---

<!-- FILE: design-system/03-Typography.md -->


# Typography

Versão 1.0

---

# Fonte Oficial

Fonte principal:

```text
Inter
```

Fallback:

```text
-apple-system, BlinkMacSystemFont, Segoe UI, sans-serif
```

---

# Hierarquia

## Hero

```text
48px / 48sp
Bold
```

## Título

```text
40px / 40sp
Bold
```

## Subtítulo

```text
24px / 24sp
Bold ou Medium
```

## Categoria

```text
20px / 20sp
Medium
```

## Card

```text
18px / 18sp
Bold ou Medium
```

## Texto

```text
16px / 16sp
Regular
```

## Descrição

```text
14px / 14sp
Regular
```

## Legenda

```text
12px / 12sp
Medium
```

---

# Pesos

- Regular
- Medium
- Bold
- Black quando necessário em títulos de grande impacto

---

# Regras

Nunca utilizar outra fonte.

Nunca misturar tipografias.

Nunca usar fonte decorativa.

Nunca usar tipografia padrão Android se ela quebrar a identidade visual.



---

<!-- FILE: design-system/04-Spacing.md -->


# Spacing

Versão 1.0

---

# Escala Oficial

Nunca utilizar medidas aleatórias.

Utilizar somente:

```text
4
8
12
16
24
32
40
48
64
96
```

---

# Uso

## Padding padrão

```text
24
```

## Cards

```text
16
```

## Hero

```text
32
```

## Sidebar

```text
24
```

## Separação entre seções

```text
48
```

## Separação grande

```text
64
```

---

# Regras

Não usar espaçamentos improvisados.

Não alterar espaçamentos para "parecer melhor" sem comparar com o Web Player.

Toda exceção deve ser documentada.



---

<!-- FILE: design-system/05-Layout.md -->


# Layout

Versão 1.0

---

# Estrutura Oficial

A estrutura principal do IZ Play é:

```text
Sidebar + Conteúdo
```

A Sidebar nunca muda de posição.

Sempre esquerda.

---

# Home

Ordem oficial:

1. Hero
2. Continue Assistindo
3. Minha Lista
4. Recomendações
5. Canais recentes
6. Filmes
7. Séries

Nunca inverter esta ordem sem decisão explícita de produto.

---

# Desktop

Mesmo layout do Web Player.

---

# Android

Mesmo layout do Web Player.

Apenas adaptar foco para D-Pad.

---

# Painel

Utiliza identidade visual do IZ Play.

Não utilizar Material Design puro.

---

# Regras

Não transformar a Home em grid único.

Não transformar Sidebar em navegação inferior.

Não criar TopBar como navegação principal.

Não esconder navegação essencial.



---

<!-- FILE: design-system/06-Components.md -->


# IZ Play Design System

# Volume 2

# Componentes

Versão 1.0

---

# Introdução

Este documento define todos os componentes reutilizáveis do IZ Play.

Nenhum componente pode ser desenvolvido sem seguir esta documentação.

Sempre reutilizar componentes existentes.

Nunca criar versões alternativas.

O Web Player continua sendo a implementação oficial.

---

# Componentes Oficiais

- Sidebar
- Hero
- Movie Card
- Series Card
- Channel Card
- Continue Watching
- Section Header
- Player Controls
- Search
- Dialog
- Toast
- Loading
- Skeleton
- Progress
- Buttons
- Inputs
- Lists
- Category Menu
- Navigation

---

# Estados

Todo componente deve possuir:

- Default
- Hover
- Focused
- Pressed
- Disabled
- Loading
- Selected

---

# Regras

Nunca modificar sem justificativa:

- dimensões
- alinhamento
- tipografia
- cores
- animações



---

<!-- FILE: design-system/components/sidebar.md -->


# Sidebar

## Objetivo

Permitir navegação rápida entre as áreas do sistema.

---

## Localização

Sempre esquerda.

Nunca direita.

Nunca superior.

Nunca inferior.

---

## Estados

- Expandida
- Recolhida

---

## Largura

Expandida:

```text
240dp
```

Recolhida:

```text
72dp
```

---

## Padding

```text
24dp
```

---

## Espaçamento entre itens

```text
12dp
```

---

## Ícones

```text
24dp
SVG
Brancos
```

---

## Texto

```text
18sp
Inter Medium
```

---

## Hover

Background Surface Hover.

Escala:

```text
1.02
```

---

## Focus Android TV

Borda vermelha.

Escala:

```text
1.03
```

---

## Animação

```text
250ms
Ease In Out
```

---

## Regras

Nunca alterar largura.

Nunca alterar posição.

Nunca alterar comportamento.

Sempre utilizar exatamente o comportamento do Web Player.



---

<!-- FILE: design-system/components/hero.md -->


# Hero

## Objetivo

Destacar o principal conteúdo.

---

## Altura

```text
320dp
```

---

## Largura

```text
100%
```

---

## Imagem

Cover.

---

## Overlay

Gradiente preto.

---

## Título

```text
48sp
Bold
```

---

## Descrição

```text
16sp
Máximo 3 linhas
```

---

## Botão

Assistir.

Sempre abaixo da descrição.

---

## Regras

Nunca centralizar.

Nunca utilizar mais de um botão principal.

Nunca utilizar sombras pesadas.



---

<!-- FILE: design-system/components/movie-card.md -->


# Movie Card

## Proporção

```text
16:9
```

---

## Radius

```text
18dp
```

---

## Imagem

Cover.

---

## Título

```text
18sp
Bold
```

---

## Hover

Escala:

```text
1.05
```

---

## Focus Android

Escala:

```text
1.08
```

Borda vermelha.

---

## Animação

```text
150ms
```

---

## Regras

Nunca adicionar sombras pesadas.

Nunca alterar proporção.

Nunca modificar radius sem alterar token oficial.



---

<!-- FILE: design-system/components/continue-watching.md -->


# Continue Watching

## Scroll

Horizontal.

---

## Cards

Mesmo padrão de Movie Card.

---

## Título

```text
24sp
```

---

## Espaçamento

```text
24dp
```

---

## Android TV

Scroll pelo D-Pad.

---

## Desktop

Mouse e teclado.

---

## Web

Mouse, teclado e touch quando aplicável.



---

<!-- FILE: design-system/components/player.md -->


# Player

## Objetivo

Reproduzir vídeo.

---

## Componentes

- Timeline
- Play
- Pause
- Volume
- Fullscreen
- EPG
- Qualidade
- Legenda
- Áudio
- Reconectar

---

## Controles

Parte inferior.

---

## Título

Parte superior.

---

## Informações

Logo abaixo.

---

## Overlay

Preto com transparência.

---

## Timeline

Sempre vermelha.

---

## Android TV

Controle pelo D-Pad.

---

## Desktop/Web

Mouse e teclado.



---

<!-- FILE: design-system/components/search.md -->


# Search

## Campo

```text
48dp
```

---

## Radius

```text
12dp
```

---

## Placeholder

Texto Secundário.

---

## Ícone

Lupa.

```text
24dp
```

---

## Resultados

Lista vertical.

---

## Regras

Nunca abrir modal sem necessidade.

Preferir tela integrada.



---

<!-- FILE: design-system/components/dialog.md -->


# Dialog

## Radius

```text
20dp
```

---

## Padding

```text
32dp
```

---

## Botões

- Confirmar
- Cancelar

---

## Regras

Nunca usar mais de dois botões principais.

Modal deve prender foco.



---

<!-- FILE: design-system/components/loading.md -->


# Loading

## Spinner

Vermelho.

---

## Skeleton

Cinza escuro.

---

## Regras

Nunca permitir loading infinito sem mensagem.

Sempre existir timeout em ações críticas.



---

<!-- FILE: design-system/components/buttons.md -->


# Buttons

## Altura

```text
48dp
```

---

## Radius

```text
12dp
```

---

## Padding

```text
24dp
```

---

## Texto

```text
16sp
Medium
```

---

## Primary

Vermelho.

---

## Secondary

Surface.

---

## Hover

Escurece discretamente.

---

## Disabled

```text
50% opacity
```



---

<!-- FILE: design-system/components/section-header.md -->


# Section Header

## Título

```text
24sp
Bold
```

---

## Botão

Ver Mais.

```text
16sp
```

---

## Espaçamento inferior

```text
16dp
```

---

## Regra

Nunca centralizar.



---

<!-- FILE: design-system/components/navigation.md -->


# Navigation Component

## Android TV

```text
← Sidebar
→ Conteúdo
↑↓ Itens
OK Seleciona
BACK Retorna
```

---

## Desktop

Mouse e teclado.

---

## Web

Mouse, teclado e touch.



---

<!-- FILE: design-system/components/animations.md -->


# Animations

## Sidebar

```text
250ms
```

---

## Cards

```text
150ms
```

---

## Dialogs

```text
200ms
```

---

## Fade

```text
200ms
```

---

## Scale

```text
1.05
```

---

## Regra

Nunca usar animações maiores que 300ms sem justificativa.



---

<!-- FILE: design-system/07-Behavior-System.md -->


# IZ Play Design System

# Volume 3

# Behavior System

Versão 1.0

---

# Objetivo

Este documento define o comportamento oficial da interface do IZ Play.

Ele deve ser seguido por:

- Web Player
- Desktop Player
- Android TV
- Futuras plataformas

O objetivo é garantir que o usuário sinta a mesma experiência em qualquer dispositivo.

---

# Regra Principal

A aparência vem do Design System.

O comportamento vem deste documento.

Nenhuma IA pode criar comportamento alternativo sem autorização.

---

# Plataformas

## Web Player

Controle principal:

- Mouse
- Teclado
- Touch em telas compatíveis

## Desktop Player

Controle principal:

- Mouse
- Teclado
- Controle remoto se suportado

## Android TV

Controle principal:

- D-Pad
- Botão OK
- Botão Voltar
- Botão Menu quando existir

---

# Princípio de Navegação

O usuário nunca deve se perder.

Toda ação deve ter um retorno previsível.

Toda tela deve lembrar o último foco.

Todo botão Voltar deve retornar para o estado anterior.

Nunca jogar o usuário para o início da tela sem necessidade.

---

# Sistema de Foco

## Android TV

Todo item interativo precisa ser focável.

Itens focáveis:

- Sidebar items
- Cards
- Botões
- Canais
- Categorias
- Busca
- Player controls
- Configurações
- Diálogos

Itens não focáveis:

- Títulos
- Descrições
- Textos informativos
- Ícones decorativos
- Backgrounds

---

# Estado de Foco

Todo item focado deve ter:

- Escala levemente maior
- Borda ou destaque vermelho
- Contraste maior
- Animação curta

Nunca usar foco invisível.

---

# Foco Padrão por Tela

## Home

Primeiro foco:

- Hero button "Assistir"
- Se não houver conteúdo no Hero, primeiro card da primeira fileira

## Canais

Primeiro foco:

- Canal selecionado anteriormente
- Se não existir, primeiro canal da lista

## Filmes

Primeiro foco:

- Primeiro card visível

## Séries

Primeiro foco:

- Primeiro card visível

## Configurações

Primeiro foco:

- Primeiro item da primeira seção

---

# Memória de Foco

Cada tela deve salvar o último foco.

Exemplo:

O usuário estava em:

```text
Home > Filmes para assistir agora > terceiro card
```

Saiu para Canais.

Ao voltar para Home, o foco deve retornar ao terceiro card.

Nunca resetar para o topo sem necessidade.

---

# D-Pad

## Direita

Move foco para item à direita.

Se estiver na Sidebar recolhida, move para o conteúdo.

## Esquerda

Move foco para item à esquerda.

Se estiver no primeiro item da linha, abre ou foca a Sidebar.

## Cima

Move para o item acima.

## Baixo

Move para o item abaixo.

## OK

Executa item focado.

## Voltar

Segue a pilha de navegação.

Nunca fechar o aplicativo imediatamente se houver tela anterior.

---

# Botão Voltar

Ordem de prioridade:

1. Fechar diálogo aberto
2. Fechar teclado/busca
3. Sair do fullscreen
4. Fechar sidebar expandida
5. Voltar para tela anterior
6. Voltar para Home
7. Perguntar se deseja sair do app

---

# Sidebar Behavior

## Estado Recolhido

Mostra:

- Logo
- Ícones
- Sem texto ou texto oculto

## Estado Expandido

Mostra:

- Logo
- Ícones
- Textos
- Item ativo
- Configurações
- Usuário

## Abrir Sidebar

A Sidebar pode abrir quando:

- Usuário pressiona esquerda no primeiro item da tela
- Usuário move mouse sobre ela no Web/Desktop
- Usuário pressiona botão Menu, se disponível
- Usuário toca no ícone de menu, se existir

## Fechar Sidebar

A Sidebar fecha quando:

- Usuário pressiona direita
- Usuário seleciona uma opção
- Usuário pressiona Voltar
- Usuário move foco para o conteúdo

---

# Fileiras Horizontais

## Direita

Vai para próximo card.

Se chegar ao fim, manter no último item.

## Esquerda

Vai para card anterior.

Se estiver no primeiro card, ir para Sidebar.

## Baixo

Vai para a fileira abaixo mantendo posição aproximada.

## Cima

Vai para fileira acima mantendo posição aproximada.

---

# Cards Behavior

## OK

Filme:

- Abre detalhe do filme

Série:

- Abre detalhe da série

Canal:

- Inicia reprodução do canal

Histórico:

- Continua de onde parou

---

# Canais Behavior

A tela de Canais deve priorizar velocidade.

Elementos:

- Lista de canais
- Player
- Informações do canal/programa
- Categorias
- Últimos canais

---

# Player Controls

## OK

Se controles ocultos:

- Mostrar controles

Se controle focado:

- Executar controle

## Baixo

Mostra controles inferiores.

## Cima

Mostra informações superiores.

## Voltar

Se fullscreen:

- Sai do fullscreen

Se controles visíveis:

- Oculta controles

Se normal:

- Retorna para tela anterior

---

# Busca Behavior

## Abrir Busca

A busca deve focar automaticamente no input.

## Digitar

Resultados atualizam em tempo real.

## Voltar

Se busca contém texto:

- Limpa texto

Se busca está vazia:

- Sai da busca

---

# Modal/Dialog Behavior

Ao abrir um modal:

- Escurecer fundo
- Bloquear foco fora do modal
- Primeiro foco no botão principal

Voltar fecha modal.

Nunca permitir foco escapar do modal.

---

# Toast Behavior

Toast serve para feedback rápido.

Tempo:

```text
2 a 4 segundos
```

Toast nunca pode bloquear navegação.

---

# Loading Behavior

Toda ação de loading precisa ter timeout.

Nunca permitir loading infinito sem mensagem.

---

# Checklist de Validação

Antes de finalizar qualquer tela:

- D-Pad funciona?
- Voltar funciona?
- Foco é visível?
- Sidebar funciona?
- Último foco é preservado?
- Player não perde estado?
- Scroll não trava?
- Modal prende foco?
- Toast não bloqueia navegação?
- Layout continua igual ao Web Player?

---

# Regra Final

O IZ Play deve parecer simples para o usuário.

Mas por baixo deve ter comportamento previsível, consistente e profissional.

A IA nunca deve improvisar navegação.

A IA deve seguir este documento.



---

<!-- FILE: design-system/08-Focus-System.md -->


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



---

<!-- FILE: design-system/09-Navigation-System.md -->


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



---

<!-- FILE: design-system/10-Player-Behavior.md -->


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



---

<!-- FILE: design-system/11-AI-Guide.md -->


# IZ Play Design System

# Volume 4

# AI Guide

Versão 1.0

---

# Objetivo

Este documento define como ferramentas de IA devem trabalhar no projeto IZ Play.

Ele deve ser seguido por:

- Claude Code
- Codex
- Cursor
- Gemini CLI
- outros agentes de programação

A IA não deve atuar como designer livre.

A IA deve atuar como engenheiro de portabilidade e manutenção.

---

# Hierarquia de Verdade

A interface oficial do IZ Play é o Web Player.

Ordem de autoridade:

1. Web Player
2. Design System
3. Behavior System
4. Android/Desktop/Painel

Se houver conflito entre Android e Web Player, o Web Player vence.

Se houver conflito entre uma opinião da IA e este documento, este documento vence.

---

# Regra Principal

A IA nunca deve redesenhar.

A IA deve:

- analisar
- comparar
- portar
- corrigir
- validar

Nunca inventar.

---

# Fluxo obrigatório antes de alterar código

Antes de qualquer alteração, a IA deve executar mentalmente este fluxo:

1. Entender a tarefa.
2. Identificar a tela ou componente afetado.
3. Consultar o Web Player.
4. Consultar o Design System.
5. Consultar o Behavior System.
6. Listar diferenças.
7. Alterar apenas o necessário.
8. Validar visualmente.
9. Informar arquivos alterados.
10. Informar o que não foi alterado.

---

# O que a IA pode fazer

A IA pode:

- corrigir layout divergente
- portar CSS para Compose
- criar componentes equivalentes
- organizar código sem alterar aparência
- melhorar navegação por D-Pad
- corrigir foco
- corrigir bugs
- ajustar performance
- documentar comportamento
- criar testes
- melhorar nomes internos sem alterar UI

---

# O que a IA não pode fazer

A IA não pode:

- trocar identidade visual
- aplicar Material Design visual
- mudar a posição da Sidebar
- criar nova Home
- mudar ordem das seções
- alterar cores oficiais
- alterar espaçamentos sem justificativa
- criar animações novas
- trocar tipografia
- remover elementos existentes
- adicionar elementos visuais sem autorização
- refatorar tela inteira sem necessidade

---

# Padrão de resposta esperado da IA

Ao trabalhar no projeto, a IA deve responder assim:

## Diagnóstico

O que encontrei.

## Diferenças

O que está diferente do Web Player.

## Plano

O que vou corrigir.

## Arquivos

Quais arquivos serão alterados.

## Validação

Como conferir se deu certo.

---

# Regra de Escopo

Toda tarefa deve ter escopo pequeno.

Errado:

```text
Recrie a Home inteira.
```

Certo:

```text
Corrija apenas o espaçamento da Sidebar e o foco dos cards na Home.
```

---

# Regra contra refatoração invisível

A IA não deve refatorar código não relacionado.

Alterações grandes exigem autorização.

---

# Regra contra gosto pessoal

A IA não deve dizer:

- "ficaria melhor se..."
- "uma experiência mais moderna seria..."
- "vou melhorar o design..."

O design já existe.

A tarefa é preservar.

---

# Frase de controle

Sempre que uma IA começar a inventar, usar esta frase no prompt:

```text
Pare. O Web Player é a fonte de verdade. Não redesenhe. Compare e porte.
```



---

<!-- FILE: design-system/12-Porting-Guide-Web-to-Android.md -->


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



---

<!-- FILE: design-system/13-Claude-Code.md -->


# Claude Code Guide

Versão 1.0

---

# Papel do Claude Code

Claude Code deve atuar como engenheiro de implementação.

Ele deve preservar a identidade do IZ Play e corrigir o código com escopo controlado.

---

# Ordem de leitura obrigatória

Antes de alterar UI:

1. design-system/01-Principles.md
2. design-system/02-Colors.md
3. design-system/03-Typography.md
4. design-system/04-Spacing.md
5. design-system/05-Layout.md
6. design-system/06-Components.md
7. design-system/07-Behavior-System.md
8. design-system/08-Focus-System.md
9. design-system/09-Navigation-System.md
10. design-system/10-Player-Behavior.md
11. design-system/11-AI-Guide.md
12. design-system/12-Porting-Guide-Web-to-Android.md

---

# Prompt base para Claude Code

```text
Você está trabalhando no projeto IZ Play.
O Web Player é a fonte oficial de UI.
Não redesenhe.
Não aplique Material Design visual.
Não mude identidade, cores, espaçamentos, ordem das seções ou componentes.
Compare a implementação atual com o Web Player e com o Design System.
Liste as diferenças antes de alterar.
Depois corrija apenas o escopo solicitado.
```

---

# Modo correto de trabalho

Claude deve:

- investigar antes de editar
- explicar o diagnóstico
- alterar poucos arquivos
- evitar refatoração ampla
- validar com build/lint quando possível
- informar limitações

---

# Quando pedir confirmação

Pedir confirmação antes de:

- alterar arquitetura
- mover arquivos
- apagar componentes
- trocar biblioteca
- alterar rotas
- alterar modelo de dados
- refatorar tela inteira
- mudar comportamento global

---

# Quando não pedir confirmação

Não precisa pedir confirmação para:

- corrigir erro simples
- ajustar import quebrado
- trocar valor visual para bater com token
- corrigir foco quebrado
- resolver warning pequeno
- aplicar regra explícita da documentação

---

# Regra Final

Claude Code não é diretor de arte do IZ Play.

Claude Code é executor técnico da identidade já definida.



---

<!-- FILE: design-system/14-Codex.md -->


# Codex Guide

Versão 1.0

---

# Papel do Codex

Codex deve ser usado para implementação objetiva, correção de bugs, portabilidade e validação.

Ele deve evitar criatividade visual.

---

# Antes de programar

Codex deve localizar:

- tela afetada
- componente afetado
- documentação correspondente
- implementação do Web Player
- implementação atual do Android/Desktop

---

# Regra para commits

Cada commit deve ter escopo claro.

Exemplos corretos:

- fix(android): align sidebar width with web player
- fix(android): restore d-pad focus on home rows
- docs(design): add player behavior guide

Exemplos errados:

- update app
- improve UI
- refactor everything
- new layout

---

# Regra para patches

Patch deve ser pequeno.

Não modificar arquivos não relacionados.

Não misturar na mesma alteração:

- layout
- comportamento
- backend
- performance
- refatoração

---

# Checklist antes de finalizar

- Build passa?
- Tela continua igual ao Web Player?
- D-Pad funciona?
- BACK funciona?
- Foco visível?
- Sidebar não mudou de posição?
- Cores usam tokens?
- Não há Material Design visual indevido?
- Nenhuma tela foi reordenada?

---

# Prompt base para Codex

```text
Trabalhe no projeto IZ Play.
A tarefa é técnica e com escopo limitado.
O Web Player é a fonte de verdade.
Não redesenhe a interface.
Não refatore fora do escopo.
Compare antes de alterar.
Corrija apenas o problema solicitado.
Depois explique os arquivos alterados e como testar.
```

---

# Regra Final

Codex deve entregar código previsível, pequeno e validável.



---

<!-- FILE: design-system/15-Review-Checklist.md -->


# Review Checklist

Versão 1.0

---

# Objetivo

Checklist obrigatório para revisar alterações feitas por IA ou desenvolvedor.

---

# Visual

- A tela parece o Web Player?
- Sidebar está à esquerda?
- Cores oficiais foram preservadas?
- Tipografia foi preservada?
- Espaçamentos estão consistentes?
- Radius dos cards está correto?
- Hero está na posição correta?
- Ordem das seções foi preservada?

---

# Android TV

- D-Pad funciona em todas as direções?
- OK executa o item focado?
- BACK segue a ordem correta?
- Foco é claramente visível?
- Foco não desaparece?
- Foco retorna ao item anterior?
- Sidebar abre e fecha corretamente?
- Player funciona sem mouse?

---

# Player

- Controles aparecem com OK/mouse?
- Controles somem após inatividade?
- Fullscreen entra corretamente?
- BACK sai do fullscreen antes de sair da tela?
- Reconectar funciona?
- Erro de stream não trava a UI?

---

# Performance

- Scroll está suave?
- Imagens carregam sob demanda?
- Listas grandes não travam?
- Player não bloqueia UI?
- Não houve renderização desnecessária?

---

# Código

- Escopo foi respeitado?
- Arquivos não relacionados ficaram intactos?
- Componentes foram reutilizados?
- Tokens foram usados?
- Não existem valores mágicos desnecessários?
- Build/lint passa?

---

# Segurança

- Nenhuma chave foi exposta?
- Nenhum endpoint sensível foi logado?
- Nenhum token foi commitado?
- Logs não mostram dados de usuário?

---

# Documentação

- Mudança relevante foi documentada?
- Novo componente entrou no Design System?
- Novo comportamento entrou no Behavior System?
- Nova regra foi adicionada quando necessário?

---

# Aprovação

Uma alteração só pode ser considerada pronta quando:

- visual está fiel
- navegação funciona
- build passa
- escopo foi respeitado
- nenhum comportamento global foi quebrado



---

<!-- FILE: design-system/16-Prompt-Templates.md -->


# Prompt Templates

Versão 1.0

---

# Prompt para corrigir layout Android

```text
Você está no projeto IZ Play.

O Web Player é a fonte de verdade visual.
Android é apenas um port em Jetpack Compose.

Tarefa:
Corrigir [TELA/COMPONENTE].

Regras:
- Não redesenhar.
- Não usar Material Design como referência visual.
- Não alterar cores oficiais.
- Não alterar ordem das seções.
- Não refatorar arquivos fora do escopo.
- Antes de editar, liste diferenças entre Android e Web Player.
- Depois corrija apenas as diferenças relacionadas à tarefa.
- Preserve navegação por D-Pad.

Ao final, informe:
- arquivos alterados
- mudanças feitas
- como testar
- riscos
```

---

# Prompt para corrigir foco Android TV

```text
Você está no projeto IZ Play.

Corrija apenas o sistema de foco em [TELA].

Regras:
- Todo item interativo deve ter foco visível.
- O foco deve usar vermelho IZ.
- D-Pad deve seguir ordem visual.
- BACK deve seguir o Behavior System.
- Não alterar layout.
- Não alterar backend.
- Não refatorar tela inteira.
```

---

# Prompt para portar tela do Web para Compose

```text
Você está no projeto IZ Play.

Portar a tela [NOME] do Web Player para Android Compose.

Processo:
1. Analise HTML/CSS da tela no Web Player.
2. Liste estrutura visual.
3. Mapeie para Compose.
4. Crie ou ajuste componentes existentes.
5. Aplique tokens do Design System.
6. Aplique foco e D-Pad.
7. Não invente layout.
```

---

# Prompt para revisar alteração feita por IA

```text
Revise esta alteração no projeto IZ Play.

Verifique:
- fidelidade ao Web Player
- respeito ao Design System
- comportamento de D-Pad
- uso correto de tokens
- escopo
- possíveis regressões

Não proponha redesign.
Aponte apenas problemas objetivos.
```

---

# Prompt de emergência quando IA começa a inventar

```text
Pare.

O Web Player é a fonte de verdade.

Não redesenhe.
Não modernize.
Não simplifique por conta própria.
Não aplique Material Design visual.

Compare com o Web Player e corrija apenas o que foi pedido.
```

---

# Prompt para documentação

```text
Crie ou atualize documentação do IZ Play.

Regras:
- Documentar estado real do projeto.
- Não inventar recurso inexistente.
- Separar recurso planejado de recurso implementado.
- Usar linguagem objetiva.
- Manter hierarquia Web -> Desktop -> Android.
```



---

<!-- FILE: .ai/AI_GUIDE.md -->


# IZ Play AI Guide

Leia sempre:

1. design-system/11-AI-Guide.md
2. design-system/12-Porting-Guide-Web-to-Android.md
3. design-system/15-Review-Checklist.md
4. design-system/16-Prompt-Templates.md

Regra máxima:

O Web Player é a fonte de verdade.

Não redesenhar.

Portar.

Corrigir.

Validar.



---

<!-- FILE: .ai/CLAUDE.md -->


# Claude Instructions

Antes de alterar o IZ Play, leia:

- design-system/01-Principles.md
- design-system/06-Components.md
- design-system/07-Behavior-System.md
- design-system/11-AI-Guide.md
- design-system/12-Porting-Guide-Web-to-Android.md

Regra máxima:

**Não redesenhe. O Web Player é a fonte de verdade.**



---

<!-- FILE: .ai/CODEX.md -->


# Codex Instructions

Trabalhe com patches pequenos.

Não refatore fora do escopo.

Não redesenhe.

Compare com o Web Player antes de alterar UI.

Use o Review Checklist antes de finalizar.
