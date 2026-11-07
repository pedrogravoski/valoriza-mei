# Spec Delta

## Purpose

Refletir no fluxo de caixa do MEI as compras e as vendas registradas no estoque e permitir consultar, por período, os lançamentos e os totais de entradas, saídas e saldo.

## ADDED Requirements

### Requirement: Compra lançada como saída de caixa

Cada entrada de estoque MUST gerar automaticamente, junto com a movimentação, um lançamento de saída no fluxo de caixa do MEI, na data da movimentação, com valor igual a quantidade × custo direto do produto e descrição "Compra: <quantidade> × <nome do produto>".

Atende: RF07.

#### Scenario: Compra de furadeiras no caixa

- **WHEN** a "Loja de Ferramentas" registra a entrada de 10 unidades da "Furadeira de impacto 650 W" (custo 180.00) em 01/07/2026
- **AND** consulta `GET /api/v1/fluxo-de-caixa?inicio=2026-07-01&fim=2026-07-31`
- **THEN** a lista contém um lançamento com `data` 2026-07-01, `tipo` `SAIDA`, `descricao` "Compra: 10 × Furadeira de impacto 650 W" e `valor` 1800.00

### Requirement: Venda lançada como entrada de caixa

Cada saída de estoque MUST gerar automaticamente, junto com a movimentação, um lançamento de entrada no fluxo de caixa do MEI, na data da movimentação, com valor igual a quantidade × valor unitário da venda e descrição "Venda: <quantidade> × <nome do produto>". Uma venda recusada não gera lançamento.

Atende: RF07.

#### Scenario: Venda de furadeiras no caixa

- **WHEN** a "Loja de Ferramentas" registra a venda de 2 furadeiras a 300.00 em 10/07/2026
- **THEN** o fluxo de caixa de julho de 2026 contém um lançamento com `data` 2026-07-10, `tipo` `ENTRADA`, `descricao` "Venda: 2 × Furadeira de impacto 650 W" e `valor` 600.00

#### Scenario: Venda recusada não altera o caixa

- **WHEN** a "Loja de Ferramentas" tem 4 trenas e tenta vender 5
- **THEN** a venda é recusada com 422
- **AND** o fluxo de caixa não contém lançamento dessa venda

### Requirement: Consulta do fluxo de caixa por período

O sistema SHALL devolver os lançamentos do MEI autenticado entre as datas de início e fim informadas (inclusive), em ordem cronológica, com o total de entradas, o total de saídas e o saldo (entradas menos saídas, que pode ser negativo), todos com 2 casas decimais. Sem período informado, MUST usar o mês da data atual.

Atende: RF07.

#### Scenario: Totais dos primeiros dez dias de julho

- **WHEN** a data atual é 15/07/2026
- **AND** a "Loja de Ferramentas" registrou a compra de 10 furadeiras em 01/07/2026 (1800.00), a venda de 2 furadeiras a 300.00 em 05/07/2026 e a venda de 1 furadeira a 300.00 em 12/07/2026
- **AND** envia `GET /api/v1/fluxo-de-caixa?inicio=2026-07-01&fim=2026-07-10`
- **THEN** o sistema responde com status 200 com `totalEntradas` 600.00, `totalSaidas` 1800.00, `saldo` -1200.00 e 2 lançamentos, primeiro o de 01/07 e depois o de 05/07

#### Scenario: Período padrão é o mês atual

- **WHEN** a data atual é 15/07/2026
- **AND** a "Loja de Ferramentas" registrou uma compra em 30/06/2026 e outra em 01/07/2026
- **AND** envia `GET /api/v1/fluxo-de-caixa` sem parâmetros
- **THEN** a resposta tem `inicio` 2026-07-01, `fim` 2026-07-31 e somente o lançamento de 01/07

#### Scenario: Período sem lançamentos

- **WHEN** a "Loja de Ferramentas" consulta um período sem lançamentos
- **THEN** o sistema responde com status 200 com `totalEntradas` 0.00, `totalSaidas` 0.00, `saldo` 0.00 e lista vazia

#### Scenario: Período invertido

- **WHEN** a "Loja de Ferramentas" envia `GET /api/v1/fluxo-de-caixa?inicio=2026-07-31&fim=2026-07-01`
- **THEN** o sistema responde com status 400 no formato padrão de erro

### Requirement: Fluxo de caixa isolado por MEI

O fluxo de caixa MUST conter somente os lançamentos gerados pelas movimentações do MEI do token, e os endpoints exigem token.

Atende: RF10, RNF07.

#### Scenario: Cada MEI consulta o próprio caixa

- **WHEN** a "Loja de Ferramentas" registrou a compra de 10 furadeiras (1800.00) e a "Papelaria Central" a compra de 50 cadernos de custo 16.00 (800.00), ambas em 01/07/2026
- **THEN** o fluxo de caixa de julho da loja tem `totalSaidas` 1800.00 e somente o lançamento das furadeiras
- **AND** o da papelaria tem `totalSaidas` 800.00 e somente o lançamento dos cadernos

#### Scenario: Fluxo de caixa sem token

- **WHEN** um cliente envia `GET /api/v1/fluxo-de-caixa` sem token
- **THEN** o sistema responde com status 401 no formato padrão de erro
