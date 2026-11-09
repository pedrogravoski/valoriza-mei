# Tasks

## 1. Ambiente e banco de dados (RNF05, RNF06)

- [x] 1.1 Declarar no `compose.yaml` o serviço `postgres:15` (banco, usuário e senha `valorizamei`, porta 5432)
- [x] 1.2 Reativar no `pom.xml` os starters `spring-boot-docker-compose`, `spring-boot-starter-data-jpa` e `spring-boot-starter-liquibase`, e adicionar o driver `org.postgresql:postgresql` (runtime)
- [x] 1.3 Configurar no `application.yaml`: `spring.jpa.hibernate.ddl-auto=validate`, `spring.jpa.open-in-view=false` e `spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml`
- [x] 1.4 Criar `db/changelog/db.changelog-master.yaml` e a pasta `db/changelog/changes/` para os changesets numerados das próximas changes

## 2. Testes de integração com PostgreSQL real (RNF04)

- [x] 2.1 Reativar no `pom.xml` os starters de teste `spring-boot-starter-data-jpa-test` e `spring-boot-starter-liquibase-test` e adicionar `org.testcontainers:testcontainers-postgresql` (test)
- [x] 2.2 Implementar `TestcontainersConfiguration` com contêiner PostgreSQL 15 anotado com `@ServiceConnection`
- [x] 2.3 Teste: ao iniciar o contexto, o Liquibase executa no PostgreSQL do contêiner (tabela `databasechangelog` existe) e o banco reporta versão 15
- [x] 2.4 Confirmar que `HealthCheckTest`, `ValorizaMeiApplicationTests` e os testes de arquitetura continuam passando com o banco ativo

## 3. Documentação da API (RF11)

- [x] 3.1 Configurar springdoc no `application.yaml`: `springdoc.api-docs.version=openapi_3_1` e `springdoc.paths-to-match=/api/v1/**`
- [x] 3.2 Criar em `br.com.valorizamei.shared.config` o bean de `OpenAPI` com título "Valoriza MEI API" e versão "v1"
- [x] 3.3 Teste do cenário "Consulta da especificação OpenAPI": `GET /v3/api-docs` retorna 200, `openapi` começa com "3.1", título e versão corretos
- [x] 3.4 Teste do cenário "Acesso à interface Swagger UI": `GET /swagger-ui/index.html` retorna 200 com HTML
- [x] 3.5 Teste do cenário "Atalho para a interface Swagger UI": `GET /swagger-ui.html` redireciona para a interface
- [x] 3.6 Teste do cenário "Endpoint de health não documentado": nenhum caminho em `paths` começa com `/actuator`
- [x] 3.7 Teste do cenário "Endpoints documentados usam o prefixo versionado": com o endpoint de teste da seção 4 registrado, todo caminho em `paths` começa com `/api/v1`

## 4. Tratamento padronizado de erros

- [x] 4.1 Habilitar `spring.mvc.problemdetails.enabled=true`
- [x] 4.2 Criar em `br.com.valorizamei.shared.erro` o `@RestControllerAdvice` (estendendo `ResponseEntityExceptionHandler`) com textos em português, propriedade `erros` para validação, `detail` próprio para corpo ilegível e tratamento genérico de 500 com log no servidor
- [x] 4.3 Criar em `src/test` o endpoint de teste `/api/v1/teste-erros` (aceita só `POST`, com DTO validado por `@NotBlank`/`@NotNull`/`@PositiveOrZero` e uma rota que lança exceção não tratada)
- [x] 4.4 Teste do cenário "Rota inexistente da API": `GET /api/v1/recurso-inexistente` retorna 404 `application/problem+json` com `status`, `title`, `detail` em português e `instance`
- [x] 4.5 Teste do cenário "Campos obrigatórios ausentes": 400 com dois itens em `erros`, cada um com `campo` e `mensagem`
- [x] 4.6 Teste do cenário "Valor fora do intervalo permitido": 400 com item em `erros` para o campo negativo
- [x] 4.7 Teste do cenário "JSON inválido": 400 com `detail` informando corpo inválido
- [x] 4.8 Teste do cenário "Método não aceito pelo endpoint": `DELETE /api/v1/teste-erros` retorna 405 em Problem Details
- [x] 4.9 Teste do cenário "Falha inesperada no processamento": 500 com `detail` genérico e sem a mensagem "falha de conexão interna", nome da exceção ou pilha no corpo

## 5. Fechamento

- [x] 5.1 Executar `mvnw.cmd verify` com todos os testes passando
- [x] 5.2 Registrar no README os pré-requisitos (Java 21, Docker Desktop) e como rodar a aplicação e os testes
- [x] 5.3 Isolamento entre tenants: não se aplica nesta change (não há dados de negócio); passa a ser exigido a partir da change `autenticacao-multitenant`
