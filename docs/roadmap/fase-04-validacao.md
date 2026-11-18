# Fase 4 — Fechar a validação de 0.3.0

**Versão alvo:** 0.3.0. **Status:** parcial; aceite prático pendente. **Pré-requisitos:** fases 1 a 3.

## Objetivo

Concluir a [matriz prática restante](../validacao.md#matriz-prática-restante) e validar as entregas de servidores e descoberta. Resultados parciais de 0.2.0 continuam como evidência histórica, sem serem reclassificados como aprovação de 0.3.0.

## Tarefas

- [x] Executar build e verificação de artefatos pelos [comandos existentes](../validacao.md#comandos).
- [x] Executar GameTests com e sem Copycats+, incluindo permissão e configuração.
- [ ] Concluir multiplayer Forge com dois clientes: recarga, multipartes, estado por jogador e reconexão.
- [ ] Concluir multiplayer NeoForge sem Copycats+ e repetir regressões afetadas com o addon.
- [ ] Validar instalações normais com JARs finais dos dois loaders, com atenção ao NeoForge ainda não repetido na etapa anterior.
- [ ] Inspecionar HUD, contornos, transições, mensagens e Ponder; avaliar os sons por escuta.
- [ ] Atualizar `docs/validacao.md` com versões, combinações, resultados e evidências, distinguindo testes automatizados e aceite manual.

## Matriz de aceite

Executar nos dois loaders, com e sem Copycats+ quando aplicável:

| Área | Cenários necessários |
| --- | --- |
| Simples | Duas ordens de mãos, Sneak, material inválido/igual e bloqueio da wrench |
| Grupo | Aplicar/substituir/remover; seis faces; diagonais excluídas; tipos/materiais distintos; limites 1/64/256 |
| Permissões | Origem/meio/borda bloqueados; barreira de propagação; claims reais |
| Inventário | Survival/Creative, estoque parcial, inventário cheio e devoluções sem perda/duplicação |
| Multipartes | Materiais iguais/diferentes, parte atingida e transferência do item pago |
| Cliente | Remapeamento, telas, troca de alvo/material, movimento, soltura, feedback e Ponder |
| Servidor | Dois jogadores, atualização remota, isolamento de estado, recarga, reconexão e reinício |
| Distribuição | JARs finais em instalação normal, reobfuscação Forge e ausência de Copycats+ |

## Critério de conclusão

- [x] Recarga Forge resolvida com evidência reproduzível; etapas não executadas continuam identificadas como pendentes.
- [ ] Matriz prática concluída e evidências registradas.
- [x] Arquitetura e documentação de uso/configuração descrevem comportamento implementado e verificado, com paridade en/pt-BR nas páginas públicas pertinentes.

Preparar ou publicar a versão é uma ação separada, fora desta reorganização de documentos.

A rodada de 2026-11-02 passou nos GameTests finais das quatro combinações: 89 com Copycats+ e 81 sem em cada loader (340 execuções). Inclui inventário e seleção das fases 5 e 6, antecipadas para esta mesma entrega. Evidências, cliente e limites estão em [Build e validação](../validacao.md#fases-4-5-e-6--inventário-e-seleção). Multiplayer real, claims, instalações normais e inspeção visual/auditiva continuam pendentes. Esta fase não está concluída.

[Anterior: feedback e Ponder](fase-03-feedback-e-ponder.md) · [Índice](README.md) · [Próxima: inventário](fase-05-inventario.md)
