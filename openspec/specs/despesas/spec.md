# despesas Specification

## Purpose
Registrar as despesas fixas e variáveis de cada MEI e calcular o rateio das despesas fixas sobre a receita mensal estimada, fornecendo os percentuais DF e DV usados no índice de markup.

## Requirements

### Requirement: Registro de despesa fixa

O sistema SHALL permitir que o MEI autenticado registre uma despesa fixa informando a descrição, o tipo `FIXA` e o valor mensal em reais. A resposta MUST trazer o identificador gerado, os dados gravados (com o percentual nulo) e a localização da nova despesa.

Atende: RF02.

#### Scenario: Aluguel da loja de ferramentas

- **WHEN** a "Loja de Ferramentas" envia `POST /api/v1/despesas` com o seu token, descrição "Aluguel", tipo `FIXA` e valor mensal 1200.00
- **THEN** o sistema responde com status 201 e o cabeçalho `Location` com `/api/v1/despesas/{id}`
- **AND** o corpo contém o `id`, `descricao` "Aluguel", `tipo` `FIXA`, `valorMensal` 1200.00 e `percentual` nulo

### Requirement: Registro de despesa variável

O sistema SHALL permitir que o MEI autenticado registre uma despesa variável informando a descrição, o tipo `VARIAVEL` e o percentual sobre o preço de venda, em pontos percentuais (3.00 = 3%). A resposta MUST trazer o identificador gerado e os dados gravados, com o valor mensal nulo.

Atende: RF02.

#### Scenario: Taxa de cartão da loja de ferramentas

- **WHEN** a "Loja de Ferramentas" envia `POST /api/v1/despesas` com descrição "Taxa de cartão", tipo `VARIAVEL` e percentual 3.00
- **THEN** o sistema responde com status 201
- **AND** o corpo contém `descricao` "Taxa de cartão", `tipo` `VARIAVEL`, `percentual` 3.00 e `valorMensal` nulo

### Requirement: Validação dos dados da despesa

O sistema MUST recusar com status 400, no formato padrão de erro com a lista de campos inválidos, o registro ou a alteração de despesa sem descrição, com descrição acima de 120 caracteres, sem tipo, com valor mensal ou percentual menor ou igual a zero ou com mais de 2 casas decimais, ou com percentual maior ou igual a 100. Nenhuma despesa é gravada ou alterada.

Atende: RF02.

#### Scenario: Campos obrigatórios ausentes

- **WHEN** a "Loja de Ferramentas" envia `POST /api/v1/despesas` com o corpo `{}`
- **THEN** o sistema responde com status 400 e `erros` contém itens para `descricao` e `tipo`

#### Scenario: Valor mensal com mais de 2 casas decimais

- **WHEN** a "Loja de Ferramentas" registra a despesa fixa "Aluguel" com valor mensal 1200.005
- **THEN** o sistema responde com status 400 e `erros` contém um item para `valorMensal` com a mensagem "informe o valor com no máximo 2 casas decimais"

#### Scenario: Percentual de 100 ou mais

- **WHEN** a "Loja de Ferramentas" registra a despesa variável "Taxa de cartão" com percentual 100.00
- **THEN** o sistema responde com status 400 e `erros` contém um item para `percentual`

#### Scenario: Tipo desconhecido

- **WHEN** a "Loja de Ferramentas" registra a despesa "Aluguel" com tipo `OUTRA`
- **THEN** o sistema responde com status 400 no formato padrão de erro

### Requirement: Valor conforme o tipo da despesa

A despesa fixa MUST ter valor mensal e MUST NOT ter percentual; a despesa variável MUST ter percentual e MUST NOT ter valor mensal. O descumprimento SHALL ser recusado com status 400, com o item de `erros` apontando o campo ausente ou o campo que não se aplica ao tipo.

Atende: RF02.

#### Scenario: Despesa fixa sem valor mensal

- **WHEN** a "Loja de Ferramentas" registra a despesa "Aluguel" com tipo `FIXA` e sem valor mensal
- **THEN** o sistema responde com status 400 e `erros` contém um item para `valorMensal`

#### Scenario: Despesa variável com valor mensal

- **WHEN** a "Loja de Ferramentas" registra a despesa "Custo de entrega" com tipo `VARIAVEL`, percentual 2.00 e valor mensal 50.00
- **THEN** o sistema responde com status 400 e `erros` contém um item para `valorMensal`

#### Scenario: Despesa variável sem percentual

- **WHEN** a "Loja de Ferramentas" registra a despesa "Custo de entrega" com tipo `VARIAVEL` e sem percentual
- **THEN** o sistema responde com status 400 e `erros` contém um item para `percentual`

### Requirement: Descrição de despesa única no MEI

A descrição da despesa MUST ser única entre as despesas do mesmo MEI, sem diferenciar maiúsculas e minúsculas. Um registro ou alteração que repita a descrição de outra despesa do mesmo MEI SHALL ser recusado com status 409 no formato padrão de erro. MEIs diferentes podem ter despesas com a mesma descrição.

Atende: RF02, RNF07.

#### Scenario: Aluguel registrado duas vezes

- **WHEN** a "Loja de Ferramentas" já tem a despesa "Aluguel"
- **AND** registra a despesa fixa "ALUGUEL" com valor mensal 1200.00
- **THEN** o sistema responde com status 409 com `title` e `detail` em português
- **AND** a loja continua com uma única despesa "Aluguel"

#### Scenario: Mesma descrição em MEIs diferentes

- **WHEN** a "Loja de Ferramentas" já tem a despesa "Aluguel"
- **AND** a "Papelaria Central" registra a despesa fixa "Aluguel" com valor mensal 900.00
- **THEN** o sistema responde com status 201

### Requirement: Listagem das despesas do MEI

O sistema SHALL listar as despesas do MEI autenticado ordenadas pela descrição, sem diferenciar maiúsculas e minúsculas, com filtro opcional pelo tipo (`FIXA` ou `VARIAVEL`). Sem despesas, a resposta MUST ser uma lista vazia com status 200.

Atende: RF02.

#### Scenario: Despesas da loja de ferramentas

- **WHEN** a "Loja de Ferramentas" registrou as despesas fixas "Aluguel" (1200.00), "Internet e telefone" (120.00) e "Honorários contábeis" (80.00) e as variáveis "Taxa de cartão" (3.00) e "Custo de entrega" (2.00)
- **AND** envia `GET /api/v1/despesas`
- **THEN** o sistema responde com status 200 e 5 despesas, nesta ordem: "Aluguel", "Custo de entrega", "Honorários contábeis", "Internet e telefone" e "Taxa de cartão"

#### Scenario: Somente as despesas fixas

- **WHEN** a "Loja de Ferramentas" tem as 5 despesas do estudo de caso
- **AND** envia `GET /api/v1/despesas?tipo=FIXA`
- **THEN** o sistema responde com "Aluguel", "Honorários contábeis" e "Internet e telefone"

#### Scenario: MEI sem despesas

- **WHEN** um MEI recém-cadastrado envia `GET /api/v1/despesas`
- **THEN** o sistema responde com status 200 e uma lista vazia

### Requirement: Consulta, alteração e exclusão de despesa

O sistema SHALL permitir consultar (200), alterar com as mesmas validações do registro (200, com os dados atualizados) e excluir (204, sem corpo) uma despesa do MEI autenticado pelo identificador. Um identificador que não corresponda a despesa do MEI MUST gerar status 404 no formato padrão de erro.

Atende: RF02.

#### Scenario: Reajuste do aluguel

- **WHEN** a "Loja de Ferramentas" envia `PUT /api/v1/despesas/{id}` para o "Aluguel" com tipo `FIXA` e valor mensal 1300.00
- **THEN** o sistema responde com status 200 com `valorMensal` 1300.00
- **AND** `GET /api/v1/despesas/{id}` devolve o valor alterado

#### Scenario: Troca de despesa fixa para variável

- **WHEN** a "Loja de Ferramentas" altera a despesa "Internet e telefone" para tipo `VARIAVEL` com percentual 1.00 e sem valor mensal
- **THEN** o sistema responde com status 200 com `tipo` `VARIAVEL`, `percentual` 1.00 e `valorMensal` nulo

#### Scenario: Exclusão dos honorários

- **WHEN** a "Loja de Ferramentas" envia `DELETE /api/v1/despesas/{id}` para "Honorários contábeis"
- **THEN** o sistema responde com status 204
- **AND** a consulta da mesma despesa responde 404

#### Scenario: Despesa inexistente

- **WHEN** a "Loja de Ferramentas" envia `GET`, `PUT` com dados válidos ou `DELETE` em `/api/v1/despesas/{id}` com um identificador que não corresponde a nenhuma despesa
- **THEN** o sistema responde com status 404 nas três requisições

### Requirement: Rateio das despesas fixas

O sistema SHALL calcular, para o MEI autenticado, o total mensal das despesas fixas, o percentual das despesas fixas sobre a receita mensal estimada (DF = total das fixas × 100 / receita) e a soma dos percentuais das despesas variáveis (DV), com 2 casas decimais (arredondamento meio para cima). Sem receita informada, a receita e o DF MUST vir nulos.

Atende: RF05.

#### Scenario: Rateio da loja de ferramentas

- **WHEN** a "Loja de Ferramentas" tem as 5 despesas do estudo de caso e a receita mensal estimada 14000.00
- **AND** envia `GET /api/v1/despesas/rateio`
- **THEN** o sistema responde com status 200 com `totalDespesasFixas` 1400.00, `receitaMensalEstimada` 14000.00, `percentualDespesasFixas` 10.00 e `percentualDespesasVariaveis` 5.00

#### Scenario: DF arredondado para exibição

- **WHEN** um MEI tem somente a despesa fixa "Aluguel" de 1000.00 e a receita mensal estimada 3000.00
- **AND** envia `GET /api/v1/despesas/rateio`
- **THEN** `percentualDespesasFixas` é 33.33

#### Scenario: Rateio sem receita informada

- **WHEN** a "Loja de Ferramentas" tem as 5 despesas do estudo de caso e ainda não informou os parâmetros financeiros
- **AND** envia `GET /api/v1/despesas/rateio`
- **THEN** o sistema responde com status 200 com `totalDespesasFixas` 1400.00, `percentualDespesasVariaveis` 5.00, `receitaMensalEstimada` nulo e `percentualDespesasFixas` nulo

#### Scenario: Rateio sem despesas

- **WHEN** um MEI sem despesas, com receita mensal estimada 14000.00, envia `GET /api/v1/despesas/rateio`
- **THEN** o sistema responde com `totalDespesasFixas` 0.00, `percentualDespesasFixas` 0.00 e `percentualDespesasVariaveis` 0.00

### Requirement: Despesas isoladas por MEI

Cada despesa MUST pertencer ao MEI do token de quem a registrou. Um MEI MUST NOT listar, consultar, alterar ou excluir despesas de outro MEI (para ele, a despesa não existe: 404), e o rateio SHALL considerar somente as despesas e a receita do próprio MEI. Os endpoints de despesas exigem token.

Atende: RF10, RNF07.

#### Scenario: Cada MEI lista somente as próprias despesas

- **WHEN** a "Loja de Ferramentas" registrou o "Aluguel" e a "Papelaria Central" registrou a "Conta de luz"
- **AND** cada uma envia `GET /api/v1/despesas` com o próprio token
- **THEN** a loja recebe somente o "Aluguel"
- **AND** a papelaria recebe somente a "Conta de luz"

#### Scenario: Despesa de outro MEI

- **WHEN** a "Papelaria Central" envia, com o próprio token, `GET`, `PUT` com dados válidos e `DELETE` em `/api/v1/despesas/{id}` com o identificador do "Aluguel" da loja
- **THEN** o sistema responde com status 404 nas três requisições
- **AND** o "Aluguel" da loja continua com valor mensal 1200.00

#### Scenario: Rateio considera só o próprio MEI

- **WHEN** a "Loja de Ferramentas" tem as 5 despesas do estudo de caso e receita 14000.00
- **AND** a "Papelaria Central" tem somente a despesa fixa "Conta de luz" de 300.00 e receita 6000.00
- **THEN** o rateio da loja tem `totalDespesasFixas` 1400.00 e `percentualDespesasFixas` 10.00
- **AND** o rateio da papelaria tem `totalDespesasFixas` 300.00, `percentualDespesasFixas` 5.00 e `percentualDespesasVariaveis` 0.00

#### Scenario: Despesas sem token

- **WHEN** um cliente envia `GET /api/v1/despesas` sem token
- **THEN** o sistema responde com status 401 no formato padrão de erro
