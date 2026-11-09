# Spec Delta

## Purpose

Calcular, para cada MEI, o índice de markup a partir das despesas variáveis, das despesas fixas rateadas e da margem de lucro, e o preço de venda sugerido de cada produto a partir do custo direto, conforme as fórmulas do artigo.

## ADDED Requirements

### Requirement: Índice de markup do MEI

O sistema SHALL calcular o índice de markup do MEI autenticado pela fórmula `Markup = 100 / [100 - (DV + DF + ML)]`, com DV e DF do rateio das despesas e ML da margem de lucro, em pontos percentuais. A resposta MUST trazer DV, DF e ML com 2 casas decimais e o índice com 4 casas (arredondamento meio para cima).

Atende: RF03.

#### Scenario: Markup da loja de ferramentas

- **WHEN** a "Loja de Ferramentas" tem as despesas do estudo de caso (fixas 1.400,00 e variáveis 5%), receita mensal estimada 14000.00 e margem de lucro 25.00
- **AND** envia `GET /api/v1/precificacao` com o seu token
- **THEN** o sistema responde com status 200 com `percentualDespesasVariaveis` 5.00, `percentualDespesasFixas` 10.00, `margemDeLucro` 25.00 e `indiceMarkup` 1.6667

#### Scenario: Markup sem despesas

- **WHEN** um MEI sem despesas informou receita 14000.00 e margem 25.00
- **AND** envia `GET /api/v1/precificacao`
- **THEN** o sistema responde com status 200 com `percentualDespesasVariaveis` 0.00, `percentualDespesasFixas` 0.00 e `indiceMarkup` 1.3333

### Requirement: Preço de venda sugerido dos produtos

O sistema SHALL calcular o preço de venda sugerido de cada produto do MEI pela fórmula `PV = Custo Direto × Markup`, usando o markup e o DF sem arredondamento e arredondando só o preço final para 2 casas decimais (meio para cima). A consulta MUST listar todos os produtos do MEI em ordem alfabética, com identificador, nome, custo direto e preço sugerido.

Atende: RF04.

#### Scenario: Preços sugeridos do estudo de caso

- **WHEN** a "Loja de Ferramentas" tem os 6 produtos, as despesas, a receita 14000.00 e a margem 25.00 do estudo de caso
- **AND** envia `GET /api/v1/precificacao`
- **THEN** `produtos` contém, nesta ordem, com custo direto e preço sugerido:
  - "Alicate universal 8\"": 28.00 e 46.67;
  - "Caixa de ferramentas plástica": 45.00 e 75.00;
  - "Furadeira de impacto 650 W": 180.00 e 300.00;
  - "Jogo de chaves de fenda (6 peças)": 25.00 e 41.67;
  - "Martelo unha 27 mm": 22.00 e 36.67;
  - "Trena 5 m": 12.00 e 20.00

#### Scenario: Preço calculado com o markup sem arredondamento

- **WHEN** a "Loja de Ferramentas" tem os dados do estudo de caso (markup exibido 1.6667)
- **THEN** o preço sugerido da "Furadeira de impacto 650 W", de custo 180.00, é 300.00, e não 300.01 (180.00 × 1.6667)

#### Scenario: Preço calculado com o DF sem arredondamento

- **WHEN** um MEI tem somente a despesa fixa "Aluguel" de 1000.00, receita 3000.00 (DF exibido 33.33), margem 25.00 e o produto "Kit de brocas" de custo 100.00
- **AND** envia `GET /api/v1/precificacao`
- **THEN** o `indiceMarkup` é 2.4000 e o preço sugerido do "Kit de brocas" é 240.00, e não 239.98

#### Scenario: MEI sem produtos

- **WHEN** um MEI com receita e margem informadas e sem produtos envia `GET /api/v1/precificacao`
- **THEN** o sistema responde com status 200 com o `indiceMarkup` calculado e `produtos` como lista vazia

### Requirement: Preço sugerido de um produto

O sistema SHALL devolver, para um produto do MEI autenticado identificado na requisição, o identificador, o nome, o custo direto, o índice de markup e o preço de venda sugerido. Um identificador que não corresponda a produto do MEI MUST gerar status 404, e um identificador em formato inválido, status 400, no formato padrão de erro.

Atende: RF04.

#### Scenario: Preço sugerido da furadeira

- **WHEN** a "Loja de Ferramentas" tem os dados do estudo de caso
- **AND** envia `GET /api/v1/precificacao/produtos/{id}` com o identificador da "Furadeira de impacto 650 W"
- **THEN** o sistema responde com status 200 com `nome` "Furadeira de impacto 650 W", `custoDireto` 180.00, `indiceMarkup` 1.6667 e `precoSugerido` 300.00

#### Scenario: Preço acompanha o novo custo

- **WHEN** a "Loja de Ferramentas" altera o custo direto da "Trena 5 m" de 12.00 para 15.00
- **AND** consulta `GET /api/v1/precificacao/produtos/{id}` da trena
- **THEN** o `precoSugerido` é 25.00

#### Scenario: Produto inexistente na precificação

- **WHEN** a "Loja de Ferramentas" envia `GET /api/v1/precificacao/produtos/{id}` com um identificador que não corresponde a nenhum produto
- **THEN** o sistema responde com status 404 no formato padrão de erro

#### Scenario: Identificador inválido na precificação

- **WHEN** a "Loja de Ferramentas" envia `GET /api/v1/precificacao/produtos/abc`
- **THEN** o sistema responde com status 400 no formato padrão de erro

### Requirement: Precificação exige parâmetros financeiros

Enquanto o MEI não tiver informado a receita mensal estimada e a margem de lucro, a precificação MUST ser recusada com status 422 no formato padrão de erro, com `title` e `detail` em português orientando a informar os parâmetros.

Atende: RF03.

#### Scenario: Precificação sem parâmetros financeiros

- **WHEN** a "Loja de Ferramentas" tem produtos e despesas, mas ainda não informou os parâmetros financeiros
- **AND** envia `GET /api/v1/precificacao`
- **THEN** o sistema responde com status 422 e tipo de conteúdo `application/problem+json`, com `title` "Parâmetros financeiros não informados" e `detail` em português

#### Scenario: Preço de um produto sem parâmetros financeiros

- **WHEN** a "Loja de Ferramentas" ainda não informou os parâmetros financeiros
- **AND** envia `GET /api/v1/precificacao/produtos/{id}` com o identificador da "Furadeira de impacto 650 W"
- **THEN** o sistema responde com status 422

### Requirement: Markup inviável

Quando a soma DV + DF + ML for maior ou igual a 100, o markup não existe pela fórmula, e a precificação MUST ser recusada com status 422 no formato padrão de erro. O `detail` SHALL informar a soma, com 2 casas e vírgula decimal, e que ela precisa ser menor que 100%.

Atende: RF03.

#### Scenario: Soma de exatamente 100%

- **WHEN** a "Loja de Ferramentas" tem as despesas do estudo de caso (DV 5% e DF 10%) e informa margem de lucro 85.00
- **AND** envia `GET /api/v1/precificacao`
- **THEN** o sistema responde com status 422 com `title` "Markup inviável"
- **AND** o `detail` contém "100,00%"

#### Scenario: Soma acima de 100%

- **WHEN** a "Loja de Ferramentas" tem as despesas do estudo de caso e informa margem de lucro 90.00
- **AND** envia `GET /api/v1/precificacao/produtos/{id}` com o identificador da "Furadeira de impacto 650 W"
- **THEN** o sistema responde com status 422 e o `detail` contém "105,00%"

### Requirement: Precificação isolada por MEI

A precificação MUST usar somente os produtos, as despesas e os parâmetros financeiros do MEI do token. Um MEI MUST NOT obter o preço sugerido de produto de outro MEI (para ele, o produto não existe: 404). Os endpoints exigem token.

Atende: RF10, RNF07.

#### Scenario: Cada MEI com o próprio markup

- **WHEN** a "Loja de Ferramentas" tem os dados do estudo de caso
- **AND** a "Papelaria Central" não tem despesas, informou receita 6000.00 e margem 20.00 e tem o produto "Caderno universitário 200 folhas" de custo 16.00
- **THEN** a precificação da loja tem `indiceMarkup` 1.6667 e somente os 6 produtos da loja
- **AND** a precificação da papelaria tem `indiceMarkup` 1.2500 e somente o caderno, com `precoSugerido` 20.00

#### Scenario: Preço de produto de outro MEI

- **WHEN** a "Papelaria Central", com parâmetros informados, envia `GET /api/v1/precificacao/produtos/{id}` com o identificador da "Furadeira de impacto 650 W" da loja
- **THEN** o sistema responde com status 404

#### Scenario: Precificação sem token

- **WHEN** um cliente envia `GET /api/v1/precificacao` sem token
- **THEN** o sistema responde com status 401 no formato padrão de erro
