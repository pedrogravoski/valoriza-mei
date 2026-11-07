# produtos Specification

## Purpose
Manter o catálogo de produtos de cada MEI com o custo direto de aquisição e o estoque mínimo, base para o cálculo do preço de venda, para o controle de estoque e para os alertas.

## Requirements

### Requirement: Cadastro de produto com custo direto

O sistema SHALL permitir que o MEI autenticado cadastre um produto informando o nome, o custo direto de aquisição em reais e o estoque mínimo. O produto cadastrado pertence ao MEI do token, e a resposta MUST trazer o identificador gerado, os dados gravados e a localização do novo produto.

Atende: RF01.

#### Scenario: Cadastro da furadeira da loja de ferramentas

- **WHEN** a "Loja de Ferramentas" envia `POST /api/v1/produtos` com o seu token, nome "Furadeira de impacto 650 W", custo direto 180.00 e estoque mínimo 3
- **THEN** o sistema responde com status 201 e o cabeçalho `Location` com `/api/v1/produtos/{id}`
- **AND** o corpo contém o `id` do produto, `nome` "Furadeira de impacto 650 W", `custoDireto` 180.00 e `estoqueMinimo` 3

#### Scenario: Nome com espaços nas pontas

- **WHEN** a "Loja de Ferramentas" cadastra o produto com nome "  Trena 5 m  ", custo direto 12.00 e estoque mínimo 5
- **THEN** o sistema responde com status 201 e `nome` "Trena 5 m"

### Requirement: Validação dos dados do produto

O sistema MUST recusar com status 400, no formato padrão de erro com a lista de campos inválidos, o cadastro ou a alteração de produto sem nome, com nome acima de 120 caracteres, com custo direto ausente, menor ou igual a zero ou com mais de 2 casas decimais, ou com estoque mínimo ausente ou negativo. Nenhum produto é gravado ou alterado.

Atende: RF01.

#### Scenario: Campos obrigatórios ausentes

- **WHEN** a "Loja de Ferramentas" envia `POST /api/v1/produtos` com o corpo `{}`
- **THEN** o sistema responde com status 400 e `erros` contém itens para `nome`, `custoDireto` e `estoqueMinimo`

#### Scenario: Custo direto zero

- **WHEN** a "Loja de Ferramentas" cadastra o produto "Trena 5 m" com custo direto 0.00 e estoque mínimo 5
- **THEN** o sistema responde com status 400 e `erros` contém um item para `custoDireto`

#### Scenario: Custo direto com mais de 2 casas decimais

- **WHEN** a "Loja de Ferramentas" cadastra o produto "Trena 5 m" com custo direto 12.005 e estoque mínimo 5
- **THEN** o sistema responde com status 400 e `erros` contém um item para `custoDireto`

#### Scenario: Estoque mínimo negativo

- **WHEN** a "Loja de Ferramentas" cadastra o produto "Trena 5 m" com custo direto 12.00 e estoque mínimo -1
- **THEN** o sistema responde com status 400 e `erros` contém um item para `estoqueMinimo`

### Requirement: Nome de produto único no MEI

O nome do produto MUST ser único entre os produtos do mesmo MEI, sem diferenciar maiúsculas e minúsculas. Um cadastro ou alteração que repita o nome de outro produto do mesmo MEI SHALL ser recusado com status 409 no formato padrão de erro. MEIs diferentes podem ter produtos com o mesmo nome.

Atende: RF01, RNF07.

#### Scenario: Produto repetido com outra grafia

- **WHEN** a "Loja de Ferramentas" já tem o produto "Furadeira de impacto 650 W"
- **AND** cadastra o produto "FURADEIRA DE IMPACTO 650 W"
- **THEN** o sistema responde com status 409 com `title` e `detail` em português
- **AND** a loja continua com um único produto com esse nome

#### Scenario: Mesmo nome em MEIs diferentes

- **WHEN** a "Loja de Ferramentas" já tem o produto "Trena 5 m"
- **AND** a "Papelaria Central" cadastra o produto "Trena 5 m"
- **THEN** o sistema responde com status 201

### Requirement: Listagem dos produtos do MEI

O sistema SHALL listar os produtos do MEI autenticado ordenados pelo nome, sem diferenciar maiúsculas e minúsculas. Quando o MEI não tiver produtos, a resposta MUST ser uma lista vazia com status 200.

Atende: RF01.

#### Scenario: Produtos da loja de ferramentas em ordem alfabética

- **WHEN** a "Loja de Ferramentas" cadastrou "Furadeira de impacto 650 W", "Jogo de chaves de fenda (6 peças)", "Martelo unha 27 mm", "Trena 5 m", "Alicate universal 8\"" e "Caixa de ferramentas plástica", com os custos e estoques mínimos do estudo de caso
- **AND** envia `GET /api/v1/produtos` com o seu token
- **THEN** o sistema responde com status 200 e uma lista de 6 produtos, nesta ordem: "Alicate universal 8\"", "Caixa de ferramentas plástica", "Furadeira de impacto 650 W", "Jogo de chaves de fenda (6 peças)", "Martelo unha 27 mm" e "Trena 5 m"
- **AND** o item "Trena 5 m" tem `custoDireto` 12.00 e `estoqueMinimo` 5

#### Scenario: MEI sem produtos

- **WHEN** um MEI recém-cadastrado envia `GET /api/v1/produtos`
- **THEN** o sistema responde com status 200 e uma lista vazia

### Requirement: Consulta de produto pelo identificador

O sistema SHALL devolver os dados de um produto do MEI autenticado a partir do seu identificador. Um identificador que não corresponda a produto do MEI MUST gerar status 404, e um identificador em formato inválido MUST gerar status 400, ambos no formato padrão de erro.

Atende: RF01.

#### Scenario: Consulta da furadeira

- **WHEN** a "Loja de Ferramentas" envia `GET /api/v1/produtos/{id}` com o identificador da "Furadeira de impacto 650 W"
- **THEN** o sistema responde com status 200 com `nome` "Furadeira de impacto 650 W", `custoDireto` 180.00 e `estoqueMinimo` 3

#### Scenario: Produto inexistente

- **WHEN** a "Loja de Ferramentas" envia `GET /api/v1/produtos/{id}` com um identificador que não corresponde a nenhum produto
- **THEN** o sistema responde com status 404 e tipo de conteúdo `application/problem+json`, com `title` e `detail` em português

#### Scenario: Identificador em formato inválido

- **WHEN** a "Loja de Ferramentas" envia `GET /api/v1/produtos/abc`
- **THEN** o sistema responde com status 400 no formato padrão de erro

### Requirement: Alteração de produto

O sistema SHALL permitir alterar o nome, o custo direto e o estoque mínimo de um produto do MEI autenticado, com as mesmas validações do cadastro, e responder com os dados atualizados. A alteração de produto que não pertença ao MEI MUST gerar status 404.

Atende: RF01.

#### Scenario: Reajuste do custo da furadeira

- **WHEN** a "Loja de Ferramentas" envia `PUT /api/v1/produtos/{id}` para a "Furadeira de impacto 650 W" com o mesmo nome, custo direto 190.00 e estoque mínimo 4
- **THEN** o sistema responde com status 200 com `custoDireto` 190.00 e `estoqueMinimo` 4
- **AND** uma nova consulta do produto devolve os valores alterados

#### Scenario: Alteração para o nome de outro produto do MEI

- **WHEN** a "Loja de Ferramentas" tem os produtos "Furadeira de impacto 650 W" e "Trena 5 m"
- **AND** altera o nome da "Trena 5 m" para "furadeira de impacto 650 w"
- **THEN** o sistema responde com status 409
- **AND** o produto continua com o nome "Trena 5 m"

#### Scenario: Alteração de produto inexistente

- **WHEN** a "Loja de Ferramentas" envia `PUT /api/v1/produtos/{id}` com dados válidos e um identificador que não corresponde a nenhum produto
- **THEN** o sistema responde com status 404

### Requirement: Exclusão de produto

O sistema SHALL permitir excluir um produto do MEI autenticado, respondendo com status 204 sem corpo; depois da exclusão, o produto não aparece na listagem nem na consulta. A exclusão de produto que não pertença ao MEI MUST gerar status 404.

Atende: RF01.

#### Scenario: Exclusão da caixa de ferramentas

- **WHEN** a "Loja de Ferramentas" envia `DELETE /api/v1/produtos/{id}` para a "Caixa de ferramentas plástica"
- **THEN** o sistema responde com status 204
- **AND** `GET /api/v1/produtos/{id}` com o mesmo identificador responde 404
- **AND** a listagem da loja não contém mais o produto

#### Scenario: Exclusão de produto inexistente

- **WHEN** a "Loja de Ferramentas" envia `DELETE /api/v1/produtos/{id}` com um identificador que não corresponde a nenhum produto
- **THEN** o sistema responde com status 404

### Requirement: Produtos isolados por MEI

Cada produto MUST pertencer ao MEI identificado no token de quem o cadastrou; identificadores de MEI enviados no corpo ou na query SHALL ser ignorados. Um MEI MUST NOT listar, consultar, alterar ou excluir produtos de outro MEI: para ele, o produto de outro MEI não existe (404). Os endpoints de produtos exigem token.

Atende: RF10, RNF07.

#### Scenario: Cada MEI lista somente os próprios produtos

- **WHEN** a "Loja de Ferramentas" cadastrou a "Furadeira de impacto 650 W" e a "Papelaria Central" cadastrou o "Caderno universitário 200 folhas"
- **AND** cada uma envia `GET /api/v1/produtos` com o próprio token
- **THEN** a loja recebe somente a "Furadeira de impacto 650 W"
- **AND** a papelaria recebe somente o "Caderno universitário 200 folhas"

#### Scenario: Consulta, alteração e exclusão do produto de outro MEI

- **WHEN** a "Papelaria Central" envia, com o próprio token, `GET`, `PUT` com dados válidos e `DELETE` em `/api/v1/produtos/{id}` com o identificador da "Furadeira de impacto 650 W" da loja
- **THEN** o sistema responde com status 404 nas três requisições
- **AND** a furadeira continua na loja com nome "Furadeira de impacto 650 W", custo direto 180.00 e estoque mínimo 3

#### Scenario: Tenant enviado no corpo é ignorado

- **WHEN** a "Loja de Ferramentas" envia `POST /api/v1/produtos` com o seu token e um corpo válido que inclui `tenantId` com o identificador da "Papelaria Central"
- **THEN** o produto é cadastrado para a "Loja de Ferramentas"
- **AND** não aparece na listagem da "Papelaria Central"

#### Scenario: Produtos sem token

- **WHEN** um cliente envia `GET /api/v1/produtos` sem token
- **THEN** o sistema responde com status 401 no formato padrão de erro
