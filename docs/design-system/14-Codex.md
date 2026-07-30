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
