# Inventário e seleção em grupo

Nas ações em grupo, o mod usa primeiro a pilha da mão escolhida e depois itens idênticos dos 36 slots do inventário principal, incluindo a hotbar. No Forge a identidade inclui NBT; no NeoForge inclui componentes. Armadura e outra mão ficam fora. Creative não consome e estoque insuficiente permite resultado parcial. Trocas individuais permanecem limitadas à mão.

O recurso está sempre ativo, sem nova opção SERVER. Cada ação captura um orçamento inicial por pilha; devoluções durante a execução não ampliam esse orçamento. A contabilidade de itens pagos e devoluções dos multipartes permanece nos adaptadores.

Segure a tecla de ação conectada (Left Alt por padrão) e role para escolher:

| Modo | Regra |
| --- | --- |
| Conectado | Seis faces, mesmo tipo de Copycat |
| Plano | Mesmo tipo; plano pela origem perpendicular à normal da face clicada |
| Todos os tipos | Seis faces, qualquer tipo suportado |

O modo aparece no HUD e no preview. A roda não troca o slot enquanto a tecla está ativa; em telas mantém o comportamento normal. Soltar a tecla desativa o estado servidor. O cliente lembra o modo durante a conexão e reinicia em Conectado ao desconectar.

Aplicar seleciona a parte pela mesma posição relativa e face do clique em cada formato. Substituir/remover altera todas as partes existentes com o material clicado em cada posição. Todos os modos preservam filtro de material, chunks carregados, barreiras de permissão e limite SERVER por posição.

O servidor guarda tecla e seleção por UUID, valida o identificador recebido e trata identificadores inválidos como Conectado. O protocolo de rede agora é 2; cliente e servidor precisam usar a mesma compilação. O preview segue a mesma busca, mas continua sem conhecer claims do servidor.
