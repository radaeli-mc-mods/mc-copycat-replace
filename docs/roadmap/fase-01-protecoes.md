# Fase 1 — Respeitar proteções de área

**Versão alvo:** 0.3.0. **Status:** implementada; aceite prático pendente. **Pré-requisito:** base 0.2.0.

## Objetivo e problema

Impedir que aplicar, substituir ou remover materiais em grupo altere posições sem permissão. Na base anterior a esta fase, o handler verificava `player.mayBuild()`, mas `BulkMaterialReplacement#findCluster` não recebia o jogador nem consultava `level.mayInteract(player, pos)` por vizinho. A busca também não disparava eventos de permissão por posição. Isso criava risco de alterações em claims de terceiros, incluindo devolução indevida de itens na remoção; a compatibilidade concreta deve ser validada com mods de claims.

## Tarefas

- [x] Definir uma verificação no servidor por jogador, posição e operação, nos dois loaders.
- [x] Excluir posições em que `level.mayInteract(player, pos)` seja falso ou a alteração seja vetada pelo mecanismo de permissão escolhido.
- [x] Impedir que posições bloqueadas entrem no grupo ou expandam a busca para seus vizinhos por aquele caminho.
- [x] Verificar também a origem; posições recusadas não alteram mundo ou inventário e não devolvem itens.
- [x] Preservar seis faces, chunks já carregados, filtros de material/tipo e limite por posição.
- [x] Manter o preview como estimativa, salvo decisão de consultar o servidor.

## Decisões adotadas

- Usar `BlockEvent.EntityPlaceEvent` para aplicar e `BlockEvent.BreakEvent` para remover; substituir exige os dois. Os eventos consultam veto sobre o Copycat existente antes da mutação, sem colocar/quebrar o bloco nem executar drops/XP pelo addon. A compatibilidade prática de cada mod de claims continua pendente.
- O teste de 256 substituições mede 512 eventos, um de cada tipo por posição elegível. É uma medição sintética, sem custo de um mod de claims real.
- Manter o preview como estimativa local, sem disparar eventos de permissão. A autorização permanece no servidor.

## Pontos de alteração

`interaction/CopycatReplaceHandler.java`, `service/BulkMaterialReplacement.java` e, conforme a solução, uma camada de permissão por loader. Estender cenários em `common/src/gameTest`.

## Aceite

- [x] GameTests com bloqueio na origem, no meio e na borda, para aplicar, substituir e remover.
- [x] Um bloqueio intermediário impede a propagação por esse caminho; caminhos alternativos continuam sujeitos às mesmas verificações.
- [x] Materiais e inventário permanecem intactos para posições recusadas, sem devolução indevida.
- [ ] Validação manual com FTB Chunks ou Open Parties and Claims compatível com cada loader, registrando as combinações testadas.
- [x] Custo de 256 posições avaliado e diferença entre preview e resultado registrada internamente. Ver [evidências das fases 1 e 2](../validacao.md#fases-1-e-2--permissões-e-configuração-server).

[Índice](README.md) · [Próxima: configuração SERVER](fase-02-configuracao-server.md)
