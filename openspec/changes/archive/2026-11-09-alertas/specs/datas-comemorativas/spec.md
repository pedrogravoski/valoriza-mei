# Spec Delta

## Purpose

Manter o calendário de datas comemorativas usado nos alertas: as datas fixas do varejo, calculadas para qualquer ano, e as datas personalizadas de cada MEI, com a próxima ocorrência de cada uma.

## ADDED Requirements

### Requirement: Datas comemorativas fixas

O sistema SHALL oferecer a todos os MEIs as datas fixas Dia das Mães (2º domingo de maio), Dia dos Pais (2º domingo de agosto), Dia das Crianças (12 de outubro), Black Friday (sexta-feira após a 4ª quinta-feira de novembro) e Natal (25 de dezembro), calculadas para qualquer ano. As datas fixas MUST NOT ser alteradas ou excluídas pelo MEI.

Atende: RF09.

#### Scenario: Calendário a partir de 15/07/2026

- **WHEN** a data atual é 15/07/2026
- **AND** a "Loja de Ferramentas", sem datas personalizadas, envia `GET /api/v1/datas-comemorativas`
- **THEN** o sistema responde com status 200 e as 5 datas fixas, nesta ordem e com estas próximas ocorrências: Dia dos Pais 2026-08-09, Dia das Crianças 2026-10-12, Black Friday 2026-11-27, Natal 2026-12-25 e Dia das Mães 2027-05-09
- **AND** cada uma tem `id` nulo e `personalizada` falso

#### Scenario: Datas fixas de 2027

- **WHEN** o sistema calcula as datas fixas de 2027
- **THEN** o Dia das Mães é 09/05/2027, o Dia dos Pais é 08/08/2027 e a Black Friday é 26/11/2027

### Requirement: Cadastro de data personalizada

O sistema SHALL permitir que o MEI autenticado cadastre uma data comemorativa própria, informando o nome, o dia e o mês. A data se repete todo ano, e a resposta MUST trazer o identificador, a próxima ocorrência a partir da data atual e os dias restantes.

Atende: RF09.

#### Scenario: Aniversário da cidade

- **WHEN** a data atual é 15/07/2026
- **AND** a "Loja de Ferramentas" envia `POST /api/v1/datas-comemorativas` com nome "Aniversário da cidade", dia 1 e mês 8
- **THEN** o sistema responde com status 201 e o cabeçalho `Location` com `/api/v1/datas-comemorativas/{id}`
- **AND** o corpo contém o `id`, `nome` "Aniversário da cidade", `dia` 1, `mes` 8, `proximaOcorrencia` 2026-08-01, `diasRestantes` 17 e `personalizada` verdadeiro

#### Scenario: Próxima ocorrência no ano seguinte

- **WHEN** a data atual é 15/07/2026
- **AND** a "Loja de Ferramentas" cadastra a data "Liquidação de verão" em 05/01
- **THEN** a `proximaOcorrencia` é 2027-01-05

#### Scenario: Data que cai hoje

- **WHEN** a data atual é 15/07/2026
- **AND** a "Loja de Ferramentas" cadastra a data "Aniversário da loja" em 15/07
- **THEN** a `proximaOcorrencia` é 2026-07-15 e `diasRestantes` é 0

### Requirement: Validação da data personalizada

O sistema MUST recusar com status 400, no formato padrão de erro com a lista de campos inválidos, a data personalizada sem nome, com nome acima de 80 caracteres, sem dia ou mês, com mês fora de 1 a 12 ou com dia que não exista em todos os anos naquele mês (por exemplo 31/04 e 29/02). Nada é gravado.

Atende: RF09.

#### Scenario: Campos obrigatórios ausentes

- **WHEN** a "Loja de Ferramentas" envia `POST /api/v1/datas-comemorativas` com o corpo `{}`
- **THEN** o sistema responde com status 400 e `erros` contém itens para `nome`, `dia` e `mes`

#### Scenario: Mês inválido

- **WHEN** a "Loja de Ferramentas" cadastra a data "Feira do bairro" com dia 10 e mês 13
- **THEN** o sistema responde com status 400 e `erros` contém um item para `mes`

#### Scenario: Dia que não existe no mês

- **WHEN** a "Loja de Ferramentas" cadastra a data "Feira do bairro" com dia 31 e mês 4
- **THEN** o sistema responde com status 400 e `erros` contém um item para `dia`

#### Scenario: 29 de fevereiro

- **WHEN** a "Loja de Ferramentas" cadastra a data "Feira do bairro" com dia 29 e mês 2
- **THEN** o sistema responde com status 400 e `erros` contém um item para `dia`

### Requirement: Nome de data personalizada único no MEI

O nome da data personalizada MUST ser único entre as datas personalizadas do mesmo MEI, sem diferenciar maiúsculas e minúsculas. Um cadastro ou alteração que repita o nome SHALL ser recusado com status 409 no formato padrão de erro.

Atende: RF09, RNF07.

#### Scenario: Data repetida com outra grafia

- **WHEN** a "Loja de Ferramentas" já tem a data "Aniversário da cidade"
- **AND** cadastra a data "ANIVERSÁRIO DA CIDADE" em 02/08
- **THEN** o sistema responde com status 409 com `title` e `detail` em português

### Requirement: Listagem do calendário

O sistema SHALL listar as datas fixas e as personalizadas do MEI autenticado ordenadas pela próxima ocorrência a partir da data atual, cada uma com identificador (nulo nas fixas), nome, dia, mês, próxima ocorrência, dias restantes e indicação de data personalizada.

Atende: RF09.

#### Scenario: Calendário com data personalizada

- **WHEN** a data atual é 15/07/2026
- **AND** a "Loja de Ferramentas" cadastrou "Aniversário da cidade" em 01/08
- **THEN** `GET /api/v1/datas-comemorativas` devolve 6 datas, começando por "Aniversário da cidade" (2026-08-01) e "Dia dos Pais" (2026-08-09)

### Requirement: Consulta, alteração e exclusão de data personalizada

O sistema SHALL permitir consultar (200), alterar com as mesmas validações do cadastro (200) e excluir (204) uma data personalizada do MEI autenticado pelo identificador. Um identificador que não corresponda a data personalizada do MEI MUST gerar status 404 no formato padrão de erro.

Atende: RF09.

#### Scenario: Mudança do dia do aniversário da cidade

- **WHEN** a "Loja de Ferramentas" altera "Aniversário da cidade" para dia 5 e mês 8
- **THEN** o sistema responde com status 200 com `dia` 5 e `proximaOcorrencia` 2026-08-05

#### Scenario: Exclusão de data personalizada

- **WHEN** a "Loja de Ferramentas" exclui "Aniversário da cidade"
- **THEN** o sistema responde com status 204
- **AND** a data não aparece mais no calendário nem nos alertas

#### Scenario: Data personalizada inexistente

- **WHEN** a "Loja de Ferramentas" envia `GET`, `PUT` com dados válidos ou `DELETE` em `/api/v1/datas-comemorativas/{id}` com um identificador que não corresponde a nenhuma data
- **THEN** o sistema responde com status 404 nas três requisições

### Requirement: Datas personalizadas isoladas por MEI

As datas personalizadas MUST pertencer ao MEI do token: um MEI MUST NOT ver, alterar ou excluir as datas de outro MEI (para ele, a data não existe: 404). Os endpoints exigem token.

Atende: RF10, RNF07.

#### Scenario: Data de outro MEI

- **WHEN** a "Papelaria Central" cadastrou "Volta às aulas" em 01/08
- **THEN** o calendário da "Loja de Ferramentas" não contém "Volta às aulas"
- **AND** `GET`, `PUT` e `DELETE` da loja em `/api/v1/datas-comemorativas/{id}` com o identificador dela respondem 404

#### Scenario: Calendário sem token

- **WHEN** um cliente envia `GET /api/v1/datas-comemorativas` sem token
- **THEN** o sistema responde com status 401 no formato padrão de erro
