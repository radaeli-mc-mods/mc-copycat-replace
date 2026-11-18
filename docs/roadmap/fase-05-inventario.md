# Fase 5 — Usar material do inventário

**Versão alvo:** mesma entrega das fases 1 a 4. **Status:** implementada; aceite prático pendente. **Pré-requisito:** fase 4.

## Objetivo e problema

Permitir ações conectadas maiores no Survival. A execução continua após a pilha da mão usando itens idênticos do inventário principal, e o preview considera esse estoque.

## Tarefas

- [x] Definir item idêntico para cada versão, considerando dados/componentes relevantes, e slots elegíveis do inventário principal.
- [x] Preservar wrench e material nas mãos como início da ação; consumir primeiro a pilha escolhida e continuar nas elegíveis.
- [x] Adaptar o fluxo para não interromper o grupo apenas porque a pilha inicial esvaziou.
- [x] Atualizar preview para o estoque total elegível, limitado pela configuração do servidor.
- [x] Preservar devoluções e transferência de itens pagos multipartes, sem contar inadvertidamente devoluções como estoque novo.
- [x] Manter execução parcial por estoque insuficiente e Creative sem consumo.

## Decisão implementada

Sempre ativo para ações em grupo, sem nova configuração. Identidade: item + NBT no Forge; item + componentes no NeoForge. São elegíveis os 36 slots principais, mão escolhida primeiro. Armadura e outra mão ficam fora. `MaterialStock` compartilha essas regras entre preview e execução; seu orçamento inicial por pilha impede contar devoluções como estoque novo.

## Pontos de alteração

`service/BulkMaterialReplacement.java`, consumo em `service/CreateCopycatAdapter.java` e `compat/copycats/CopycatsPlusCompat.java`, preview e eventual configuração. Compartilhar as regras de elegibilidade entre estimativa e execução dentro de cada módulo.

## Aceite

- [x] Grupo maior que uma pilha usa o inventário nas duas ordens de mãos, sem consumir itens com dados incompatíveis.
- [x] Estoque insuficiente altera somente o que pode ser pago; estoque exato não perde nem duplica itens.
- [x] Inventário cheio, devoluções e multipartes conservam a contabilidade; Creative não consome.
- [x] Proteções continuam impedindo mudanças e consumo em posições recusadas.
- [x] Preview acompanha estoque e configuração, mantendo a ressalva sobre permissões desconhecidas.
- [x] GameTests e cliente automatizado passam nos dois loaders, com e sem Copycats+. Aceite prático completo permanece na fase 4.

[Anterior: validação 0.3.0](fase-04-validacao.md) · [Índice](README.md) · [Próxima: seleção](fase-06-selecao.md)

As evidências automatizadas desta rodada e os limites de aceite estão em [Build e validação](../validacao.md#fases-4-5-e-6--inventário-e-seleção). Sem bump, tag, publicação ou commit nesta tarefa.
