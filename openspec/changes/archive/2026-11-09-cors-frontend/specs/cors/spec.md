# Spec Delta

## Purpose

Permitir que o frontend web, servido em outra origem, acesse a API pelo navegador, aceitando somente as origens configuradas e mantendo a autenticação por token e o formato padronizado de erro.

## ADDED Requirements

### Requirement: Pré-checagem do navegador para origem permitida

Para uma origem configurada como permitida, o sistema SHALL responder à pré-checagem CORS (`OPTIONS` com `Access-Control-Request-Method`) sem exigir token, autorizando os métodos `GET`, `POST`, `PUT`, `DELETE` e `OPTIONS` e os cabeçalhos `Authorization` e `Content-Type`, com validade de 3600 segundos e sem permitir credenciais de cookie.

Atende: RF10, RF11.

#### Scenario: Pré-checagem do cadastro de produto

- **WHEN** o navegador envia `OPTIONS /api/v1/produtos` com `Origin: http://localhost:4200`, `Access-Control-Request-Method: POST` e `Access-Control-Request-Headers: authorization,content-type`, sem token
- **THEN** o sistema responde com status 200
- **AND** `Access-Control-Allow-Origin` é `http://localhost:4200`
- **AND** `Access-Control-Allow-Methods` contém `POST` e `Access-Control-Allow-Headers` contém `authorization` e `content-type`
- **AND** `Access-Control-Max-Age` é 3600 e não há `Access-Control-Allow-Credentials`

### Requirement: Respostas para origem permitida

As respostas a requisições de uma origem permitida, de sucesso ou de erro, MUST trazer `Access-Control-Allow-Origin` com essa origem, para que o frontend leia o corpo, inclusive o 401 de token ausente ou expirado. O cabeçalho `Location` SHALL ser exposto ao frontend.

Atende: RF10, RF11.

#### Scenario: Consulta da conta pelo frontend

- **WHEN** o frontend em `http://localhost:4200` envia `GET /api/v1/conta` com um token válido
- **THEN** o sistema responde com status 200 e `Access-Control-Allow-Origin` `http://localhost:4200`

#### Scenario: Token ausente lido pelo frontend

- **WHEN** o frontend em `http://localhost:4200` envia `GET /api/v1/conta` sem token
- **THEN** o sistema responde com status 401 em Problem Details
- **AND** a resposta tem `Access-Control-Allow-Origin` `http://localhost:4200`

#### Scenario: Location do cadastro visível ao frontend

- **WHEN** o frontend em `http://localhost:4200` cadastra um produto com `POST /api/v1/produtos`
- **THEN** o sistema responde com status 201 e `Access-Control-Expose-Headers` contém `Location`

### Requirement: Recusa de origem não permitida

Requisições CORS de origem não configurada MUST ser recusadas com status 403 no formato padrão de erro, com `title` e `detail` em português, sem cabeçalhos CORS de permissão e sem executar a operação.

Atende: RF10.

#### Scenario: Pré-checagem de origem desconhecida

- **WHEN** o navegador envia `OPTIONS /api/v1/produtos` com `Origin: http://site-desconhecido.com` e `Access-Control-Request-Method: POST`
- **THEN** o sistema responde com status 403 e tipo de conteúdo `application/problem+json`, com `title` "Origem não permitida"
- **AND** a resposta não tem `Access-Control-Allow-Origin`

#### Scenario: Cadastro a partir de origem desconhecida

- **WHEN** uma página em `http://site-desconhecido.com` envia `POST /api/v1/produtos` com um token válido e um produto válido
- **THEN** o sistema responde com status 403 em Problem Details
- **AND** nenhum produto é cadastrado

### Requirement: Origens configuráveis

As origens permitidas SHALL ser configuradas fora do código, por propriedade da aplicação, aceitando uma ou mais origens exatas. Sem configuração, MUST ser permitida somente `http://localhost:4200`.

Atende: RF11.

#### Scenario: Origem padrão de desenvolvimento

- **WHEN** a aplicação sobe sem configurar as origens
- **THEN** `http://localhost:4200` é permitida e `http://localhost:4300` é recusada com 403

#### Scenario: Duas origens configuradas

- **WHEN** a aplicação sobe com as origens `http://localhost:4200` e `https://app.valorizamei.com.br`
- **THEN** a pré-checagem das duas origens responde 200 com a respectiva origem em `Access-Control-Allow-Origin`

### Requirement: Requisições fora do navegador e da mesma origem

Requisições sem o cabeçalho `Origin` ou com a mesma origem da própria API (como o Swagger UI servido pela API) MUST ser atendidas como antes, sem recusa por CORS.

Atende: RF11.

#### Scenario: Cliente sem Origin

- **WHEN** um cliente envia `GET /api/v1/conta` com token válido e sem `Origin`
- **THEN** o sistema responde com status 200 e sem `Access-Control-Allow-Origin`

#### Scenario: Swagger UI na mesma origem

- **WHEN** o Swagger UI, servido em `http://localhost`, envia `POST /api/v1/auth/login` com `Origin: http://localhost` e credenciais válidas
- **THEN** o sistema responde com status 200
