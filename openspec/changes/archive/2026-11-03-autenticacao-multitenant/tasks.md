# Tasks

## 1. Banco de dados: MEI e usuário (RNF06, RNF07)

- [x] 1.1 Criar o changeset `db/changelog/changes/001-criar-tabelas-mei-usuario.yaml` com as tabelas `mei` e `usuario` (FK `tenant_id` → `mei(id)`, e-mail único) e incluí-lo no changelog mestre; verificar com `MigracoesBancoTest` que as tabelas existem após a migração
- [x] 1.2 Criar em `br.com.valorizamei.autenticacao.model` as entidades `Mei` e `Usuario` (ids UUID gerados pela aplicação) e os repositories em `autenticacao.repository`; verificar que o contexto sobe com `ddl-auto=validate` e que `ArquiteturaTest` continua passando
- [x] 1.3 Teste de integração: gravar dois usuários com o mesmo e-mail é rejeitado pela restrição única do banco

## 2. Segurança, token e tenant (RF10, RNF07)

- [x] 2.1 Reativar no `pom.xml` `spring-boot-starter-security` e `spring-boot-starter-security-test` e adicionar `spring-boot-starter-security-oauth2-resource-server`; verificar que `mvnw.cmd -DskipTests package` baixa as dependências e compila
- [x] 2.2 Criar em `shared.config` o bean `Clock` e, no `application.yaml`, as propriedades `valorizamei.seguranca.jwt.segredo` (`${VALORIZAMEI_JWT_SEGREDO:...}`) e `valorizamei.seguranca.jwt.validade` (60 minutos); verificar que o contexto sobe (os testes usam o segredo padrão de desenvolvimento)
- [x] 2.3 Criar em `shared.seguranca` o encoder/decoder JWT HS256 (validação de `exp` com o `Clock`), o emissor de token (`iss`, `sub`, `tenant_id`, `iat`, `exp`) e o `PasswordEncoder` BCrypt; teste unitário: o token emitido é decodificado com os claims esperados
- [x] 2.4 Criar em `shared.seguranca` a interface `VerificadorDeTenant`, o conversor que exige `tenant_id` válido e existente e o componente `TenantAtual`; implementar o verificador no service da feature `autenticacao`
- [x] 2.5 Configurar a cadeia de segurança stateless (sem sessão e sem CSRF), com as rotas públicas do design e todas as demais autenticadas, e o `AuthenticationEntryPoint` e o `AccessDeniedHandler` em Problem Details com textos em português em `shared.erro`
- [x] 2.6 Criar em `src/test` o apoio de autenticação (`ApoioDeAutenticacao`), já usando o cadastro e o login reais (ver 4.7)
- [x] 2.7 Teste do cenário "Endpoint protegido sem token": `GET /api/v1/conta` sem token retorna 401 `application/problem+json` com `title`/`detail` em português, `instance` `/api/v1/conta` e `WWW-Authenticate: Bearer`
- [x] 2.8 Teste do cenário "Rota inexistente sem token": 401 em Problem Details
- [x] 2.9 Teste do cenário "Health sem token": `GET /actuator/health` retorna 200
- [x] 2.10 Teste do cenário "Especificação e Swagger UI sem token": `GET /v3/api-docs` e `GET /swagger-ui/index.html` retornam 200
- [x] 2.11 Atualizar `TratadorGlobalDeErrosTest` para enviar o token do apoio de autenticação, incluindo o cenário modificado "Rota inexistente da API" (cliente autenticado recebe 404); verificar que os 8 testes passam
- [x] 2.12 Criar a classe `ErroDeNegocioException` (estende `ErrorResponseException`) em `shared.erro`; teste com o endpoint de teste: a exceção gera Problem Details com o status, o título e o detalhe informados

## 3. Cadastro de conta (RF10, RNF01)

- [x] 3.1 Criar os DTOs de cadastro (validações: campos obrigatórios, e-mail válido, senha de 8 a 72 caracteres), a factory, o service (normaliza o e-mail, grava MEI e usuário na mesma transação, hash BCrypt, 409 para e-mail duplicado, inclusive por violação da restrição única) e o endpoint `POST /api/v1/auth/cadastro`
- [x] 3.2 Teste do cenário "Cadastro bem-sucedido da loja de ferramentas": 201 com nome do negócio, nome e e-mail, sem senha nem hash no corpo
- [x] 3.3 Teste do cenário "Senha curta": 400 com item em `erros` para `senha`
- [x] 3.4 Teste do cenário "E-mail inválido e nome do negócio ausente": 400 com itens para `email` e `nomeNegocio`
- [x] 3.5 Teste do cenário "E-mail já cadastrado com outra grafia": 409 em Problem Details e a contagem de MEIs e usuários no banco não muda
- [x] 3.6 Teste do cenário "Senha gravada como hash": o valor no banco começa com `$2a$`, `$2b$` ou `$2y$`, é diferente da senha e corresponde a ela pelo `PasswordEncoder`

## 4. Login e validação do token (RF10)

- [x] 4.1 Criar os DTOs de login, o service (e-mail normalizado, comparação com hash fixo quando o e-mail não existe, mesma resposta 401 "Credenciais inválidas") e o endpoint `POST /api/v1/auth/login`, que devolve token, tipo "Bearer" e validade em segundos
- [x] 4.2 Teste do cenário "Login bem-sucedido": 200 com token, tipo "Bearer" e validade maior que zero
- [x] 4.3 Teste do cenário "E-mail informado com outra grafia no login": 200 com token
- [x] 4.4 Teste do cenário "Senha incorreta": 401 em Problem Details sem token no corpo
- [x] 4.5 Teste do cenário "E-mail não cadastrado": 401 com `title` e `detail` iguais aos da senha incorreta
- [x] 4.6 Teste do cenário "Cadastro e login sem token": 201 e 200 sem cabeçalho `Authorization`
- [x] 4.7 Passar o apoio de autenticação dos testes a usar o cadastro e o login reais; verificar que os testes dos grupos 2 e 3 continuam passando

## 5. Conta autenticada e isolamento entre tenants (RF10, RNF07)

- [x] 5.1 Criar o endpoint `GET /api/v1/conta`, que devolve nome do negócio, nome e e-mail do MEI obtido por `TenantAtual`, sem ler nenhum tenant da requisição
- [x] 5.2 Teste do cenário "Token válido": `GET /api/v1/conta` com o token do login retorna 200
- [x] 5.3 Teste do cenário "Conta consultada a partir do token": a "Loja de Ferramentas" recebe "Loja de Ferramentas" e "carlos@lojaferramentas.com.br"
- [x] 5.4 Teste de isolamento, cenário "Dois MEIs consultam a própria conta": "Loja de Ferramentas" e "Papelaria Central" recebem somente os próprios dados
- [x] 5.5 Teste de isolamento, cenário "Tenant informado na query é ignorado": `?tenantId=<id da Papelaria Central>` com o token da loja devolve os dados da loja
- [x] 5.6 Teste do cenário "Token adulterado": token do login com o payload alterado retorna 401
- [x] 5.7 Teste do cenário "Token expirado": token assinado com `exp` no passado retorna 401
- [x] 5.8 Teste do cenário "Token sem identificação do MEI": token assinado sem `tenant_id` retorna 401
- [x] 5.9 Teste do cenário "Token de MEI inexistente": token assinado com `tenant_id` aleatório retorna 401

## 6. Documentação da API (RF10, RF11)

- [x] 6.1 Declarar no bean `OpenAPI` o esquema `bearerAuth` (`http`, `bearer`, `JWT`) como requisito global e marcar cadastro e login sem requisito de segurança
- [x] 6.2 Teste do cenário "Esquema Bearer declarado": `components.securitySchemes` tem o esquema e `GET /api/v1/conta` o exige
- [x] 6.3 Teste do cenário "Cadastro e login sem exigência de token na documentação": as duas operações têm `security` vazio
- [x] 6.4 Atualizar `DocumentacaoApiTest` (o endpoint de teste passa a exigir token) e verificar que os 5 testes existentes continuam passando
- [x] 6.5 Atualizar o README: variável `VALORIZAMEI_JWT_SEGREDO`, como cadastrar, fazer login e usar o botão "Authorize" do Swagger UI; verificar o roteiro com `mvnw.cmd spring-boot:test-run`

## 7. Fechamento

- [x] 7.1 Executar `mvnw.cmd verify` com todos os testes passando, incluindo os de arquitetura
- [x] 7.2 Registrar o teste de aceitação do RF10, do RNF01 e do RNF07 (ação executada, resultado esperado, resultado obtido e se o requisito foi atendido) em `docs/testes-aceitacao.md`
- [x] 7.3 Verificar manualmente pelo Swagger UI: cadastro, login, "Authorize" com o token e `GET /api/v1/conta` com 200; sem token, 401

## Workflow follow-up

- Arquivar a change com `openspec archive autenticacao-multitenant` após a revisão.
- Confirmar que as specs `autenticacao` e `isolamento-tenant` foram criadas em `openspec/specs/` e que `erros-api` e `documentacao-api` foram atualizadas.
