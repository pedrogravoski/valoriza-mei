# Testes de aceitação

Registro dos testes funcionais executados pelo autor para cada requisito (seção 4.6 do artigo): ação executada, resultado esperado, resultado obtido e se o requisito foi atendido. Os testes automatizados citados rodam com `mvnw.cmd verify`; as verificações manuais foram feitas com a aplicação em execução (`mvnw.cmd spring-boot:test-run`).

## RF10 — Autenticar os usuários e garantir que cada MEI acesse somente os próprios dados

Change: `autenticacao-multitenant`. Data: 03/11/2026.

| Ação executada | Resultado esperado | Resultado obtido | Atendido |
|---|---|---|---|
| `POST /api/v1/auth/cadastro` com "Loja de Ferramentas", "Carlos Silva", "carlos@lojaferramentas.com.br" e senha "Senha@2026" | 201 com os dados da conta, sem senha | 201 com nome do negócio, nome e e-mail, sem senha nem hash | Sim |
| Novo cadastro com " Carlos@LojaFerramentas.com.br " | 409, sem criar MEI nem usuário | 409 "E-mail já cadastrado"; contagens de `mei` e `usuario` inalteradas (`CadastroTest`) | Sim |
| `POST /api/v1/auth/login` com e-mail e senha corretos | 200 com token, tipo "Bearer" e validade | 200 com token, tipo "Bearer" e validade de 3600 segundos | Sim |
| Login com senha errada e com e-mail não cadastrado | 401 com a mesma mensagem nos dois casos, sem token | 401 "Credenciais inválidas" / "E-mail ou senha inválidos." nos dois casos (`LoginTest`) | Sim |
| `GET /api/v1/conta` sem token | 401 em Problem Details, em português | 401 "Não autenticado", `instance` `/api/v1/conta` e `WWW-Authenticate: Bearer` | Sim |
| `GET /api/v1/conta` com o token do login | 200 com os dados da Loja de Ferramentas | 200 com "Loja de Ferramentas", "Carlos Silva" e o e-mail cadastrado | Sim |
| `GET /api/v1/conta` com token adulterado, expirado, sem MEI e de MEI inexistente | 401 nos quatro casos | 401 nos quatro casos (`ContaEIsolamentoTest`) | Sim |

## RNF01 — Armazenar as senhas exclusivamente na forma de hash BCrypt

| Ação executada | Resultado esperado | Resultado obtido | Atendido |
|---|---|---|---|
| Cadastro com a senha "Senha@2026" e consulta da coluna `usuario.senha_hash` | Valor diferente da senha, no formato BCrypt e correspondente a ela | Hash com prefixo `$2a$`, diferente da senha e reconhecido pelo `PasswordEncoder` (`CadastroTest`) | Sim |

## RNF07 — Isolar os dados de cada MEI em uma arquitetura multi-tenant

| Ação executada | Resultado esperado | Resultado obtido | Atendido |
|---|---|---|---|
| "Loja de Ferramentas" e "Papelaria Central" consultam `GET /api/v1/conta` com os próprios tokens | Cada MEI recebe somente os próprios dados | Cada um recebe apenas o seu nome de negócio e e-mail (`ContaEIsolamentoTest`) | Sim |
| A loja envia `GET /api/v1/conta?tenantId=<id da Papelaria Central>` | O parâmetro é ignorado e a loja recebe os próprios dados | Resposta com os dados da Loja de Ferramentas (`ContaEIsolamentoTest`) | Sim |
| Token da loja com o `tenant_id` trocado pelo da papelaria após a emissão | 401, sem acesso aos dados da papelaria | 401 (assinatura inválida) (`ContaEIsolamentoTest`) | Sim |
