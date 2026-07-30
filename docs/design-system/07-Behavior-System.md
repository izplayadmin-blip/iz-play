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
