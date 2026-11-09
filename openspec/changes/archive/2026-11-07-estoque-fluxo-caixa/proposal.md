# Proposal

## Why

O MEI precisa saber quanto tem de cada produto e quanto dinheiro entrou e saiu com as compras e as vendas. Hoje o sistema conhece o custo e o preço sugerido, mas não as quantidades nem o caixa. Esta change registra as entradas e saídas de estoque de cada produto (RF06) e reflete cada uma no fluxo de caixa (RF07). Ela também fornece o estoque atual e as vendas por período, base dos alertas de ruptura e das sugestões sazonais (RF08, RF09).

## What Changes

- Registro de movimentações de estoque vinculadas a um produto do MEI (RF06):
  - **entrada** (compra): informa a quantidade, e o valor unitário é o custo direto do produto;
  - **saída** (venda): informa a quantidade e o valor unitário da venda.
- A data da movimentação é opcional (padrão: hoje) e não pode ser futura, para permitir registrar vendas e compras de dias anteriores.
- Estoque atual de cada produto, igual a entradas menos saídas. Uma venda maior que o estoque atual é recusada com 422, informando o estoque disponível; o estoque nunca fica negativo (RF06).
- Consulta das movimentações, com filtro opcional por produto, e do estoque atual de todos os produtos do MEI (RF06).
- Cada movimentação gera automaticamente, na mesma transação, um lançamento no fluxo de caixa (RF07):
  - entrada de estoque → **saída de caixa** de quantidade × custo direto;
  - saída de estoque → **entrada de caixa** de quantidade × valor da venda.
- Consulta do fluxo de caixa por período (padrão: mês atual), com os lançamentos e os totais de entradas, saídas e saldo (RF07).
- **BREAKING (comportamento):** um produto com movimentações de estoque não pode mais ser excluído. A exclusão responde 409, para não perder o histórico de estoque e de caixa (RF01, RF06).
- Movimentações, estoque e caixa isolados por MEI (RF10, RNF07).
- Tabelas `movimentacao_estoque`, `saldo_estoque` e `lancamento_caixa`, criadas por changeset Liquibase (RNF06).

## Capabilities

### New Capabilities

- `estoque`: entradas e saídas de estoque por produto, estoque atual e recusa de venda sem estoque suficiente (RF06, RF10, RNF07).
- `fluxo-de-caixa`: lançamentos de caixa gerados pelas movimentações de estoque e consulta por período com totais e saldo (RF07, RF10, RNF07).

### Modified Capabilities

- `produtos`: novo requisito: um produto com movimentações de estoque não pode ser excluído (409).

## Impact

- Banco: changeset `004` com as tabelas `movimentacao_estoque`, `saldo_estoque` e `lancamento_caixa`.
- Novos pacotes de feature `br.com.valorizamei.estoque` e `br.com.valorizamei.caixa`. O estoque usa os services de `produto` (validar o produto e obter o custo) e de `caixa` (gerar o lançamento).
- `ProdutoService.excluir`: converte a violação de chave estrangeira em 409.
- Novos endpoints protegidos: `/api/v1/estoque/movimentacoes`, `/api/v1/estoque` e `/api/v1/fluxo-de-caixa`.
- Testes: integração com o estoque do estudo de caso, venda sem estoque, caixa por período, exclusão de produto com movimentações e isolamento entre dois MEIs. O `BancoDeTeste` passa a limpar as novas tabelas.
- `docs/testes-aceitacao.md`: registro do teste de aceitação do RF06 e do RF07.
- README: estado, endpoints, exemplos no Swagger UI, problemas comuns e total de testes.
