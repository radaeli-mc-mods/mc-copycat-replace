# Fase 7 — Desfazer a última ação em grupo

**Versão alvo:** 0.4.0. **Status:** planejada. **Pré-requisitos:** fases 5 e 6.

## Objetivo

Desfazer uma única ação em grupo por jogador. O servidor guarda estado anterior, materiais, itens pagos e movimentação de inventário. Uma tecla pede a reversão, permitida somente se nenhuma posição afetada mudou desde a ação. Sem histórico; o registro é descartado ao sair do servidor.

## Decisões antes da implementação

- Definir estado comparado para detectar alterações, incluindo partes e itens pagos, não apenas `BlockState`.
- Definir compensação após itens serem movidos, gastos ou soltos. Se a reversão não puder ocorrer sem perda/duplicação, recusar com feedback e sem alteração parcial. Não sobrescrever o inventário atual com um snapshot antigo.
- Resolver itens devolvidos como entidades por falta de espaço, ações parciais e Creative.
- Definir tratamento de chunks descarregados, troca de dimensão, morte e permissões revogadas, mantendo uma única ação em memória.

## Tarefas

- [ ] Registrar por jogador somente posições efetivamente alteradas, estado anterior, resultado esperado e movimentações de itens da fase 5.
- [ ] Incluir materiais multipartes e itens pagos na captura e restauração.
- [ ] Substituir registro por nova ação elegível; definir se tentativa sem mudanças mantém o registro anterior.
- [ ] Criar tecla remapeável, pedido ao servidor e feedback de sucesso/recusa.
- [ ] Validar todas as posições, permissões e compensações antes de reverter.
- [ ] Descartar registro ao desconectar e após desfazer; impedir repetição do pedido e acesso ao registro de outro jogador.

## Pontos de alteração

Orquestração em `service/BulkMaterialReplacement.java`, captura/restauração nos adaptadores, estado por jogador no servidor, rede e teclas. Definir novas classes após resolver a contabilidade, preservando o namespace `com.radaeli`.

## Aceite

- [ ] Aplicar/substituir/remover podem ser desfeitos uma vez, inclusive após execução parcial.
- [ ] Posição modificada, permissão revogada ou compensação inviável recusa a operação inteira, sem reversão parcial.
- [ ] Survival, Creative, inventário movimentado/cheio e multipartes não perdem nem duplicam itens.
- [ ] Registros independentes por jogador; reconexão não recupera ação antiga e pacotes repetidos não repetem devoluções.
- [ ] Três modos de seleção e consumo do inventário continuam corretos após reverter.
- [ ] GameTests, cliente e multiplayer pertinente passam nos dois loaders, com e sem Copycats+.
- [ ] Repetir a matriz de regressão da [fase 4](fase-04-validacao.md) para o conjunto de 0.4.0 e atualizar documentação do comportamento implementado e verificado.

[Anterior: seleção](fase-06-selecao.md) · [Índice](README.md) · [Acabamento opcional](acabamento.md)
