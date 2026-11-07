# Proposal

## Why

O produto é o dado central do Valoriza MEI: o cálculo do preço de venda (RF03, RF04), as movimentações de estoque (RF06, RF07) e os alertas (RF08, RF09) partem do cadastro de cada produto com o seu custo direto de aquisição. Com a autenticação multi-tenant pronta, esta change entrega o primeiro dado de negócio isolado por MEI, atendendo ao RF01 e aplicando pela primeira vez o padrão de isolamento definido para as tabelas de negócio (RNF07).

## What Changes

- Cadastro de produto com nome, custo direto de aquisição (em reais, com 2 casas decimais) e estoque mínimo (RF01). O estoque mínimo é informado já no cadastro porque é o limite usado pelo alerta de ruptura (RF08).
- Consulta da lista de produtos do MEI, ordenada pelo nome, e consulta de um produto pelo identificador (RF01).
- Alteração dos dados de um produto e exclusão de um produto (RF01).
- Nome do produto único dentro do mesmo MEI, sem diferenciar maiúsculas e minúsculas; MEIs diferentes podem ter produtos com o mesmo nome (RF01, RNF07).
- Cada produto pertence ao MEI do token. Produtos de outro MEI são tratados como inexistentes (404) na consulta, na alteração e na exclusão (RF10, RNF07).
- Tabela `produto`, criada por changeset Liquibase (RNF06).

O estoque atual não faz parte do cadastro: ele será o resultado das entradas e saídas registradas na change de estoque (RF06).

## Capabilities

### New Capabilities

- `produtos`: cadastro, consulta, alteração e exclusão dos produtos do MEI, com custo direto, estoque mínimo e isolamento por tenant (RF01, RF10, RNF07).

### Modified Capabilities

Nenhuma. O formato de erro (`erros-api`), a documentação (`documentacao-api`) e o isolamento (`isolamento-tenant`) já cobrem o comportamento geral; os cenários específicos de produtos ficam na nova spec.

## Impact

- Banco: changeset `002` com a tabela `produto` (`tenant_id` com chave estrangeira para `mei` e índice único por MEI e nome).
- Novo pacote de feature `br.com.valorizamei.produto` (controller, service, factory, repository, model, dto).
- Novos endpoints protegidos em `/api/v1/produtos`, documentados automaticamente no Swagger UI.
- Testes: novos testes de integração da feature, incluindo o teste de isolamento entre dois MEIs exigido pelo `config.yaml`; o apoio de autenticação dos testes é reutilizado.
- `docs/testes-aceitacao.md`: registro do teste de aceitação do RF01.
- README: tabela de estado e lista de endpoints atualizadas.
