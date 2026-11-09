# autenticacao Specification

## Purpose
Permitir que o MEI crie sua conta e se autentique na API do Valoriza MEI por meio de um token assinado, protegendo os endpoints que dão acesso aos dados do negócio.

## Requirements

### Requirement: Cadastro de conta do MEI

O sistema SHALL permitir, sem autenticação, o cadastro de uma conta pelo endpoint `POST /api/v1/auth/cadastro`, informando nome do negócio, nome do usuário, e-mail e senha. O cadastro cria o MEI e o seu usuário e responde com status 201, sem incluir a senha nem o hash na resposta.

Atende: RF10.

#### Scenario: Cadastro bem-sucedido da loja de ferramentas

- **WHEN** um cliente sem token envia `POST /api/v1/auth/cadastro` com nome do negócio "Loja de Ferramentas", nome "Carlos Silva", e-mail "carlos@lojaferramentas.com.br" e senha "Senha@2026"
- **THEN** o sistema responde com status 201
- **AND** o corpo contém o nome do negócio, o nome e o e-mail cadastrados
- **AND** o corpo não contém a senha nem o hash da senha

### Requirement: Validação dos dados de cadastro

O sistema MUST rejeitar o cadastro com status 400, no formato de erro de validação da API, quando faltar algum campo obrigatório, o e-mail for inválido ou a senha tiver menos de 8 ou mais de 72 caracteres.

Atende: RF10.

#### Scenario: Senha curta

- **WHEN** um cliente envia `POST /api/v1/auth/cadastro` com a senha "1234567"
- **THEN** o sistema responde com status 400
- **AND** o campo `erros` contém um item cujo `campo` é `senha`

#### Scenario: E-mail inválido e nome do negócio ausente

- **WHEN** um cliente envia `POST /api/v1/auth/cadastro` com o e-mail "carlos-sem-arroba" e sem o nome do negócio
- **THEN** o sistema responde com status 400
- **AND** o campo `erros` contém um item para o campo `email` e outro para o campo `nomeNegocio`

### Requirement: E-mail único por usuário

O e-mail SHALL identificar um único usuário em todo o sistema, sem diferenciar maiúsculas de minúsculas nem considerar espaços nas extremidades. Um cadastro com e-mail já existente MUST ser rejeitado com status 409 em Problem Details, sem criar MEI nem usuário.

Atende: RF10.

#### Scenario: E-mail já cadastrado com outra grafia

- **WHEN** já existe uma conta com o e-mail "carlos@lojaferramentas.com.br"
- **AND** um cliente envia `POST /api/v1/auth/cadastro` com o e-mail " Carlos@LojaFerramentas.com.br "
- **THEN** o sistema responde com status 409 no formato Problem Details, com `detail` em português informando que o e-mail já está cadastrado
- **AND** nenhum novo MEI ou usuário é criado

### Requirement: Senha armazenada somente como hash BCrypt

O sistema MUST armazenar a senha do usuário exclusivamente como hash BCrypt; a senha em texto puro MUST NOT ser gravada no banco nem devolvida em nenhuma resposta da API.

Atende: RNF01.

#### Scenario: Senha gravada como hash

- **WHEN** a conta de "carlos@lojaferramentas.com.br" é cadastrada com a senha "Senha@2026"
- **THEN** o valor gravado para a senha no banco é diferente de "Senha@2026" e está no formato BCrypt (prefixo `$2a$`, `$2b$` ou `$2y$`)
- **AND** o hash gravado é reconhecido como correspondente à senha "Senha@2026"

### Requirement: Login com emissão de token

O sistema SHALL autenticar, sem token prévio, pelo endpoint `POST /api/v1/auth/login` com e-mail e senha. Com credenciais corretas, responde com status 200 contendo o token de acesso (JWT assinado), o tipo "Bearer" e a validade do token em segundos.

Atende: RF10.

#### Scenario: Login bem-sucedido

- **WHEN** existe a conta de "carlos@lojaferramentas.com.br" com a senha "Senha@2026"
- **AND** um cliente envia `POST /api/v1/auth/login` com esse e-mail e essa senha
- **THEN** o sistema responde com status 200
- **AND** o corpo contém o token de acesso, o tipo "Bearer" e a validade em segundos maior que zero

#### Scenario: E-mail informado com outra grafia no login

- **WHEN** existe a conta de "carlos@lojaferramentas.com.br" com a senha "Senha@2026"
- **AND** um cliente envia `POST /api/v1/auth/login` com o e-mail "CARLOS@lojaferramentas.com.br" e a senha "Senha@2026"
- **THEN** o sistema responde com status 200 e devolve o token de acesso

### Requirement: Credenciais inválidas não revelam o motivo

Quando o e-mail não estiver cadastrado ou a senha estiver incorreta, o login MUST responder com status 401 em Problem Details, com a mesma mensagem em português nos dois casos e sem emitir token.

Atende: RF10.

#### Scenario: Senha incorreta

- **WHEN** existe a conta de "carlos@lojaferramentas.com.br" com a senha "Senha@2026"
- **AND** um cliente envia `POST /api/v1/auth/login` com esse e-mail e a senha "SenhaErrada1"
- **THEN** o sistema responde com status 401 no formato Problem Details, sem token no corpo

#### Scenario: E-mail não cadastrado

- **WHEN** um cliente envia `POST /api/v1/auth/login` com o e-mail "naoexiste@exemplo.com" e qualquer senha
- **THEN** o sistema responde com status 401
- **AND** o `title` e o `detail` são iguais aos do cenário de senha incorreta

### Requirement: Endpoints protegidos exigem token válido

Todo endpoint sob `/api/v1`, exceto o cadastro e o login, MUST exigir o envio de um token válido no cabeçalho `Authorization: Bearer <token>`. O sistema MUST recusar com status 401 a requisição sem token, com token de assinatura inválida, adulterado ou expirado.

Atende: RF10.

#### Scenario: Requisição sem token

- **WHEN** um cliente envia `GET /api/v1/conta` sem o cabeçalho `Authorization`
- **THEN** o sistema responde com status 401

#### Scenario: Token adulterado

- **WHEN** um cliente envia `GET /api/v1/conta` com um token obtido no login cujo conteúdo foi alterado após a emissão
- **THEN** o sistema responde com status 401

#### Scenario: Token expirado

- **WHEN** um cliente envia `GET /api/v1/conta` com um token assinado pelo sistema cuja validade já terminou
- **THEN** o sistema responde com status 401

#### Scenario: Token válido

- **WHEN** um cliente envia `GET /api/v1/conta` com o token obtido no login
- **THEN** o sistema responde com status 200

### Requirement: Endpoints públicos fora da API protegida

O cadastro, o login, a documentação da API (`/v3/api-docs` e Swagger UI) e o health da aplicação (`/actuator/health`) SHALL permanecer acessíveis sem token.

Atende: RF10, RF11.

#### Scenario: Health sem token

- **WHEN** um cliente envia `GET /actuator/health` sem token
- **THEN** o sistema responde com status 200

#### Scenario: Cadastro e login sem token

- **WHEN** um cliente sem token envia `POST /api/v1/auth/cadastro` e depois `POST /api/v1/auth/login` com dados válidos
- **THEN** o sistema responde com status 201 e 200, respectivamente, sem exigir autenticação
