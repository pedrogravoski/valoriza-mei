# Proposal

## Why

As funcionalidades de negócio do Valoriza MEI (produtos, despesas, precificação, estoque, fluxo de caixa e alertas) dependem de uma base comum que ainda não existe: banco PostgreSQL com schema versionado, testes de integração contra banco real, documentação OpenAPI da API e um formato único de erro. Construir essa base primeiro evita que cada feature resolva essas questões de forma diferente e atende aos requisitos RF11, RNF04, RNF05 e RNF06 do artigo (seção 4.2).

## What Changes

- Ambiente de desenvolvimento com PostgreSQL 15 em contêiner Docker, iniciado junto com a aplicação (RNF05).
- Persistência via JPA com o schema criado e evoluído exclusivamente por changesets Liquibase, com validação do schema na inicialização (RNF06).
- Testes de integração executados contra uma instância real de PostgreSQL 15 em contêiner (RNF04).
- Documentação da API no padrão OpenAPI 3.1, com interface Swagger UI, restrita aos endpoints versionados em `/api/v1` (RF11).
- Formato padronizado de resposta de erro para toda a API (Problem Details), com mensagens em português e sem exposição de detalhes internos.

## Capabilities

### New Capabilities

- `documentacao-api`: publicação da especificação OpenAPI e da interface Swagger UI dos endpoints versionados da API (RF11).
- `erros-api`: formato e conteúdo das respostas de erro da API, incluindo erros de validação, requisição malformada, recurso inexistente e erro interno.

### Modified Capabilities

<!-- Nenhuma: ainda não existem specs no projeto. -->

## Impact

- `pom.xml`: reativação dos starters de JPA, Liquibase e Docker Compose (e respectivos starters de teste) e inclusão do módulo PostgreSQL do Testcontainers e do driver PostgreSQL.
- `compose.yaml`: serviço PostgreSQL 15.
- `src/main/resources`: configuração de datasource, JPA (`ddl-auto=validate`), Liquibase (changelog mestre) e springdoc.
- `br.com.valorizamei.shared`: configuração da documentação OpenAPI e tratamento global de erros.
- Testes: `TestcontainersConfiguration` passa a fornecer o PostgreSQL; novos testes de integração para documentação, erros e migrações.
- Nenhum endpoint de negócio é criado nesta change; as próximas changes (autenticação, produtos, despesas etc.) passam a se apoiar nesta base.
