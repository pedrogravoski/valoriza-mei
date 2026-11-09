# Spec Delta

## Purpose

Publicar a documentação da API do Valoriza MEI no padrão OpenAPI, permitindo consultar e executar os endpoints versionados de forma independente do frontend.

## ADDED Requirements

### Requirement: Especificação OpenAPI publicada

O sistema SHALL publicar a especificação da API no padrão OpenAPI 3.1, em formato JSON, no caminho `/v3/api-docs`, identificando a API pelo título "Valoriza MEI API" e pela versão "v1".

Atende: RF11.

#### Scenario: Consulta da especificação OpenAPI

- **WHEN** um cliente envia `GET /v3/api-docs`
- **THEN** o sistema responde com status 200 e conteúdo JSON
- **AND** o campo `openapi` começa com "3.1"
- **AND** o campo `info.title` é "Valoriza MEI API" e o campo `info.version` é "v1"

### Requirement: Interface Swagger UI disponível

O sistema SHALL disponibilizar a interface Swagger UI, construída a partir da especificação OpenAPI publicada, para listar e executar os endpoints da API.

Atende: RF11.

#### Scenario: Acesso à interface Swagger UI

- **WHEN** um cliente envia `GET /swagger-ui/index.html`
- **THEN** o sistema responde com status 200 e conteúdo HTML

#### Scenario: Atalho para a interface Swagger UI

- **WHEN** um cliente envia `GET /swagger-ui.html`
- **THEN** o sistema redireciona o cliente para a interface Swagger UI

### Requirement: Documentação restrita à API versionada

A especificação OpenAPI MUST conter somente endpoints sob o prefixo `/api/v1`; endpoints de gerenciamento da aplicação, como os do actuator, MUST NOT aparecer na documentação.

Atende: RF11.

#### Scenario: Endpoint de health não documentado

- **WHEN** um cliente consulta `GET /v3/api-docs`
- **THEN** nenhum caminho listado em `paths` começa com `/actuator`

#### Scenario: Endpoints documentados usam o prefixo versionado

- **WHEN** a aplicação possui endpoints de API publicados
- **THEN** todo caminho listado em `paths` da especificação começa com `/api/v1`
