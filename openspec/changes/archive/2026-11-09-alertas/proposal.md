# Proposal

## Why

Com o estoque e as vendas registrados, o sistema já tem os dados para avisar o MEI antes que o problema aconteça: produto prestes a faltar e datas comemorativas em que a procura muda. Esta change entrega os dois últimos requisitos funcionais do artigo: o alerta de ruptura (RF08) e o alerta de datas comemorativas próximas com sugestões de reposição, promoção ou reajuste de preço com base no giro de cada produto (RF09).

## What Changes

- Alerta de ruptura (RF08): produtos cujo estoque atual é menor ou igual ao estoque mínimo definido (mínimo maior que zero). No estudo de caso, só a "Trena 5 m" (4 ≤ 5).
- Calendário de datas comemorativas (RF09):
  - **datas fixas:** Dia das Mães (2º domingo de maio), Dia dos Pais (2º domingo de agosto), Dia das Crianças (12/10), Black Friday (sexta-feira após a 4ª quinta-feira de novembro) e Natal (25/12), calculadas para qualquer ano;
  - **datas personalizadas:** cada MEI cadastra as próprias (nome, dia e mês), que se repetem todo ano.
- Datas comemorativas próximas: as que ocorrem nos próximos 30 dias, contando a partir da data atual. No estudo de caso, com a data atual 15/07/2026, o Dia dos Pais (09/08/2026, aviso a partir de 10/07/2026).
- Sugestões com base no giro dos últimos 30 dias, quando houver data comemorativa próxima (RF09):
  - **reforçar estoque:** vendas maiores que o estoque atual (furadeira 6 > 5 e trena 10 > 4);
  - **promoção:** nenhuma venda e estoque maior que zero (caixa de ferramentas plástica);
  - **reajuste de preço:** preço médio das vendas abaixo do preço sugerido pela precificação.
- Consulta única de alertas: ruptura, datas comemorativas próximas e sugestões, calculados a cada consulta, sem tabela de alertas.
- Cadastro, listagem, alteração e exclusão das datas personalizadas do MEI. As datas fixas aparecem na listagem e não podem ser alteradas.
- Datas personalizadas e alertas isolados por MEI (RF10, RNF07).
- Tabela `data_comemorativa`, criada por changeset Liquibase (RNF06).

## Capabilities

### New Capabilities

- `alertas`: alerta de ruptura de estoque e alerta de datas comemorativas próximas com sugestões de reforço de estoque, promoção e reajuste de preço (RF08, RF09, RF10, RNF07).
- `datas-comemorativas`: calendário com as datas comemorativas fixas e as datas personalizadas de cada MEI, com a próxima ocorrência de cada uma (RF09, RF10, RNF07).

### Modified Capabilities

Nenhuma. As specs `estoque` e `precificacao` não mudam de comportamento: os alertas só leem os dados delas.

## Impact

- Banco: changeset `005` com a tabela `data_comemorativa`.
- Novos pacotes de feature:
  - `br.com.valorizamei.calendario`: datas fixas e personalizadas;
  - `br.com.valorizamei.alerta`: ruptura e sugestões, usando os services de `estoque`, `calendario` e `precificacao`.
- `EstoqueService`: novo método com as vendas por produto num período (quantidade e valor total), para o giro e o preço médio.
- `PrecificacaoService`: novo método que devolve a precificação sem recusar com 422, vazio quando o markup não pode ser calculado. Assim, o alerta de reajuste é omitido e os demais alertas continuam funcionando.
- Novos endpoints protegidos: `GET /api/v1/alertas` e `/api/v1/datas-comemorativas`.
- Testes: unitários das datas e das regras de sugestão, integração com o estudo de caso na data 15/07/2026 (`RelogioFixoConfiguration`) e isolamento entre dois MEIs. O `BancoDeTeste` passa a limpar a tabela nova.
- `docs/testes-aceitacao.md`: registro do teste de aceitação do RF08 e do RF09.
- README: estado, endpoints, exemplos, problemas comuns e total de testes.
