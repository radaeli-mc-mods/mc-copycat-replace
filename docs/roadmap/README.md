# Implementação por fases — Create: Copycat Replace!

> Planejamento interno. Não publicar nem adaptar estas páginas no `mod-docs` ou no README público. As fases 1 e 2 estão implementadas, com aceite prático ainda pendente. A fase 3 está implementada com aceite de cliente pendente; a fase 4 tem validação parcial. As fases 5 e 6 estão implementadas na mesma entrega das fases 1 a 4; a fase 7 continua planejada; implementação e validação não são equivalentes.

Base: **0.2.0**, com NeoForge 1.21.1 / Create 6.0.10 e Forge 1.20.1 / Create 6.0.8. Cada fase atende aos dois loaders, com e sem Copycats+ quando aplicável.

O estado detalhado de tarefas, testes e pendências está nas páginas de cada fase e nas [evidências de validação](../validacao.md#fases-1-e-2--permissões-e-configuração-server).

## Ordem de implementação

| Fase | Versão alvo | Entrega | Depende de | Status |
| --- | --- | --- | --- | --- |
| [1 — Proteções de área](fase-01-protecoes.md) | 0.3.0 | Permissão por posição e barreiras na propagação | Base 0.2.0 | Implementada; aceite prático pendente |
| [2 — Configuração do servidor](fase-02-configuracao-server.md) | 0.3.0 | Limite por mundo, sincronização e migração definida | Fase 1 como sequência de trabalho | Implementada; aceite multiplayer pendente |
| [3 — Feedback e descoberta](fase-03-feedback-e-ponder.md) | 0.3.0 | Aviso de material recusado e cena de Ponder | Fases 1 e 2 | Implementada; aceite de cliente pendente |
| [4 — Validação de 0.3.0](fase-04-validacao.md) | 0.3.0 | Matriz prática, multiplayer e instalação normal | Fases 1 a 3 | Parcial; aceite prático pendente |
| [5 — Material do inventário](fase-05-inventario.md) | Mesma entrega das fases 1–4 | Consumo de várias pilhas e preview do estoque | Fase 4 | Implementada; aceite prático pendente |
| [6 — Modos de seleção](fase-06-selecao.md) | Mesma entrega das fases 1–4 | Conectado, Plano e Todos os tipos | Fase 5 | Implementada; aceite prático pendente |
| [7 — Desfazer ação em grupo](fase-07-desfazer.md) | 0.4.0 | Uma ação reversível por jogador, sem histórico | Fases 5 e 6 | Planejada |

As dependências definem a sequência recomendada de entrega. A migração de configuração não depende tecnicamente da proteção de claims, mas deve estar pronta antes do aceite de 0.3.0. O [acabamento](acabamento.md) pode acompanhar qualquer fase, com aceite próprio.

## Como executar e concluir uma fase

1. Resolver as decisões em aberto antes de implementar o comportamento afetado.
2. Implementar nos dois módulos, respeitando as diferenças de API. Os caminhos Java citados nas fases são relativos a `com/radaeli/copycatreplace/` em cada módulo; testes e traduções compartilhados ficam em `common/`.
3. Executar as verificações pertinentes de [build e validação](../validacao.md). Registrar evidências reais e distinguir build, GameTests, cliente, multiplayer e inspeção manual.
4. Marcar tarefas somente após concluí-las e registrar pendências na página da fase. Atualizar o status na página e nesta tabela ao iniciar ou concluir a fase.
5. Atualizar a documentação afetada do projeto e as páginas públicas pertinentes em inglês e pt-BR somente com comportamento implementado e verificado, seguindo `mod-docs-sync`. A criação inicial das páginas do mod deve cumprir os requisitos de catálogo, navegação e configurações da skill.

A fase 4 fecha o aceite de 0.3.0. As fases 5 e 6 foram antecipadas para a mesma entrega das fases 1 a 4, a pedido do usuário; a fase 7 fica fora desta implementação. Repetir a matriz de regressão após o recurso de desfazer. Este plano não inclui bump, commit, tag, push ou publicação.

## Limites preservados

- Sem port para Fabric nesta sequência.
- Sem novos blocos ou itens próprios.
- O preview local continua uma estimativa quando desconhece permissões do servidor.
- O [plano inicial NeoForge](../../PLANO_IMPLEMENTACAO.md) permanece histórico; a [arquitetura atual](../arquitetura.md) e as [evidências existentes](../validacao.md) continuam como referências da base.

[Índice técnico](../README.md) · [Roadmap na raiz](../../ROADMAP.md)
