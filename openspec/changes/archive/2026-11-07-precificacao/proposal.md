# Proposal

## Why

A precificação por markup é o núcleo do Valoriza MEI: muitos microempreendedores definem o preço de venda sem considerar as despesas e a margem desejada. Com os produtos (custo direto, RF01), as despesas, o rateio (DV e DF, RF02 e RF05) e a margem de lucro (ML) já registrados, falta combinar esses valores. Esta change calcula o índice de markup (RF03) e o preço de venda sugerido de cada produto (RF04), com as fórmulas da seção 2.2 do artigo.

## What Changes

- Cálculo do índice de markup do MEI (RF03): `Markup = 100 / [100 - (DV + DF + ML)]`, com DV e DF do rateio e ML dos parâmetros financeiros. O markup é exibido com 4 casas decimais.
- Cálculo do preço de venda sugerido de cada produto (RF04): `PV = Custo Direto × Markup`.
  - O PV usa o markup e o DF sem arredondamento e é arredondado só ao final, para 2 casas (meio para cima).
  - No estudo de caso, a furadeira de custo 180,00 tem markup 1,6667 e PV 300,00.
- Consulta da precificação do MEI: os percentuais usados, o índice de markup e a lista de produtos com custo direto e preço sugerido (RF03, RF04).
- Consulta da precificação de um produto pelo identificador (RF04).
- Recusa com 422, em Problem Details em português, quando o markup não pode ser calculado (RF03):
  - os parâmetros financeiros (receita mensal estimada e margem) não foram informados;
  - a soma DV + DF + ML é de 100% ou mais.
- Precificação isolada por MEI: cada MEI só calcula com os próprios produtos, despesas e parâmetros (RF10, RNF07).

Nenhuma tabela nova: o preço é calculado a cada consulta, a partir dos dados atuais.

## Capabilities

### New Capabilities

- `precificacao`: cálculo do índice de markup e do preço de venda sugerido dos produtos do MEI, a partir do custo direto, das despesas rateadas e da margem de lucro (RF03, RF04, RF10, RNF07).

### Modified Capabilities

Nenhuma. As specs `produtos`, `despesas` e `parametros-financeiros` não mudam: a precificação só lê os dados delas.

## Impact

- Novo pacote de feature `br.com.valorizamei.precificacao` (controller, service, factory e dto, sem repository nem model).
- A precificação acessa as features `produto` e `despesa` somente pelos services e DTOs delas, como exigem as regras de arquitetura.
- Novos endpoints protegidos: `GET /api/v1/precificacao` e `GET /api/v1/precificacao/produtos/{id}`.
- Testes: integração com o estudo de caso completo (6 produtos, 5 despesas, receita e margem), casos de 422, isolamento entre dois MEIs e testes unitários das fórmulas.
- `docs/testes-aceitacao.md`: registro do teste de aceitação do RF03 e do RF04.
- README: estado, endpoints, exemplo pelo Swagger UI, problemas comuns e total de testes.
