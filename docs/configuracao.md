# Configuração do servidor

Referência do checkout identificado como **0.2.0**, após a implementação das fases 1 e 2. Não houve bump de versão.

## `copycat_replace-server.toml`

Arquivo TOML editado pelo administrador do servidor ou pelo dono do mundo singleplayer. O servidor determina quantas posições cada ação conectada pode alterar; o cliente recebe o limite para o preview. Não há arquivo de configuração próprio de cliente nem opções adicionais de proteção.

| Loader | Caminho e criação |
| --- | --- |
| Forge 1.20.1 / 47.4.0 | `<servidor>/<mundo>/serverconfig/copycat_replace-server.toml`, criado pelo loader ao carregar o mundo. Em singleplayer: `saves/<mundo>/serverconfig/`. |
| NeoForge 1.21.1 / 21.1.236 | `config/copycat_replace-server.toml`, criado ao carregar a configuração SERVER. Um arquivo já existente em `<mundo>/serverconfig/copycat_replace-server.toml` tem precedência para esse mundo. Em singleplayer, o override fica em `saves/<mundo>/serverconfig/`. |

No NeoForge, para dar um limite próprio a um mundo, pare o servidor ou feche o mundo e copie o arquivo base para seu `serverconfig`. Edite a cópia e carregue o mundo novamente. Criar um override enquanto o mundo já está aberto não troca o arquivo monitorado até o próximo carregamento. Mundos sem override usam a base compartilhada. No Forge, `defaultconfigs/copycat_replace-server.toml` pode servir de modelo na criação de arquivos de mundo; não substitui a edição de um mundo já configurado.

## Padrão completo e schema

Padrão gerado no Forge:

```toml
#Maximum connected Copycat positions affected by one bulk action (1-256).
#Range: 1 ~ 256
max_connected_blocks = 64
```

Padrão gerado no NeoForge:

```toml
#Maximum connected Copycat positions affected by one bulk action (1-256).
# Default: 64
# Range: 1 ~ 256
max_connected_blocks = 64
```

| Chave | Tipo | Padrão | Valores aceitos | Efeito |
| --- | --- | --- | --- | --- |
| `max_connected_blocks` | Inteiro | `64` | `1` a `256`, inclusive, em posições de blocos | Limita aplicar, substituir e remover em grupo. Inclui a origem; uma posição multiparte conta uma vez. |

Não há grupos, listas ou outras chaves editáveis do addon. O valor `1` limita ao alvo original; `0` e negativos são inválidos. O limite continua sujeito a estoque disponível, conectividade, tipo, material e permissões. O preview é uma estimativa local: posições recusadas por proteções podem reduzir o resultado real.

O spec do loader limita números abaixo de 1 a 1 e acima de 256 a 256; chaves ausentes ou valores de tipo incompatível usam o padrão 64. Use inteiros; entradas numéricas fracionárias podem ser convertidas para inteiro pelo loader. TOML malformado pode provocar recuperação do arquivo ou erro de carregamento pelo loader. Faça backup antes de editar.

## Recarga e controles

O NeoForge monitora o arquivo ativo. O Forge mantém o monitor nativo e uma verificação adicional a cada 20 ticks do servidor, exigindo conteúdo estável em duas leituras para contornar eventos de gravação agrupados ignorados pelo NightConfig 3.6.4. Quando uma edição válida é recarregada, o servidor usa o novo limite e envia o valor aos clientes conectados para atualizar o preview. O teste de servidor `server_config_reload` verifica a leitura e restauração do valor pelo monitor real do loader. No NeoForge, monitoramento desabilitado nas opções do loader exige reiniciar o servidor/reabrir o mundo; mover/criar um override também exige novo carregamento. O teste de recarga no servidor não substitui validar o preview em conexões reais.

A tecla **Bulk Action on Connected Copycats**, padrão **Left Alt**, é remapeada em **Controls**, não neste TOML. Regras de claims pertencem ao mod de proteção do servidor.

Confira os [comandos e evidências de validação](validacao.md) para distinguir o teste do arquivo da aceitação multiplayer.
