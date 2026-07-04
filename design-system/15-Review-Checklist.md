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
