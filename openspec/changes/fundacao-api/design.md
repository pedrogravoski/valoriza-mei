# Design

## Context

O projeto tem hoje apenas a aplicação Spring Boot 4.1.1 (Java 21) com actuator restrito ao `health`, springdoc-openapi 3.1.0 no classpath sem configuração, testes de arquitetura com ArchUnit e uma `TestcontainersConfiguration` vazia. Os starters de JPA, Liquibase, Docker Compose e Security estão comentados no `pom.xml`, e o `compose.yaml` não declara serviços. O artigo (seções 4.4.3, 4.4.4, 4.4.6 e 4.4.7) define PostgreSQL 15, Liquibase, OpenAPI/Swagger UI, Testcontainers e Docker como base técnica.

## Goals / Non-Goals

**Goals:**

- Subir o PostgreSQL 15 automaticamente no ambiente de desenvolvimento, sem instalação manual.
- Garantir que o schema só exista via changesets Liquibase e que a aplicação falhe ao iniciar se as entidades divergirem do schema.
- Executar os testes de integração contra PostgreSQL 15 real em contêiner.
- Publicar OpenAPI 3.1 e Swagger UI apenas para `/api/v1/**`.
- Padronizar todas as respostas de erro da API em Problem Details, em português.

**Non-Goals:**

- Autenticação, JWT e multi-tenant (change `autenticacao-multitenant`).
- Qualquer tabela ou endpoint de negócio.
- Deploy em AWS, MiniStack e HTTPS (fora do escopo do Projeto Final II, seção 4.5 do artigo).

## Decisions

1. **PostgreSQL via `compose.yaml` + `spring-boot-docker-compose`.** O serviço `postgres:15` é declarado no `compose.yaml` e o Spring Boot o inicia e cria a conexão automaticamente ao rodar a aplicação. Credenciais fixas de desenvolvimento (`valorizamei`/`valorizamei`, banco `valorizamei`) ficam só no compose. *Alternativa descartada:* datasource manual no `application.yaml`, que duplicaria configuração e exigiria subir o contêiner à mão.

2. **Schema somente por Liquibase, com `ddl-auto=validate`.** Changelog mestre em `db/changelog/db.changelog-master.yaml`, incluindo arquivos individuais em `db/changelog/changes/` com nomes numerados (`001-...yaml`). Nesta change o mestre não tem changesets de negócio; a primeira tabela nasce na change de autenticação. `spring.jpa.open-in-view=false` para não manter conexão aberta durante a renderização da resposta.

3. **Testcontainers com `@ServiceConnection`.** A `TestcontainersConfiguration` declara um contêiner PostgreSQL 15 (módulo `testcontainers-postgresql`) anotado com `@ServiceConnection`; todos os testes `@SpringBootTest` que importam essa configuração usam o mesmo banco real e executam as migrações Liquibase. A classe `TestValorizaMeiApplication` continua permitindo rodar a aplicação localmente com o contêiner de teste.

4. **springdoc configurado por propriedades e um bean de `OpenAPI` em `shared`.** `springdoc.api-docs.version=openapi_3_1` e `springdoc.paths-to-match=/api/v1/**`; o título "Valoriza MEI API" e a versão "v1" ficam em um bean de configuração no pacote `br.com.valorizamei.shared.config`. *Alternativa descartada:* anotações `@OpenAPIDefinition` na classe da aplicação, que misturariam configuração de documentação com a classe de inicialização.

5. **Tratamento global de erros em `br.com.valorizamei.shared.erro`.** Um `@RestControllerAdvice` estendendo `ResponseEntityExceptionHandler` com `spring.mvc.problemdetails.enabled=true`:
   - sobrescreve o tratamento de validação de argumentos para adicionar a propriedade `erros` (lista de `{campo, mensagem}`);
   - sobrescreve o tratamento de corpo ilegível para `detail` "O corpo da requisição é inválido.";
   - trata `Exception` genérica com status 500, `detail` genérico e log do erro no servidor;
   - centraliza `title`/`detail` em português para 404, 405 e demais status tratados pela classe base.
   O pacote `shared` não depende de nenhuma feature, como exigem os testes de arquitetura.

6. **Endpoint de teste apenas no escopo de testes.** Os cenários de validação, 405 e 500 precisam de um endpoint; ele é criado em `src/test` (ex.: `/api/v1/teste-erros`) e importado só nos testes de erro. Como `ArquiteturaTest` não importa classes de teste, ele não interfere nas regras de arquitetura nem aparece na aplicação real.

## Risks / Trade-offs

- **Docker obrigatório para desenvolver e testar.** Sem Docker Desktop em execução, a aplicação não sobe e os testes de integração falham. É coerente com o RNF04/RNF05 e será documentado no README.
- **Compatibilidade springdoc 3.1.0 × Spring Boot 4.1.1.** Se a geração em OpenAPI 3.1 apresentar incompatibilidade, a versão do springdoc será ajustada antes de seguir; o teste de `/v3/api-docs` detecta o problema.
- **Change de autenticação precisa preservar esta base.** Ao ativar o Spring Security, `/v3/api-docs` e `/swagger-ui/**` devem continuar públicos e as respostas 401/403 também devem seguir Problem Details; isso será requisito explícito naquela change.
- **Testes mais lentos.** Subir o PostgreSQL em contêiner adiciona alguns segundos; mitigado pelo reaproveitamento do mesmo contexto Spring entre testes.
