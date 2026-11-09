# Spec Delta

## Purpose

Guardar, para cada MEI, a receita mensal estimada e a margem de lucro desejada, usadas no rateio das despesas fixas e no cálculo do índice de markup.

## ADDED Requirements

### Requirement: Informar receita mensal estimada e margem de lucro

O sistema SHALL permitir que o MEI autenticado informe a receita mensal estimada, em reais, e a margem de lucro desejada, em pontos percentuais (25.00 = 25%). A gravação cria os parâmetros na primeira vez e os substitui nas seguintes, e a resposta MUST trazer os valores gravados.

Atende: RF03, RF05.

#### Scenario: Parâmetros da loja de ferramentas

- **WHEN** a "Loja de Ferramentas" envia `PUT /api/v1/parametros-financeiros` com o seu token, receita mensal estimada 14000.00 e margem de lucro 25.00
- **THEN** o sistema responde com status 200 com `receitaMensalEstimada` 14000.00 e `margemDeLucro` 25.00

#### Scenario: Atualização da margem de lucro

- **WHEN** a "Loja de Ferramentas" já informou receita 14000.00 e margem 25.00
- **AND** envia `PUT /api/v1/parametros-financeiros` com receita 14000.00 e margem 30.00
- **THEN** o sistema responde com status 200 com `margemDeLucro` 30.00
- **AND** `GET /api/v1/parametros-financeiros` devolve a margem 30.00

### Requirement: Consulta dos parâmetros financeiros

O sistema SHALL devolver, com status 200, a receita mensal estimada e a margem de lucro do MEI autenticado. Enquanto o MEI não os tiver informado, os dois valores MUST vir nulos.

Atende: RF03, RF05.

#### Scenario: Consulta após informar os parâmetros

- **WHEN** a "Loja de Ferramentas" informou receita 14000.00 e margem 25.00
- **AND** envia `GET /api/v1/parametros-financeiros`
- **THEN** o sistema responde com status 200 com `receitaMensalEstimada` 14000.00 e `margemDeLucro` 25.00

#### Scenario: Parâmetros ainda não informados

- **WHEN** um MEI recém-cadastrado envia `GET /api/v1/parametros-financeiros`
- **THEN** o sistema responde com status 200 com `receitaMensalEstimada` nulo e `margemDeLucro` nulo

### Requirement: Validação dos parâmetros financeiros

O sistema MUST recusar com status 400, no formato padrão de erro com a lista de campos inválidos, parâmetros sem receita ou sem margem, com receita menor ou igual a zero, com margem negativa, maior ou igual a 100 ou com mais de 2 casas decimais em qualquer um dos valores. Os parâmetros gravados anteriormente não são alterados.

Atende: RF03, RF05.

#### Scenario: Campos obrigatórios ausentes

- **WHEN** a "Loja de Ferramentas" envia `PUT /api/v1/parametros-financeiros` com o corpo `{}`
- **THEN** o sistema responde com status 400 e `erros` contém itens para `receitaMensalEstimada` e `margemDeLucro`

#### Scenario: Receita zero

- **WHEN** a "Loja de Ferramentas" envia receita 0.00 e margem 25.00
- **THEN** o sistema responde com status 400 e `erros` contém um item para `receitaMensalEstimada`

#### Scenario: Margem de 100 ou mais

- **WHEN** a "Loja de Ferramentas" já informou receita 14000.00 e margem 25.00
- **AND** envia receita 14000.00 e margem 100.00
- **THEN** o sistema responde com status 400 e `erros` contém um item para `margemDeLucro`
- **AND** `GET /api/v1/parametros-financeiros` continua devolvendo a margem 25.00

#### Scenario: Margem zero é aceita

- **WHEN** a "Loja de Ferramentas" envia receita 14000.00 e margem 0.00
- **THEN** o sistema responde com status 200 com `margemDeLucro` 0.00

### Requirement: Parâmetros financeiros isolados por MEI

Os parâmetros financeiros MUST pertencer ao MEI do token: a consulta e a gravação SHALL ler e alterar somente os parâmetros desse MEI, e identificadores de MEI enviados no corpo ou na query MUST ser ignorados. Os endpoints exigem token.

Atende: RF10, RNF07.

#### Scenario: Cada MEI consulta os próprios parâmetros

- **WHEN** a "Loja de Ferramentas" informou receita 14000.00 e margem 25.00
- **AND** a "Papelaria Central" informou receita 6000.00 e margem 20.00
- **THEN** a consulta da loja devolve 14000.00 e 25.00
- **AND** a consulta da papelaria devolve 6000.00 e 20.00

#### Scenario: Gravação não altera outro MEI

- **WHEN** a "Loja de Ferramentas" informou receita 14000.00 e margem 25.00
- **AND** a "Papelaria Central" envia `PUT /api/v1/parametros-financeiros` com receita 6000.00, margem 20.00 e `tenantId` com o identificador da loja no corpo
- **THEN** a consulta da loja continua devolvendo 14000.00 e 25.00

#### Scenario: Parâmetros sem token

- **WHEN** um cliente envia `GET /api/v1/parametros-financeiros` sem token
- **THEN** o sistema responde com status 401 no formato padrão de erro
