# Spec Delta

## MODIFIED Requirements

### Requirement: Formato padronizado de erro

Toda resposta de erro (status 4xx ou 5xx) de requisições sob `/api/v1` SHALL usar o formato Problem Details (RFC 9457), com tipo de conteúdo `application/problem+json` e os campos `status`, `title`, `detail` e `instance`, com `title` e `detail` em português.

Atende: requisito transversal, base para RF01 a RF11 (sem RF ou RNF específico no artigo).

#### Scenario: Rota inexistente da API

- **WHEN** um cliente autenticado envia `GET /api/v1/recurso-inexistente`
- **THEN** o sistema responde com status 404 e tipo de conteúdo `application/problem+json`
- **AND** o corpo contém `status` 404, `title` e `detail` em português e `instance` igual a `/api/v1/recurso-inexistente`

## ADDED Requirements

### Requirement: Falta de autenticação em Problem Details

Quando uma requisição a um endpoint protegido for recusada por falta de token válido, o sistema SHALL responder com status 401 no formato Problem Details, com `title` e `detail` em português, `instance` com o caminho da requisição e o cabeçalho `WWW-Authenticate` indicando o esquema Bearer.

Atende: RF10.

#### Scenario: Endpoint protegido sem token

- **WHEN** um cliente envia `GET /api/v1/conta` sem token
- **THEN** o sistema responde com status 401 e tipo de conteúdo `application/problem+json`
- **AND** o corpo contém `status` 401, `title` e `detail` em português e `instance` igual a `/api/v1/conta`
- **AND** a resposta contém o cabeçalho `WWW-Authenticate` com o esquema `Bearer`

#### Scenario: Rota inexistente sem token

- **WHEN** um cliente envia `GET /api/v1/recurso-inexistente` sem token
- **THEN** o sistema responde com status 401 no formato Problem Details, sem revelar se a rota existe
