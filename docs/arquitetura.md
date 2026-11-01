# Arquitetura multiloader

## Módulos

`neoforge` mantém Minecraft 1.21.1, NeoForge 21.1.236, Java 21 e Create 6.0.10. `forge1201` usa Minecraft 1.20.1, Forge 47.4.0, Java 17 e Create 6.0.8. O Gradle 8.11.1 roda em Java 21, com toolchains específicas. Os plugins são ModDevGradle 2.0.144 e ForgeGradle 6.0.54.

Cada módulo possui Java e metadados próprios. Traduções ficam em `common/src/main/resources`. As interfaces internas de alvo e os comportamentos são equivalentes, sem uma camada pública de abstração entre versões do Minecraft. Os cenários de aceitação em `common/src/gameTest` compilam contra os dois loaders; somente os pontos de entrada dos testes dependem do loader.

## Dependências locais

O Create Forge completo foi copiado de `hotbar-locker/build/reference/create/create-1.20.1-6.0.8.jar`. SHA-256 da origem e da cópia:

```text
6fbb910c367dbce8e4fc7e5bf64b6edd4de980906ed00af8e47e4af843c0d9b0
```

O runtime utiliza os JARs embarcados pelo Create: Ponder 1.0.91, Flywheel 1.0.5, Registrate MC1.20-1.3.3 e MixinExtras 0.4.1. APIs adicionais necessárias ao compilador Forge são `compileOnly`; não são reempacotadas no addon. As dependências Forge passam por `fg.deobf`. As runs de desenvolvimento também remapeiam os refmaps de dependências.

Copycats+ Forge 3.0.4 foi obtido da versão Modrinth `OjWz9bJQ`; o SHA-512 foi conferido com os metadados do download. A auditoria com `javap` confirmou as interfaces de materiais simples e multipartes, seleção por interação, armazenamento e setters usados pelo adaptador. A ausência de Copycats+ é verificada antes de chamar a classe de integração.

## Fluxo das ações

O evento de interação escolhe as mãos e valida o alvo. As mudanças de mundo e inventário acontecem no servidor. A busca usa seis faces e posições com o mesmo tipo de Copycat/material elegível, respeita `hasChunkAt` e o limite configurado. O limite inclui a origem e conta posições, mesmo quando uma posição possui várias partes.

O armazenamento de materiais multipartes conserva um item pago por material compartilhado: ao trocar uma parte, o item anterior é transferido para outra parte que ainda use esse material; a devolução só acontece quando deixa de ser necessário. Alterar a orientação do mesmo material conserva o item pago.

`MaterialEditPermission` verifica `mayBuild`, chunk carregado e `level.mayInteract` por posição no servidor. Antes de incluir uma posição elegível na busca, consulta `EntityPlaceEvent` para aplicar, `BreakEvent` para remover e ambos para substituir. A troca individual usa a mesma verificação. Os eventos são consultas de veto sobre o Copycat existente: nenhum bloco é colocado ou quebrado para consultar a permissão; efeitos de drops e XP dos eventos não são executados pelo addon. A posição recusada não entra no grupo nem expande a busca por aquele caminho. Cada candidato elegível recebe no máximo um evento de cada tipo por busca. No cenário de 256 posições permitidas, substituir consulta 512 eventos; candidatos recusados na fronteira também são consultados, sem expandir a busca por eles. Isso permite que proteções inscritas nesses eventos vetem a alteração; não constitui garantia de compatibilidade com todo mod de claims. O preview não dispara eventos nem consulta claims.

No Forge, `SimpleChannel` registra mensagens direcionais de estado da tecla e limite do servidor, com processamento na thread principal. No NeoForge permanecem os payloads nativos. A configuração agora é `SERVER`, com padrão 64 e intervalo inclusivo 1–256. O Forge usa `<mundo>/serverconfig/copycat_replace-server.toml`; o NeoForge 21.1.236 usa `config/copycat_replace-server.toml` como base e prefere um override existente no `serverconfig` do mundo. A sincronização nativa dos loaders envia a configuração na conexão; o pacote próprio de limite permanece para atualizar o preview durante a execução no evento `Reloading`, além do login. Não são usados os valores locais do cliente para autorizar ações. A tecla padrão continua Left Alt.

No Forge, `config/ServerConfigReload.java` verifica somente o arquivo SERVER ativo a cada 20 ticks e exige duas leituras iguais. Esse contorno preserva o spec e a invalidação de cache do Forge quando o NightConfig 3.6.4 descarta notificações `ENTRY_MODIFY` com contagem maior que 1. O registro é limpo no descarregamento da configuração; não há monitor extra no NeoForge.

`LegacyConfigMigration.prepare()` importa automaticamente o limite do COMMON antes de registrar o SERVER. No Forge, cria apenas o modelo ausente no diretório `defaultconfigs` definido pelo loader, que será usado para gerar novos SERVER de mundo. No NeoForge, cria somente a base SERVER ausente em `config/`. Arquivos SERVER, overrides e modelos existentes permanecem prioritários; o COMMON não é alterado. O spec corrige valores inválidos em memória; TOML malformado usa os padrões normais do loader. Caminhos e schema estão na [referência de configuração](configuracao.md). A [pendência antiga de recarga COMMON](validacao.md#pendência-conhecida-recarga-no-forge) permanece como evidência histórica; a implementação SERVER tem validação registrada separadamente.

O preview é uma estimativa local limitada pelo valor recebido do servidor. O HUD e os contornos ficam em classes de cliente. O Forge usa a API de vértices 1.20.1 e uma subclasse de `RenderType` para acessar os estados protegidos de linhas, sem Mixin próprio.

## Distribuição

Os JARs finais contêm somente classes de produção, traduções, licença e metadados do loader. Testes usam source set e mod de validação separados. Forge executa `reobfJar`. O workflow continua acionado somente por tags `v*`, confere versão/changelog e envia um arquivo por loader ao CurseForge. A versão preparada é 0.2.0; criar o commit local não aciona a publicação.
