# Fase 3 — Feedback e descoberta dentro do jogo

**Versão alvo:** 0.3.0. **Status:** implementada; aceite de cliente/Ponder pendente. **Pré-requisitos:** fases 1 e 2.

## Objetivo e problema

Explicar tentativas recusadas e ensinar as funções existentes. A implementação acrescenta aviso de material recusado e uma cena de Ponder para descobrir troca com wrench e material, bypass com Sneak e ação conectada.

## Tarefas: material recusado

- [x] Mostrar mensagem curta na actionbar para material recusado no clique simples e na ação em grupo sem alterações por esse motivo.
- [x] Distinguir material inválido de resultado zero por mesmo material, falta de estoque ou permissão; não tratar todo resultado zero como material inválido.
- [x] Adicionar chaves em `common/src/main/resources/assets/copycat_replace/lang/en_us.json` e manter as traduções que existirem.
- [x] Preservar cancelamento do clique e bloqueio da wrench, sem mensagem duplicada por mão ou lado lógico.

## Tarefas: Ponder

- [x] Conferir APIs locais do Ponder embarcado no Create: base analisada com 1.0.82 no NeoForge e 1.0.91 no Forge.
- [x] Registrar uma cena ligada aos Copycats do Create.
- [x] Demonstrar troca simples nas duas ordens de mãos, Sneak e tecla de ação conectada.
- [x] Demonstrar aplicar, substituir e remover em grupo, respeitando permissões e limites.
- [ ] Verificar carregamento e acesso com e sem Copycats+.

## Pontos de alteração

`interaction/CopycatReplaceHandler.java`, retornos dos serviços/adaptadores quando necessário e traduções compartilhadas. Definir registro, cenas e recursos de Ponder na implementação, respeitando APIs e carregamento de cliente de cada módulo.

## Aceite

- [x] Material inválido simples/conectado apresenta feedback correto sem mudar mundo ou inventário.
- [x] Nenhuma rotação, desmontagem ou remoção normal da wrench ocorre por causa da recusa.
- [x] Permissão negada não aparece como material inválido.
- [ ] Cena acessível pressionando W sobre um Copycat do Create nos dois loaders, com e sem Copycats+.
- [ ] Textos e demonstrações correspondem aos controles reais, incluindo tecla remapeada quando a API permitir exibi-la.

Implementação e testes preparados nos dois loaders. As verificações de cliente, acesso por W, reprodução/renderização da cena e execução sem Copycats+ não foram concluídas nesta rodada por falta de memória livre. Os detalhes e comandos estão na [validação atual](../validacao.md#fases-3-e-4--feedback-ponder-e-aceite).

[Anterior: configuração SERVER](fase-02-configuracao-server.md) · [Índice](README.md) · [Próxima: validação](fase-04-validacao.md)
