# Fase 2 — Mover o limite para configuração SERVER

**Versão alvo:** 0.3.0. **Status:** implementada; aceite multiplayer pendente. **Sequência:** após a fase 1; sem dependência técnica direta dela.

## Objetivo e problema

Tratar `max_connected_blocks` como regra do mundo. Na base anterior a esta fase, a configuração era `COMMON`, usava `config/copycat_replace-common.toml` e possuía sincronização própria por `ConnectedBlockLimitPayload` e `ConnectedBlockLimitSync`. A recarga em execução não foi observada no Forge, conforme a [pendência registrada](../validacao.md#pendência-conhecida-recarga-no-forge).

## Tarefas

- [x] Registrar `ModConfig.Type.SERVER` nos dois loaders, com arquivo por mundo no Forge; no NeoForge 21.1.236, base em `config/` e override existente em `<mundo>/serverconfig/copycat_replace-server.toml`, conforme o comportamento nativo.
- [x] Preservar padrão 64 e intervalo inclusivo de 1 a 256.
- [x] Conferir as APIs de sincronização do loader e manter o envio do limite no login e na recarga; o aceite com clientes e troca de mundo continua listado abaixo.
- [x] Remover payload e sync próprios somente se a sincronização do loader atender ao preview e às atualizações durante a execução; caso contrário, manter o necessário e registrar o motivo.
- [x] Implementar a política escolhida para instalações que já usam COMMON.
- [x] Atualizar testes de limite, recarga e multiplayer para o novo ciclo de vida.

## Decisões adotadas

- Importação automática do COMMON ao criar a configuração SERVER: modelo ausente no Forge e base ausente no NeoForge. Preservar arquivos e modelos existentes e o COMMON original.
- Manter o pacote próprio: o sync nativo cobre a conexão, não a atualização do HUD durante a execução. O Forge ganha verificação do arquivo ativo a cada 20 ticks com duas leituras estáveis, pois o NightConfig 3.6.4 descarta eventos de gravação agrupados. O NeoForge usa seu monitor nativo. Ver [evidências](../validacao.md#fases-1-e-2--permissões-e-configuração-server) para separar teste de servidor de aceite multiplayer.

## Pontos de alteração e documentação

`CopycatReplace.java`, `config/CopycatReplaceConfig.java`, classes de limite em `network/`, leitura pelo preview e cenários multiplayer. Após implementar, atualizar README, [arquitetura](../arquitetura.md), [validação](../validacao.md) e a referência pública de configurações em ambos os idiomas, com padrão completo, schema, localização e recarga verificados.

## Aceite

- [ ] Mundos distintos mantêm limites distintos, sem valor de outro mundo no preview.
- [ ] Limites 1, 64 e 256 respeitados no servidor e refletidos no cliente.
- [ ] Edição durante a execução atualiza regra e preview nos dois loaders; conferir também reconexão e reinício.
- [x] Importação automática testada com COMMON ausente, padrão, personalizado, inválido e destino já existente.
- [ ] Multiplayer Forge passa pela recarga e prossegue até multipartes e reconexão, com e sem Copycats+.

[Anterior: proteções](fase-01-protecoes.md) · [Índice](README.md) · [Próxima: feedback e Ponder](fase-03-feedback-e-ponder.md)
