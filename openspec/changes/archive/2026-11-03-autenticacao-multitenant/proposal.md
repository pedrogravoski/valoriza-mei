# Proposal

## Why

O Valoriza MEI é um SaaS em que vários microempreendedores compartilham a mesma aplicação. Antes de criar qualquer dado de negócio (produtos, despesas, estoque), cada requisição precisa identificar o MEI responsável e impedir o acesso aos dados de outro. Esta change implementa a autenticação com JWT e senhas em BCrypt e define o tenant de cada requisição, atendendo ao RF10, ao RNF01 e ao RNF07 do artigo (seções 4.2 e 4.4.5).

## What Changes

- Cadastro de conta: o MEI informa o nome do negócio, o próprio nome, o e-mail e a senha. O sistema cria o MEI (tenant) e o usuário, e a senha é armazenada somente como hash BCrypt (RF10, RNF01).
- Login por e-mail e senha, devolvendo um token JWT assinado com validade limitada (RF10).
- Consulta da conta autenticada, com os dados do usuário e do negócio obtidos a partir do token (RF10, RNF07).
- **BREAKING:** todos os endpoints sob `/api/v1` passam a exigir o token, exceto o cadastro e o login. Requisições sem token válido recebem 401 em Problem Details, em português (RF10).
- O identificador do MEI (tenant) é obtido exclusivamente do token e fica disponível às próximas features por um componente compartilhado. Valores de tenant enviados no corpo, na query ou no caminho são ignorados (RNF07).
- A documentação OpenAPI e o Swagger UI continuam públicos e passam a declarar o esquema de autenticação Bearer, permitindo testar os endpoints protegidos pelo Swagger (RF11).
- Primeiras tabelas do banco (`mei` e `usuario`), criadas por changeset Liquibase (RNF06).

## Capabilities

### New Capabilities

- `autenticacao`: cadastro de conta, login com emissão de JWT, armazenamento de senhas em BCrypt e exigência de token nos endpoints protegidos (RF10, RNF01).
- `isolamento-tenant`: identificação do MEI a partir do token e garantia de que cada MEI acessa somente os próprios dados (RF10, RNF07).

### Modified Capabilities

- `erros-api`: a rota inexistente passa a ser verificada com cliente autenticado (sem token, a resposta é 401), e a falta de autenticação ganha resposta própria em Problem Details.
- `documentacao-api`: a documentação permanece acessível sem token e passa a declarar o esquema de autenticação Bearer (JWT).

## Impact

- `pom.xml`: reativação de `spring-boot-starter-security` e `spring-boot-starter-security-test`; inclusão de `spring-boot-starter-security-oauth2-resource-server` (emissão e validação do JWT).
- Banco: changeset `001` com as tabelas `mei` e `usuario`.
- Novo pacote de feature `br.com.valorizamei.autenticacao` (controller, service, factory, repository, model, dto).
- `br.com.valorizamei.shared`: configuração de segurança, emissão e validação do token, acesso ao tenant atual, `Clock` injetável e respostas 401 em Problem Details.
- `application.yaml`: segredo de assinatura e validade do token configuráveis.
- Testes existentes que chamam `/api/v1` (erros e documentação) passam a enviar token.
- README: como obter o token e usá-lo no Swagger, e como configurar o segredo.
- As próximas changes (produtos, despesas etc.) passam a gravar `tenant_id` em todas as tabelas de negócio usando o tenant do token.
