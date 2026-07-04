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
