# Build e validação

## Comandos

Executar na raiz, com JDK 21 em `JAVA_HOME`. O toolchain Forge usa Java 17; o resolver pode baixá-lo quando necessário. No Windows usar `gradlew.bat`; em Linux usar `./gradlew`.

```powershell
.\gradlew.bat buildAll
.\gradlew.bat verifyAll
.\gradlew.bat verifyAll -PwithoutCopycats
.\gradlew.bat runForgeClient
.\gradlew.bat runNeoForgeClient
.\gradlew.bat runForgeServer
.\gradlew.bat runNeoForgeServer
.\gradlew.bat :forge1201:runClientSmoke
.\gradlew.bat :neoforge:runClientSmoke
```

`buildAll` constrói os loaders em sequência. `verifyAll` verifica os artefatos e executa GameTests dos dois loaders em sequência. A opção `-PwithoutCopycats` mantém a integração compilada, mas remove Copycats+ do runtime. O VS Code oferece Build All como build padrão e tarefas individuais.

Para somente compilar e verificar os JARs, usar `buildAll`. `verifyAll` inicia servidores de GameTests; `runClientSmoke` abre um cliente. O `build` da raiz também inclui `check`/`verifyAll`. Executar as tarefas de jogo individualmente, conforme a memória disponível. Os scripts opcionais de multiplayer abrem um servidor e dois clientes e não fazem parte de `buildAll`.

Os artefatos ficam em `forge1201/build/libs/copycat_replace-forge-1.20.1-0.2.0.jar` e `neoforge/build/libs/copycat_replace-neoforge-1.21.1-0.2.0.jar`.

## Camadas de evidência

| Camada | Critério |
|---|---|
| Build | Compilação, empacotamento e reobfuscação Forge |
| Artefato | Metadados expandidos, loader/Java corretos, traduções/licença e ausência de classes de teste/dependências embarcadas |
| GameTests | Blocos e inventários reais no servidor, nos dois loaders, com e sem Copycats+ |
| Cliente | Entrada remapeada, pacotes reais, sincronização do bloco, preview, HUD, telas e eventos de som |
| Visual | Inspeção das capturas e comportamento durante transições |
| Multiplayer | Servidor dedicado com dois clientes, limite e estado por jogador, mudanças observadas remotamente e reconexão |
| Produção | Repetição usando o JAR Forge final reobfuscado em instalação normal |

GameTests cobrem ações individuais e conectadas, ordens de mãos, Creative/Survival, material inválido/igual, bypass, estoque parcial, inventário cheio, barreiras por tipo/material/diagonal, limites 1/64/256 e multipartes. Testes com jogadores simulados não substituem multiplayer com conexões reais.

O teste de cliente cria um mundo descartável em `build/client-smoke-copycats` ou `build/client-smoke-create`. `result.txt` registra PASS/FAIL e cada etapa; capturas ficam em `screenshots`. Ele usa as ações e canais reais do addon. Capturas precisam ser inspecionadas antes de declarar aprovação visual. O evento de som comprova despacho; não equivale a uma avaliação auditiva humana.

## Resultado desta implementação

Esta seção registra a base anterior às fases 1 e 2. Consulte a seção [Permissões e configuração SERVER](#fases-1-e-2--permissões-e-configuração-server) para o estado das alterações atuais.

Versão preparada: **0.2.0**. Os resultados de jogo abaixo foram obtidos antes do bump, com o código de produção ainda identificado como 0.1.0; a preparação da versão altera somente a identificação nos artefatos. O build e a verificação dos JARs foram repetidos para 0.2.0, sem iniciar Minecraft. Os testes práticos adicionais foram interrompidos a pedido do usuário por consumo de memória do host; sua continuação fica a cargo do usuário. Nenhuma instância de teste iniciada pelo agente permaneceu em execução.

| Camada | Forge 1.20.1 / Create 6.0.8 | NeoForge 1.21.1 / Create 6.0.10 |
|---|---|---|
| Build e artefatos | Aprovados, incluindo reobfuscação | Aprovados |
| GameTests com Copycats+ | 21/21 aprovados | 21/21 aprovados |
| GameTests sem Copycats+ | 17/17 aprovados | 17/17 aprovados |
| Cliente com e sem Copycats+ | 22 etapas aprovadas em cada execução | 22 etapas aprovadas em cada execução |
| Visual | Capturas de HUD/contornos inspecionadas; revisão prática completa pendente | Capturas de HUD/contornos inspecionadas; revisão prática completa pendente |
| Multiplayer com Copycats+ | Parcial; pendência de recarga abaixo | Aprovado no cenário automatizado com dois clientes |
| Multiplayer sem Copycats+ | Parcial; pendência de recarga abaixo | Pendente |
| Instalação normal com JAR final | Inicialização, aplicação, substituição, remoção e atualização remota verificadas; cenário completo pendente | Não repetida nesta etapa |
| Sons | Despacho de evento aprovado; avaliação auditiva pendente | Despacho de evento aprovado; avaliação auditiva pendente |

São **76 execuções de GameTests** nas quatro combinações, não 76 cenários diferentes. Os testes de cliente aprovados verificam clique real, tecla remapeada, abertura/fechamento de tela, limite recebido, preview e soltura da tecla. Eles não cobrem toda a matriz manual abaixo.

Evidências locais, mantidas nas pastas ignoradas de build:

- `build/version-0.2.0-build.log`: build e verificação dos artefatos 0.2.0, sem execução de jogo.
- `build/validation-all.log`: builds, artefatos e GameTests com Copycats+.
- `build/validation-without.log`: GameTests sem Copycats+.
- `<módulo>/build/client-smoke-copycats/result.txt` e `client-smoke-create/result.txt`: etapas e resultado do cliente.
- `neoforge/build/multiplayer-copycats/result.txt`: multiplayer aprovado, incluindo multipartes, recarga do limite, estado por jogador e desconexão/reconexão com tecla ativa.
- `forge1201/build/multiplayer-production-copycats/`: logs da instalação Forge normal com o JAR reobfuscado.

As capturas representativas estão em [Forge com preview](validation/forge-preview.png), [NeoForge com preview](validation/neoforge-preview.png) e [Forge após soltar a tecla](validation/forge-released.png). A execução remota do workflow de publicação não foi realizada.

### Pendência conhecida: recarga no Forge

Registro histórico do arquivo COMMON. A causa foi reproduzida durante as fases 1 e 2: o NightConfig 3.6.4 ignora notificações `ENTRY_MODIFY` cuja contagem é maior que 1. Um programa isolado em Java 17 no Windows observou `ENTRY_MODIFY count=2` e nenhum callback do NightConfig. A implementação SERVER agora inclui um contorno restrito ao arquivo deste addon no Forge; o aceite multiplayer deve ser repetido com o novo arquivo.

No cenário Forge, o servidor carregou inicialmente o limite 3 e o enviou ao cliente. Após editar `config/copycat_replace-common.toml` para limite 1 durante a execução, a configuração em memória continuou em 3 e não foi observado o evento de recarga. O listener de `ModConfigEvent.Reloading` está registrado e agenda o envio na thread do servidor; falta esclarecer por que a alteração do arquivo não foi recebida nessa instalação.

O comportamento foi observado em desenvolvimento e na instalação normal Forge. O cenário parou na etapa de recarga, portanto não comprova as etapas posteriores de multipartes e reconexão no Forge. O encerramento posterior por desconexão dos clientes não identifica a causa da falta de recarga. Essa pendência impede declarar a validação completa.

Para reproduzir manualmente, iniciar o servidor com `max_connected_blocks = 3`, conectar um cliente e conferir o preview sobre quatro Copycats conectados. Editar o arquivo COMMON do servidor para `max_connected_blocks = 1` sem reiniciar e conferir o HUD e quantos blocos a ação altera. Registrar também o resultado após reconectar e após reiniciar o servidor. Repetir com e sem Copycats+.

### Matriz prática restante

Executar nos dois loaders, com e sem Copycats+, usando os JARs finais:

- Aplicar e substituir individualmente nas duas ordens de mãos; Sneak, materiais inválidos e bloqueio dos efeitos da wrench.
- Aplicar, substituir e remover grupos; seis faces, diagonais excluídas, tipos/materiais distintos e limites 1, 64 e 256.
- Survival e Creative; estoque insuficiente, execução parcial, inventário cheio e devoluções sem perda/duplicação.
- Multipartes com materiais iguais e diferentes; seleção da parte, transferência do item pago e remoção.
- Tecla remapeada, telas, troca de alvo/material, movimento e soltura; revisão visual contínua de HUD/contornos e escuta dos sons.
- Servidor dedicado com dois jogadores; alterações remotas, isolamento da tecla, recarga do limite, desconexão/reconexão e inicialização sem Copycats+.

Os resultados automatizados acima são evidência parcial dessa matriz. O aceite prático completo permanece pendente.

## Fases 1 e 2 — permissões e configuração SERVER

O código continua identificado como **0.2.0**, sem bump, tag ou publicação. As fases estão implementadas, com aceite prático separado da validação automatizada.

| Verificação | Forge 1.20.1 / Create 6.0.8 | NeoForge 1.21.1 / Create 6.0.10 |
| --- | --- | --- |
| Compilação, build e artefatos | Aprovados, incluindo reobfuscação | Aprovados |
| GameTests com Copycats+ | 42/42 aprovados | 42/42 aprovados |
| GameTests sem Copycats+ | 37/37 aprovados | 37/37 aprovados |
| Recarga do arquivo SERVER | Aprovada, incluindo 256, correção de 0 para 1 e restauração | Aprovada, incluindo 256, correção de 0 para 1 e restauração |
| Claims reais e multiplayer desta alteração | Pendentes | Pendentes |

Em medição anterior com Copycats+, o cenário sintético de 256 substituições/512 eventos levou aproximadamente **3,18 ms no Forge** e **5,67 ms no NeoForge**, incluindo alterações de materiais. Esses tempos são observações locais, sem mod de claims, não um benchmark de servidores. Candidatos recusados na fronteira também recebem consultas de permissão e podem aumentar o total de eventos antes de completar o grupo permitido.

A execução NeoForge com Copycats+ usou override do mundo com limite 64 e base global com limite 7; o arquivo ativo registrado pelo teste foi o override. Isso comprova a seleção do arquivo pelo loader, não a atualização do HUD ao alternar mundos.

A inicialização sem Copycats+ foi repetida com COMMON 5 e novos SERVER ausentes. O Forge gerou seu modelo e o SERVER de mundo com 5; o NeoForge gerou sua base SERVER com 5. Ambos carregaram o limite 5 e mantiveram o COMMON intacto. `MigrationScenarios` verifica valores 1/17/64/256, ausência do COMMON, TOML malformado, correção de 0 para 1, preservação do arquivo antigo e recusa de sobrescrever destinos existentes.

Evidências atuais: `build/automatic-migration.log` registra build e testes com Copycats+; `build/automatic-migration-create.log` registra build e testes sem Copycats+, nos dois loaders, incluindo verificação dos artefatos e reobfuscação Forge.

`build/automatic-migration-create.log` registra `buildAll verifyAll -PwithoutCopycats --offline` aprovado. São **158 execuções de GameTests** nas quatro combinações, não 154 cenários diferentes. A documentação pública em inglês e pt-BR passou em `pnpm run check` e `pnpm run build`; os seis blocos TOML de padrões (projeto e dois idiomas, duas variantes por destino) foram analisados com o parser NightConfig e comparados às declarações dos dois módulos.

`PermissionScenarios` cobre origem/meio/borda recusados ao aplicar, substituir e remover; substituição exige veto favorável nos dois eventos; troca simples nas duas mãos; caminho alternativo permitido; preview sem eventos; contabilidade de materiais; e 256 posições com 512 eventos de permissão. `CopycatsScenarios` também verifica que veto em multipartes preserva materiais e itens pagos. São listeners sintéticos dos eventos reais dos loaders, sem FTB Chunks ou Open Parties and Claims instalados.

`ServerConfigScenarios` edita o arquivo SERVER ativo e aguarda a configuração em memória mudar: valor baixo, 256, 0 corrigido para 1, valor intermediário e restauração do conteúdo original. O Forge usa o monitor nativo e o contorno de notificações agrupadas; o NeoForge usa seu monitor nativo. O teste tem orçamento de ticks maior porque o GameTestServer NeoForge avança ticks sem cadência de tempo real, enquanto o monitor usa tempo real.

O script `scripts/run_multiplayer_validation.py` foi atualizado para preparar e editar `<mundo>/serverconfig/copycat_replace-server.toml`. No NeoForge ele cria o override antes da inicialização. Preparar o script não representa executar o cenário com dois clientes.

### Aceite prático restante destas fases

- Claims reais em ambos os loaders: aplicar/substituir/remover na origem e através de uma fronteira de claim, com inventário e materiais preservados nas posições recusadas.
- Dois clientes nos dois loaders, com e sem Copycats+: login, recarga e atualização do preview, multipartes, isolamento da tecla, desconexão/reconexão e reinício.
- Alternar mundos com limites diferentes e conferir que o HUD não conserva o valor do mundo anterior.
- Repetir em instalação normal com os JARs finais. A aprovação dos GameTests não substitui esses cenários.
