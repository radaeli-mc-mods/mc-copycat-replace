# Fase 6 — Adicionar modos de seleção

**Versão alvo:** mesma entrega das fases 1 a 4. **Status:** implementada; aceite prático pendente. **Pré-requisito:** fase 5.

## Objetivo

Com a tecla de ação conectada pressionada, rolar o mouse alterna o modo, apresentado no HUD.

| Modo | Seleção implementada |
| --- | --- |
| Conectado | Seis faces, mesmo tipo de Copycat |
| Plano | Mesmo tipo; plano pela origem perpendicular à face clicada |
| Todos os tipos | Seis faces, qualquer tipo de Copycat suportado |

## Tarefas e decisões

- [x] Definir Plano como plano pela origem perpendicular à normal da face clicada e decidir se mantém o filtro de mesmo tipo.
- [x] Preservar material elegível, permissões, chunks carregados e limite por posição em todos os modos.
- [x] Alternar pela roda somente com tecla ativa, sem interferência indevida em telas ou troca de slot.
- [x] Guardar modo no cliente e enviá-lo com o estado da tecla por `BulkReplaceKeyStatePayload`.
- [x] Validar modo recebido e manter estado por jogador no servidor; definir reset ao sair/reconectar.
- [x] Usar a mesma seleção no preview e na execução, com autorização no servidor.
- [x] Definir seleção de partes ao atravessar formatos distintos de Copycats+.
- [x] Atualizar HUD, traduções e demonstrações afetadas depois da implementação.

## Pontos de alteração

`client/CopycatReplaceClientKeys.java`, `client/ConnectedBulkIndicator.java`, `network/BulkReplaceKeyStatePayload.java`, `network/BulkReplaceMode.java` e `service/BulkMaterialReplacement.java`. `SelectionMode` separa a seleção do estado momentâneo da tecla. O servidor guarda ambos por UUID; IDs inválidos viram Conectado. O cliente lembra o modo na conexão e o reinicia ao desconectar; soltar a tecla e abrir telas desativam o estado servidor.

## Aceite

- [x] Conectado preserva o comportamento anterior; Plano exclui posições fora do plano para todas as faces.
- [x] Todos os tipos atravessa tipos suportados, preservando filtro de material e exclusão de blocos não suportados.
- [x] HUD e execução usam o mesmo modo; pacotes inválidos não ampliam limites ou burlam permissões.
- [ ] Jogadores usam modos independentes; soltar a tecla, abrir telas e reconectar não deixa estado indevido ativo.
- [ ] Aceite completo dos três modos com aplicar/substituir/remover, estoque parcial e multipartes nos dois loaders. Os GameTests já cobrem geometria/ações/multipartes por modo; estoque parcial também tem cenários dedicados. O aceite de cliente continua separado.

[Anterior: inventário](fase-05-inventario.md) · [Índice](README.md) · [Próxima: desfazer](fase-07-desfazer.md)

## Decisões implementadas

Plano mantém o filtro de mesmo tipo. Ao aplicar entre formatos distintos, a seleção da parte usa a mesma coordenada relativa e face do clique em cada posição. Substituir/remover segue todas as partes existentes com o material de origem, preservando as demais. A roda só é consumida com tecla ativa e sem tela aberta.

As evidências automatizadas desta rodada e os limites de aceite estão em [Build e validação](../validacao.md#fases-4-5-e-6--inventário-e-seleção). Sem bump, tag, publicação ou commit nesta tarefa.
