# Spec Delta

## Purpose

Avisar o MEI sobre produtos em ruptura de estoque e sobre datas comemorativas próximas, sugerindo reforço de estoque, promoção ou reajuste de preço com base no giro de cada produto.

## ADDED Requirements

### Requirement: Alerta de ruptura de estoque

O sistema SHALL listar, em ordem alfabética, os produtos do MEI autenticado com estoque mínimo maior que zero e estoque atual menor ou igual ao mínimo, com estoque atual e mínimo. Produtos com estoque mínimo zero MUST NOT gerar alerta de ruptura. O alerta não depende de data comemorativa.

Atende: RF08.

#### Scenario: Ruptura no estudo de caso

- **WHEN** a "Loja de Ferramentas" tem o estoque do estudo de caso (furadeira 5/3, jogo de chaves 20/5, martelo 15/4, trena 4/5, alicate 12/4 e caixa de ferramentas 10/3, em estoque atual/mínimo)
- **AND** envia `GET /api/v1/alertas` com o seu token
- **THEN** o sistema responde com status 200 e `ruptura` contém somente a "Trena 5 m", com `estoqueAtual` 4 e `estoqueMinimo` 5

#### Scenario: Produto sem estoque

- **WHEN** a "Loja de Ferramentas" cadastrou a "Furadeira de impacto 650 W" com estoque mínimo 3 e não registrou movimentações
- **THEN** `ruptura` contém a furadeira com `estoqueAtual` 0

#### Scenario: Estoque igual ao mínimo

- **WHEN** a "Loja de Ferramentas" tem 5 trenas e estoque mínimo 5
- **THEN** `ruptura` contém a trena

#### Scenario: Estoque mínimo zero não gera alerta

- **WHEN** a "Loja de Ferramentas" cadastrou um produto com estoque mínimo 0 e sem movimentações
- **THEN** `ruptura` não contém o produto

### Requirement: Datas comemorativas próximas nos alertas

Os alertas SHALL listar as datas comemorativas, fixas e personalizadas do MEI, cuja próxima ocorrência esteja entre a data atual e 30 dias depois (inclusive), ordenadas pela data, com nome, data, dias restantes e indicação de data personalizada. A resposta MUST informar a data de referência usada.

Atende: RF09.

#### Scenario: Dia dos Pais no estudo de caso

- **WHEN** a data atual é 15/07/2026
- **AND** a "Loja de Ferramentas" envia `GET /api/v1/alertas`
- **THEN** a resposta tem `dataReferencia` 2026-07-15 e `datasComemorativas` contém somente o "Dia dos Pais", com `data` 2026-08-09, `diasRestantes` 25 e `personalizada` falso

#### Scenario: Data personalizada próxima

- **WHEN** a data atual é 15/07/2026
- **AND** a "Loja de Ferramentas" cadastrou a data "Aniversário da cidade" em 01/08
- **THEN** `datasComemorativas` contém, nesta ordem, "Aniversário da cidade" (2026-08-01, 17 dias, `personalizada` verdadeiro) e "Dia dos Pais" (2026-08-09, 25 dias)

#### Scenario: Limite de 30 dias

- **WHEN** a data atual é 10/07/2026
- **THEN** o "Dia dos Pais" (09/08/2026, 30 dias) é uma data próxima
- **AND** com a data atual 09/07/2026 (31 dias) não é

### Requirement: Sugestão de reforço de estoque

Quando houver data comemorativa próxima, o sistema SHALL sugerir reforçar o estoque de cada produto cujas vendas nos últimos 30 dias (da data atual menos 29 dias até a data atual) superem o estoque atual, com um motivo em português que informe as vendas e o estoque.

Atende: RF09.

#### Scenario: Reforço da furadeira e da trena

- **WHEN** a data atual é 15/07/2026 e a "Loja de Ferramentas" tem o estoque e as vendas do estudo de caso (vendas em 30 dias: furadeira 6, jogo de chaves 9, martelo 5, trena 10, alicate 4 e caixa de ferramentas 0)
- **AND** envia `GET /api/v1/alertas`
- **THEN** `sugestoes` contém `REFORCAR_ESTOQUE` somente para a "Furadeira de impacto 650 W" (6 vendas e 5 em estoque) e a "Trena 5 m" (10 vendas e 4 em estoque)
- **AND** o motivo da furadeira é "Vendeu 6 unidades nos últimos 30 dias e tem 5 em estoque."

### Requirement: Sugestão de promoção

Quando houver data comemorativa próxima, o sistema SHALL sugerir promoção para cada produto sem vendas nos últimos 30 dias e com estoque atual maior que zero, com um motivo em português que informe o estoque.

Atende: RF09.

#### Scenario: Promoção da caixa de ferramentas

- **WHEN** a data atual é 15/07/2026 e a "Loja de Ferramentas" tem o estoque e as vendas do estudo de caso
- **THEN** `sugestoes` contém `PROMOCAO` somente para a "Caixa de ferramentas plástica", com o motivo "Nenhuma venda nos últimos 30 dias, com 10 unidades em estoque."

### Requirement: Sugestão de reajuste de preço

Quando houver data comemorativa próxima e a precificação do MEI puder ser calculada, o sistema SHALL sugerir reajuste de preço para cada produto com vendas nos últimos 30 dias cujo preço médio de venda (valor vendido dividido pela quantidade, com 2 casas) seja menor que o preço sugerido, com um motivo em português que informe os dois preços. Sem precificação disponível, essa sugestão MUST ser omitida, sem afetar os demais alertas.

Atende: RF09.

#### Scenario: Furadeira vendida abaixo do preço sugerido

- **WHEN** a data atual é 15/07/2026 e a "Loja de Ferramentas" tem as despesas, a receita 14000.00 e a margem 25.00 do estudo de caso (preço sugerido da furadeira 300.00)
- **AND** registrou 11 furadeiras em estoque e vendeu 6 a 280.00 nos últimos 30 dias
- **THEN** `sugestoes` contém, para a furadeira, `REFORCAR_ESTOQUE` e `REAJUSTE_PRECO`, com o motivo do reajuste "Preço médio de venda de R$ 280,00 nos últimos 30 dias, abaixo do preço sugerido de R$ 300,00."

#### Scenario: Vendas no preço sugerido

- **WHEN** a "Loja de Ferramentas" tem os dados do estudo de caso, com as vendas feitas pelo preço sugerido de cada produto
- **THEN** `sugestoes` não contém `REAJUSTE_PRECO`

#### Scenario: Sem parâmetros financeiros

- **WHEN** a data atual é 15/07/2026 e a "Loja de Ferramentas" tem o estoque e as vendas do estudo de caso, mas não informou os parâmetros financeiros
- **AND** envia `GET /api/v1/alertas`
- **THEN** o sistema responde com status 200, com a ruptura da trena e as sugestões de reforço e promoção, e sem `REAJUSTE_PRECO`

### Requirement: Sugestões somente com data comemorativa próxima

O sistema MUST gerar sugestões de reforço, promoção e reajuste somente quando houver ao menos uma data comemorativa próxima. Sem data próxima, `sugestoes` SHALL ser uma lista vazia, e o alerta de ruptura continua sendo informado.

Atende: RF08, RF09.

#### Scenario: Sem data comemorativa próxima

- **WHEN** a data atual é 15/06/2026 (Dia dos Pais a 55 dias) e o MEI não tem datas personalizadas nos 30 dias seguintes
- **AND** os produtos têm o estoque e as vendas do estudo de caso
- **THEN** `datasComemorativas` e `sugestoes` são listas vazias
- **AND** `ruptura` contém a "Trena 5 m"

### Requirement: Alertas isolados por MEI

Os alertas MUST considerar somente os produtos, as movimentações, a precificação e as datas personalizadas do MEI do token, e o endpoint exige token.

Atende: RF10, RNF07.

#### Scenario: Cada MEI com os próprios alertas

- **WHEN** a "Loja de Ferramentas" tem o estudo de caso e a "Papelaria Central" tem o "Caderno universitário 200 folhas" com 2 unidades e estoque mínimo 10, e cadastrou a data "Volta às aulas" em 01/08
- **THEN** a ruptura da loja contém somente a trena, e a da papelaria somente o caderno
- **AND** "Volta às aulas" aparece só nos alertas da papelaria

#### Scenario: Alertas sem token

- **WHEN** um cliente envia `GET /api/v1/alertas` sem token
- **THEN** o sistema responde com status 401 no formato padrão de erro
