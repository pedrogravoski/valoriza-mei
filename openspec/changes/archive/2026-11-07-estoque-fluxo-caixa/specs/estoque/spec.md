# Spec Delta

## Purpose

Registrar as entradas (compras) e saídas (vendas) de estoque de cada produto do MEI e manter o estoque atual, sem permitir que ele fique negativo.

## ADDED Requirements

### Requirement: Entrada de estoque

O sistema SHALL permitir que o MEI autenticado registre a entrada (compra) de um produto seu informando o produto, o tipo `ENTRADA` e a quantidade. O valor unitário da entrada MUST ser o custo direto do produto, e a resposta traz a movimentação gravada, com o valor total e o estoque atual do produto após a entrada.

Atende: RF06.

#### Scenario: Compra de furadeiras

- **WHEN** a "Loja de Ferramentas" tem a "Furadeira de impacto 650 W" (custo 180.00) sem movimentações
- **AND** envia `POST /api/v1/estoque/movimentacoes` com o produto, tipo `ENTRADA` e quantidade 10
- **THEN** o sistema responde com status 201 e o cabeçalho `Location` com `/api/v1/estoque/movimentacoes/{id}`
- **AND** o corpo contém `nomeProduto` "Furadeira de impacto 650 W", `tipo` `ENTRADA`, `quantidade` 10, `valorUnitario` 180.00, `valorTotal` 1800.00 e `estoqueAtual` 10

### Requirement: Saída de estoque

O sistema SHALL permitir que o MEI autenticado registre a saída (venda) de um produto seu informando o produto, o tipo `SAIDA`, a quantidade e o valor unitário da venda. A resposta traz a movimentação gravada, com o valor total e o estoque atual do produto após a saída.

Atende: RF06.

#### Scenario: Venda de furadeiras

- **WHEN** a "Loja de Ferramentas" tem 10 unidades da "Furadeira de impacto 650 W"
- **AND** registra a saída de 2 unidades com valor unitário 300.00
- **THEN** o sistema responde com status 201 com `tipo` `SAIDA`, `quantidade` 2, `valorUnitario` 300.00, `valorTotal` 600.00 e `estoqueAtual` 8

### Requirement: Validação da movimentação

O sistema MUST recusar com status 400, no formato padrão de erro com a lista de campos inválidos, a movimentação sem produto, sem tipo ou com tipo desconhecido, com quantidade menor que 1 ou maior que 1.000.000, com data futura, a saída sem valor unitário ou com valor unitário menor ou igual a zero ou com mais de 2 casas decimais, e a entrada com valor unitário informado. Nada é gravado.

Atende: RF06.

#### Scenario: Campos obrigatórios ausentes

- **WHEN** a "Loja de Ferramentas" envia `POST /api/v1/estoque/movimentacoes` com o corpo `{}`
- **THEN** o sistema responde com status 400 e `erros` contém itens para `produtoId`, `tipo` e `quantidade`

#### Scenario: Quantidade zero

- **WHEN** a "Loja de Ferramentas" registra a entrada da furadeira com quantidade 0
- **THEN** o sistema responde com status 400 e `erros` contém um item para `quantidade`

#### Scenario: Venda sem valor unitário

- **WHEN** a "Loja de Ferramentas" registra a saída de 1 furadeira sem valor unitário
- **THEN** o sistema responde com status 400 e `erros` contém um item para `valorUnitario`

#### Scenario: Compra com valor unitário

- **WHEN** a "Loja de Ferramentas" registra a entrada de 10 furadeiras com valor unitário 170.00
- **THEN** o sistema responde com status 400 e `erros` contém um item para `valorUnitario`

#### Scenario: Data futura

- **WHEN** a data atual é 15/07/2026
- **AND** a "Loja de Ferramentas" registra a entrada de 10 furadeiras com data 16/07/2026
- **THEN** o sistema responde com status 400 e `erros` contém um item para `data`

### Requirement: Data da movimentação

A movimentação SHALL ser registrada na data informada, que pode ser anterior à data atual. Sem data informada, a movimentação MUST receber a data atual.

Atende: RF06.

#### Scenario: Movimentação sem data

- **WHEN** a data atual é 15/07/2026
- **AND** a "Loja de Ferramentas" registra a entrada de 10 furadeiras sem data
- **THEN** a movimentação é gravada com `data` 2026-07-15

#### Scenario: Venda de dia anterior

- **WHEN** a data atual é 15/07/2026
- **AND** a "Loja de Ferramentas" registra a venda de 1 furadeira a 300.00 com data 2026-07-10
- **THEN** a movimentação é gravada com `data` 2026-07-10

### Requirement: Venda sem estoque suficiente

Uma saída com quantidade maior que o estoque atual do produto MUST ser recusada com status 422 no formato padrão de erro, com `detail` em português informando o produto, o estoque disponível e a quantidade pedida. Nenhuma movimentação ou lançamento de caixa é gravado, e o estoque não muda.

Atende: RF06.

#### Scenario: Venda de trenas acima do estoque

- **WHEN** a "Loja de Ferramentas" tem 4 unidades da "Trena 5 m"
- **AND** registra a saída de 5 unidades com valor unitário 20.00
- **THEN** o sistema responde com status 422 com `title` "Estoque insuficiente" e `detail` contendo "Trena 5 m", "4" e "5"
- **AND** o estoque atual da trena continua 4

#### Scenario: Venda de produto sem movimentações

- **WHEN** a "Loja de Ferramentas" tem a "Caixa de ferramentas plástica" sem movimentações
- **AND** registra a saída de 1 unidade
- **THEN** o sistema responde com status 422

#### Scenario: Venda de todo o estoque

- **WHEN** a "Loja de Ferramentas" tem 4 unidades da "Trena 5 m"
- **AND** registra a saída de 4 unidades
- **THEN** o sistema responde com status 201 com `estoqueAtual` 0

### Requirement: Movimentação de produto do próprio MEI

A movimentação MUST referenciar um produto do MEI autenticado. Um produto inexistente ou de outro MEI SHALL gerar status 404 no formato padrão de erro, sem gravar nada.

Atende: RF06, RNF07.

#### Scenario: Produto inexistente

- **WHEN** a "Loja de Ferramentas" registra a entrada de 10 unidades de um produto que não existe
- **THEN** o sistema responde com status 404

### Requirement: Consulta das movimentações

O sistema SHALL listar as movimentações do MEI autenticado da mais recente para a mais antiga, com filtro opcional pelo produto, e SHALL devolver uma movimentação pelo identificador. Um identificador que não corresponda a movimentação do MEI MUST gerar status 404.

Atende: RF06.

#### Scenario: Movimentações da furadeira

- **WHEN** a "Loja de Ferramentas" registrou a entrada de 10 furadeiras em 01/07/2026, a venda de 2 furadeiras em 10/07/2026 e a entrada de 14 trenas em 05/07/2026
- **AND** envia `GET /api/v1/estoque/movimentacoes?produtoId=<id da furadeira>`
- **THEN** o sistema responde com status 200 e 2 movimentações: primeiro a venda de 10/07, depois a entrada de 01/07

#### Scenario: Todas as movimentações

- **WHEN** a "Loja de Ferramentas" registrou as três movimentações do cenário anterior
- **AND** envia `GET /api/v1/estoque/movimentacoes`
- **THEN** o sistema responde com 3 movimentações, nas datas 10/07, 05/07 e 01/07

#### Scenario: Movimentação inexistente

- **WHEN** a "Loja de Ferramentas" envia `GET /api/v1/estoque/movimentacoes/{id}` com um identificador que não corresponde a nenhuma movimentação
- **THEN** o sistema responde com status 404

### Requirement: Estoque atual dos produtos

O sistema SHALL listar todos os produtos do MEI autenticado em ordem alfabética, com o estoque atual (entradas menos saídas) e o estoque mínimo. Um produto sem movimentações MUST aparecer com estoque atual 0.

Atende: RF06.

#### Scenario: Estoque do estudo de caso

- **WHEN** a "Loja de Ferramentas" tem os 6 produtos do estudo de caso e registrou as entradas e saídas: furadeira 11 e 6, jogo de chaves 29 e 9, martelo 20 e 5, trena 14 e 10, alicate 16 e 4, caixa de ferramentas 10 e nenhuma saída
- **AND** envia `GET /api/v1/estoque`
- **THEN** o sistema responde com status 200 e os estoques atuais: alicate 12, caixa de ferramentas 10, furadeira 5, jogo de chaves 20, martelo 15 e trena 4, com os estoques mínimos do cadastro

#### Scenario: Produto sem movimentações

- **WHEN** a "Loja de Ferramentas" cadastrou a "Furadeira de impacto 650 W" e não registrou movimentações
- **AND** envia `GET /api/v1/estoque`
- **THEN** a furadeira aparece com `estoqueAtual` 0 e `estoqueMinimo` 3

### Requirement: Estoque isolado por MEI

As movimentações e o estoque MUST pertencer ao MEI do token: um MEI MUST NOT movimentar, listar ou consultar o estoque ou as movimentações de outro MEI. Os endpoints de estoque exigem token.

Atende: RF10, RNF07.

#### Scenario: Movimentação de produto de outro MEI

- **WHEN** a "Papelaria Central" registra a entrada de 10 unidades da "Furadeira de impacto 650 W" da loja
- **THEN** o sistema responde com status 404
- **AND** o estoque da furadeira na loja não muda

#### Scenario: Cada MEI consulta o próprio estoque

- **WHEN** a "Loja de Ferramentas" tem 10 furadeiras e a "Papelaria Central" tem 50 unidades do "Caderno universitário 200 folhas"
- **THEN** `GET /api/v1/estoque` da loja traz somente a furadeira com 10
- **AND** o da papelaria traz somente o caderno com 50
- **AND** `GET /api/v1/estoque/movimentacoes/{id}` da papelaria com o identificador de uma movimentação da loja responde 404

#### Scenario: Estoque sem token

- **WHEN** um cliente envia `GET /api/v1/estoque` sem token
- **THEN** o sistema responde com status 401 no formato padrão de erro
