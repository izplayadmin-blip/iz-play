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
