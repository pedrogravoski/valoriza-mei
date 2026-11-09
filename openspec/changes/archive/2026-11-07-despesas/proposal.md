# Proposal

## Why

O índice de markup (RF03) depende de três percentuais: as despesas variáveis (DV), as despesas fixas rateadas sobre a receita (DF) e a margem de lucro desejada (ML). Hoje o sistema só conhece os produtos e o custo direto. Esta change registra as despesas do MEI classificadas como fixas ou variáveis (RF02), guarda a receita mensal estimada e a margem de lucro, e calcula o rateio das despesas fixas (RF05). Assim a change de precificação (RF03, RF04) só precisa combinar esses valores com o custo de cada produto.

## What Changes

- Cadastro de despesas do MEI com descrição e classificação (RF02):
  - **fixa:** valor mensal em reais (ex.: aluguel 1.200,00);
  - **variável:** percentual sobre o preço de venda (ex.: taxa de cartão 3%).
- Listagem, consulta, alteração e exclusão das despesas, com filtro opcional pela classificação (RF02).
- Descrição de despesa única no MEI, sem diferenciar maiúsculas e minúsculas, para evitar que a mesma despesa seja somada duas vezes no rateio (RF02).
- Parâmetros financeiros do MEI, um registro por conta: receita mensal estimada e margem de lucro desejada, em percentual (RF03, RF05).
- Consulta do rateio (RF05): total das despesas fixas, receita mensal estimada, percentual das despesas fixas (DF = total das fixas / receita × 100) e soma dos percentuais das despesas variáveis (DV). No estudo de caso: fixas 1.400,00, receita 14.000,00, DF 10% e DV 5%.
- Despesas e parâmetros pertencem ao MEI do token; registros de outro MEI são tratados como inexistentes (RF10, RNF07).
- Tabelas `despesa` e `parametros_financeiros`, criadas por changeset Liquibase (RNF06).

## Capabilities

### New Capabilities

- `despesas`: cadastro, consulta, alteração e exclusão das despesas fixas e variáveis do MEI e cálculo do rateio das despesas fixas sobre a receita mensal estimada (RF02, RF05, RF10, RNF07).
- `parametros-financeiros`: receita mensal estimada e margem de lucro desejada de cada MEI, usadas no rateio e no cálculo do markup (RF03, RF05, RF10, RNF07).

### Modified Capabilities

Nenhuma.

## Impact

- Banco: changeset `003` com as tabelas `despesa` e `parametros_financeiros`.
- Novo pacote de feature `br.com.valorizamei.despesa` (controller, service, factory, repository, model, dto).
- Novos endpoints protegidos: `/api/v1/despesas`, `/api/v1/despesas/{id}`, `/api/v1/despesas/rateio` e `/api/v1/parametros-financeiros`.
- Testes: integração da feature com o cenário da loja de ferramentas, isolamento entre dois MEIs e limpeza das novas tabelas no `BancoDeTeste`.
- `docs/testes-aceitacao.md`: registro do teste de aceitação do RF02 e do RF05.
- README: estado, endpoints, exemplos no Swagger UI e total de testes.
