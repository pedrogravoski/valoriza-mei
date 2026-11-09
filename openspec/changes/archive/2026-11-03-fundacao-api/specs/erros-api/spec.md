# Spec Delta

## Purpose

Definir um formato único e previsível para as respostas de erro da API do Valoriza MEI, com mensagens em português e sem exposição de detalhes internos da aplicação.

## ADDED Requirements

### Requirement: Formato padronizado de erro

Toda resposta de erro (status 4xx ou 5xx) de requisições sob `/api/v1` SHALL usar o formato Problem Details (RFC 9457), com tipo de conteúdo `application/problem+json` e os campos `status`, `title`, `detail` e `instance`, com `title` e `detail` em português.

Atende: requisito transversal, base para RF01 a RF11 (sem RF ou RNF específico no artigo).

#### Scenario: Rota inexistente da API

- **WHEN** um cliente envia `GET /api/v1/recurso-inexistente`
- **THEN** o sistema responde com status 404 e tipo de conteúdo `application/problem+json`
- **AND** o corpo contém `status` 404, `title` e `detail` em português e `instance` igual a `/api/v1/recurso-inexistente`

### Requirement: Erros de validação detalhados por campo

Quando os dados enviados a um endpoint da API violarem suas regras de validação, o sistema SHALL responder com status 400 e incluir no corpo o campo `erros`, uma lista com um item por violação contendo `campo` (nome do campo) e `mensagem` (descrição do problema em português).

Atende: requisito transversal, base para RF01 a RF11 (sem RF ou RNF específico no artigo).

#### Scenario: Campos obrigatórios ausentes

- **WHEN** um cliente envia a um endpoint da API um corpo JSON sem dois campos obrigatórios
- **THEN** o sistema responde com status 400 no formato Problem Details
- **AND** o campo `erros` contém dois itens, cada um com o `campo` ausente e uma `mensagem` em português

#### Scenario: Valor fora do intervalo permitido

- **WHEN** um cliente envia a um endpoint da API um valor numérico negativo em um campo que exige valor maior ou igual a zero
- **THEN** o sistema responde com status 400
- **AND** o campo `erros` contém um item cujo `campo` é o campo informado

### Requirement: Corpo de requisição malformado

Quando o corpo de uma requisição à API não puder ser interpretado (JSON inválido ou valor incompatível com o tipo do campo), o sistema SHALL responder com status 400 no formato Problem Details, com `detail` informando que o corpo da requisição é inválido.

Atende: requisito transversal, base para RF01 a RF11 (sem RF ou RNF específico no artigo).

#### Scenario: JSON inválido

- **WHEN** um cliente envia a um endpoint da API um corpo com JSON sintaticamente inválido
- **THEN** o sistema responde com status 400 no formato Problem Details
- **AND** o `detail` informa, em português, que o corpo da requisição é inválido

### Requirement: Método HTTP não suportado

Quando um endpoint da API for chamado com um método HTTP que ele não aceita, o sistema SHALL responder com status 405 no formato Problem Details.

Atende: requisito transversal, base para RF01 a RF11 (sem RF ou RNF específico no artigo).

#### Scenario: Método não aceito pelo endpoint

- **WHEN** um cliente envia `DELETE` a um endpoint da API que aceita apenas `POST`
- **THEN** o sistema responde com status 405 no formato Problem Details

### Requirement: Erro interno sem exposição de detalhes

Quando ocorrer uma falha inesperada no processamento de uma requisição à API, o sistema SHALL responder com status 500 no formato Problem Details com uma mensagem genérica em português e MUST NOT expor no corpo da resposta rastreamento de pilha, nome de classe de exceção ou mensagem técnica da falha.

Atende: requisito transversal, base para RF01 a RF11 (sem RF ou RNF específico no artigo).

#### Scenario: Falha inesperada no processamento

- **WHEN** o processamento de uma requisição à API lança uma exceção não tratada com a mensagem técnica "falha de conexão interna"
- **THEN** o sistema responde com status 500 no formato Problem Details
- **AND** o `detail` contém apenas uma mensagem genérica em português
- **AND** o corpo da resposta não contém a mensagem técnica, o nome da classe da exceção nem rastreamento de pilha
