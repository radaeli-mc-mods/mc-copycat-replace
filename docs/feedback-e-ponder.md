# Feedback e descoberta

Uma tentativa com wrench e material recusado pelo Copycat mostra uma mensagem curta acima da hotbar. O clique continua consumido: essa recusa não deve girar nem desmontar o Copycat. A mesma mensagem aparece em uma ação conectada quando nenhuma posição muda por recusa do material. Material igual, falta de estoque e permissão negada não recebem esse aviso.

A mensagem vem do servidor, pela mão que inicia a interação. O código não envia uma segunda mensagem pelo cliente ou pela outra mão. Os contadores de ações conectadas continuam aparecendo quando há alterações.

## Tutorial de Ponder

A cena adicional **Changing Copycat materials / Trocando materiais dos Copycats** é registrada nos itens **Copycat Step** e **Copycat Panel** do Create. Na lista de cenas de Ponder desses itens, ela ensina:

1. Troca individual com wrench na mão principal e material na secundária.
2. Troca com a ordem das mãos invertida.
3. Sneak para permitir a ação normal da wrench.
4. Aplicar, substituir e remover materiais em Copycats conectados.
5. Restrição por faces, tipo, estoque, limite do servidor e permissões.
6. Consumo de itens idênticos do inventário principal.
7. Alternância pela roda e diferenças entre Conectado, Plano e Todos os tipos.

A tecla de ação conectada aparece conforme seu mapeamento ao abrir/compilar a cena. O padrão continua Left Alt; ela pode ser remapeada em Controls. O tutorial usa somente blocos do Create, com registro independente da instalação de Copycats+.

O mundo do Ponder demonstra os resultados com animações; ele não altera seu mundo nem executa verificações reais de claims. A cena mostra uma barreira de permissão e um exemplo de limite 2, que não muda o valor configurado no servidor.

Consulte a [validação](validacao.md) para distinguir implementação, verificações automatizadas e inspeção prática.
