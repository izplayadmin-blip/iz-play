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
