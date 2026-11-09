# Spec Delta

## ADDED Requirements

### Requirement: Documentação acessível sem autenticação

A especificação OpenAPI e a interface Swagger UI MUST continuar acessíveis sem token, mesmo com os endpoints da API protegidos.

Atende: RF11.

#### Scenario: Especificação e Swagger UI sem token

- **WHEN** um cliente sem token envia `GET /v3/api-docs` e `GET /swagger-ui/index.html`
- **THEN** o sistema responde com status 200 nas duas requisições

### Requirement: Esquema de autenticação documentado

A especificação OpenAPI SHALL declarar o esquema de autenticação Bearer com token JWT, exigido pelos endpoints protegidos, e SHALL indicar que o cadastro e o login não exigem autenticação, permitindo informar o token na interface Swagger UI para executar os endpoints protegidos.

Atende: RF10, RF11.

#### Scenario: Esquema Bearer declarado

- **WHEN** um cliente consulta `GET /v3/api-docs`
- **THEN** `components.securitySchemes` contém um esquema do tipo `http`, com `scheme` igual a `bearer` e `bearerFormat` igual a `JWT`
- **AND** a operação `GET /api/v1/conta` exige esse esquema

#### Scenario: Cadastro e login sem exigência de token na documentação

- **WHEN** um cliente consulta `GET /v3/api-docs`
- **THEN** as operações `POST /api/v1/auth/cadastro` e `POST /api/v1/auth/login` não exigem nenhum esquema de autenticação
