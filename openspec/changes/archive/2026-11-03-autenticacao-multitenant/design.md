# Design

## Context

A `fundacao-api` entregou PostgreSQL 15 com Liquibase (ainda sem tabelas), Testcontainers, OpenAPI/Swagger restrito a `/api/v1/**` e o `TratadorGlobalDeErros` em Problem Details. O `spring-boot-starter-security` e o seu starter de teste estão comentados no `pom.xml`. O artigo (seção 4.4.5) define Spring Security com JWT e senhas em BCrypt, e determina que o identificador do MEI venha exclusivamente do token. A motivação está em proposal.md, e o comportamento esperado, nas specs `autenticacao`, `isolamento-tenant`, `erros-api` e `documentacao-api`.

## Goals / Non-Goals

**Goals:**

- Emitir e validar o JWT dentro da própria API, sem servidor de identidade externo.
- Oferecer às próximas features um único ponto de acesso ao tenant atual e um padrão de isolamento por `tenant_id` fácil de seguir e de testar.
- Manter as respostas 401 no mesmo formato Problem Details em português da `fundacao-api`.

**Non-Goals:**

- Refresh token, logout com revogação de token, recuperação de senha e confirmação de e-mail.
- Vários usuários por MEI, papéis e permissões (cada MEI tem um único usuário nesta etapa).
- Limite de tentativas de login (proteção contra força bruta).
- CORS para o frontend Angular (tratado junto com a integração do frontend).
- Direito ao esquecimento da LGPD (fora do escopo do Projeto Final II, seção 4.5 do artigo).

## Decisions

1. **JWT emitido pela API com o OAuth2 Resource Server do Spring Security, assinado com HS256.** O `spring-boot-starter-security-oauth2-resource-server` fornece a validação do token no filtro de segurança e o `NimbusJwtEncoder` para emiti-lo; a mesma chave simétrica assina e valida.
   - *Alternativa descartada:* servidor de identidade externo (Keycloak, Cognito), que adicionaria infraestrutura sem ganho para o escopo.
   - *Alternativa descartada:* biblioteca JWT avulsa com filtro próprio, que duplicaria o que o Spring Security já oferece.

2. **Conteúdo e validade do token.**
   - O token contém `iss` = `valoriza-mei`, `sub` = id do usuário, `tenant_id` = id do MEI, além de `iat` e `exp`.
   - A validade padrão é de 60 minutos, configurável por `valorizamei.seguranca.jwt.validade` (formato `Duration`).
   - O segredo vem de `valorizamei.seguranca.jwt.segredo`, alimentado pela variável de ambiente `VALORIZAMEI_JWT_SEGREDO`, com no mínimo 32 bytes. Um valor padrão de desenvolvimento fica no `application.yaml` e é documentado como inseguro para produção.
   - A emissão e a validação de `exp` usam o `Clock` injetável, para que os testes controlem o tempo.

3. **Cadeia de segurança stateless em `br.com.valorizamei.shared.seguranca`.**
   - Sem sessão HTTP e sem CSRF, porque a API é stateless e autenticada por cabeçalho.
   - Rotas públicas: `POST /api/v1/auth/cadastro`, `POST /api/v1/auth/login`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` e `/actuator/health`. Qualquer outra rota exige token.
   - Um `AuthenticationEntryPoint` próprio responde 401 em Problem Details, em português, com `instance` e `WWW-Authenticate: Bearer`. Um `AccessDeniedHandler` responde 403 no mesmo formato, para quando houver papéis.
   - Os textos ficam em `shared.erro`, junto com os demais.

4. **Tenant validado na autenticação, por meio de uma porta em `shared`.**
   - O conversor do JWT exige o claim `tenant_id` em formato UUID e consulta a interface `VerificadorDeTenant`, declarada em `shared.seguranca` e implementada pelo service da feature `autenticacao`, para confirmar que o MEI existe. Se faltar o claim ou o MEI não existir, a autenticação falha e a resposta é 401.
   - Assim o `shared` não depende da feature (inversão de dependência), e nenhuma feature recebe um tenant inexistente.
   - O custo é uma consulta por chave primária a cada requisição autenticada.

5. **Acesso ao tenant atual por `TenantAtual`.** Componente em `shared.seguranca` que devolve o UUID do MEI a partir do token autenticado. As próximas features usam somente ele; os DTOs não têm campo de tenant, e parâmetros de tenant em query, caminho ou corpo nunca são lidos.

6. **Padrão de isolamento explícito para as tabelas de negócio.**
   - Toda tabela de negócio terá `tenant_id uuid not null`, com chave estrangeira para `mei(id)` e índice.
   - O service define o `tenant_id` da entidade a partir de `TenantAtual`. Os repositories só expõem métodos filtrados por tenant (por exemplo, `findByIdAndTenantId` e `findAllByTenantId`).
   - Um registro de outro MEI é tratado como inexistente (404), sem revelar que existe.
   - Cada change de negócio inclui o seu teste de isolamento, como exige o `config.yaml`.
   - *Alternativa descartada:* multi-tenancy automática do Hibernate (`@TenantId`). Ela reduz código repetido, mas esconde o filtro, não cobre consultas nativas e é mais difícil de explicar e auditar no trabalho.

7. **Modelo de dados (changeset `001-criar-tabelas-mei-usuario`).**
   - `mei`: `id uuid` PK, `nome_negocio varchar(120) not null`, `criado_em timestamptz not null`.
   - `usuario`: `id uuid` PK, `tenant_id uuid not null` FK para `mei(id)`, `nome varchar(120) not null`, `email varchar(254) not null` com restrição única, `senha_hash varchar(100) not null`, `criado_em timestamptz not null`.
   - Os ids são UUID gerados pela aplicação (JPA `GenerationType.UUID`).
   - O e-mail é normalizado (`trim` e minúsculas) antes de gravar e de consultar.

8. **Feature `br.com.valorizamei.autenticacao`.**
   - Controllers: `AutenticacaoController` (`/api/v1/auth/cadastro` e `/api/v1/auth/login`) e `ContaController` (`GET /api/v1/conta`).
   - Services: cadastro, login e conta. O service de conta também implementa o `VerificadorDeTenant`.
   - Factory para conversão entre DTOs e entidades, repositories de `Mei` e `Usuario`, e DTOs de request e response.
   - O cadastro grava o MEI e o usuário na mesma transação.

9. **Senhas com `BCryptPasswordEncoder` (custo 10) e login sem vazamento de informação.**
   - A senha tem de 8 a 72 caracteres; 72 é o limite efetivo do BCrypt.
   - No login, quando o e-mail não existe, a senha é comparada mesmo assim com um hash fixo, para que o tempo de resposta não revele se o e-mail está cadastrado.
   - E-mail inexistente e senha errada geram a mesma resposta 401.

10. **Erros de negócio por `ErroDeNegocioException` em `shared.erro`.**
    - Ela estende `ErrorResponseException` e recebe status, título e detalhe em português. Exemplos: 401 "Credenciais inválidas" e 409 "E-mail já cadastrado".
    - O `ResponseEntityExceptionHandler` já trata `ErrorResponseException`, e o `TratadorGlobalDeErros` preserva títulos próprios. Por isso nenhum tratador novo é necessário, e o mesmo tipo atende às próximas features, por exemplo com 404 de recurso de outro tenant.
    - Uma violação da restrição única de e-mail em cadastros simultâneos também é convertida em 409.

11. **Documentação.**
    - O bean `OpenAPI` declara o esquema `bearerAuth` (`http`, `bearer`, `JWT`) como requisito global.
    - O cadastro e o login são marcados sem requisito de segurança.
    - O Swagger UI passa a exibir o botão "Authorize" para informar o token.

12. **Testes.**
    - Um apoio de teste em `src/test` cadastra uma conta, faz login e devolve o token. Ele é reutilizado pelos testes desta change e pelos testes de isolamento das próximas.
    - Os testes existentes de erros e de documentação passam a enviar o token.
    - O token expirado é gerado assinando um token com `exp` no passado. O token sem `tenant_id` ou de MEI inexistente é gerado com o mesmo encoder do sistema.

## Risks / Trade-offs

- [Segredo padrão versionado no `application.yaml`] → Uso só em desenvolvimento. O README orienta definir `VALORIZAMEI_JWT_SEGREDO`, e o deploy está fora do escopo.
- [Token não pode ser revogado antes de expirar] → Validade curta (60 minutos). A revogação fica para uma evolução futura.
- [Chave simétrica: quem valida também consegue emitir] → Aceitável, porque a própria API é a única emissora e validadora. Se houver outros serviços, migrar para RS256.
- [Uma consulta ao banco por requisição autenticada para verificar o MEI] → Consulta por chave primária. Se pesar no futuro, pode receber cache.
- [Esquecer o filtro por tenant em uma feature futura] → Repositories expõem só métodos filtrados, e cada change de negócio tem teste de isolamento obrigatório.
- [BREAKING: endpoints `/api/v1` passam a exigir token] → Ainda não há clientes da API; os testes existentes são atualizados nesta change.

## Migration Plan

- O changeset `001` cria `mei` e `usuario` na inicialização. Não há dados a migrar.
- Para reverter em desenvolvimento, basta recriar o banco (contêiner descartável) e remover o changeset.
- Ao subir com `spring-boot:test-run`, use o cadastro e o login pelo Swagger para obter o token.
