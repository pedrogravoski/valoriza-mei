# Spec Delta

## Purpose

Garantir que, na plataforma compartilhada por vários microempreendedores, cada requisição seja atribuída a um único MEI identificado pelo token e que cada MEI acesse somente os próprios dados.

## ADDED Requirements

### Requirement: MEI identificado exclusivamente pelo token

O sistema SHALL identificar o MEI (tenant) de cada requisição autenticada exclusivamente a partir do token de acesso. Identificadores de MEI ou de tenant enviados no corpo, na query ou no caminho MUST ser ignorados.

Atende: RF10, RNF07.

#### Scenario: Conta consultada a partir do token

- **WHEN** a "Loja de Ferramentas" (usuário "carlos@lojaferramentas.com.br") envia `GET /api/v1/conta` com o seu token
- **THEN** o sistema responde com status 200 com o nome do negócio "Loja de Ferramentas" e o e-mail "carlos@lojaferramentas.com.br"

#### Scenario: Tenant informado na query é ignorado

- **WHEN** existem as contas "Loja de Ferramentas" e "Papelaria Central"
- **AND** a "Loja de Ferramentas" envia `GET /api/v1/conta?tenantId=<identificador da Papelaria Central>` com o seu token
- **THEN** o sistema responde com os dados da "Loja de Ferramentas"

### Requirement: Acesso restrito aos dados do próprio MEI

Cada MEI MUST acessar somente os próprios dados: toda consulta ou alteração de dados de negócio SHALL considerar apenas os registros do MEI identificado no token, e o mesmo endpoint, chamado por MEIs diferentes, devolve os dados de cada um.

Atende: RF10, RNF07.

#### Scenario: Dois MEIs consultam a própria conta

- **WHEN** existem as contas "Loja de Ferramentas" (usuário "carlos@lojaferramentas.com.br") e "Papelaria Central" (usuário "ana@papelariacentral.com.br")
- **AND** cada uma envia `GET /api/v1/conta` com o próprio token
- **THEN** a "Loja de Ferramentas" recebe somente os seus dados
- **AND** a "Papelaria Central" recebe somente os seus dados

### Requirement: Token sem MEI válido é recusado

Uma requisição cujo token não identifique um MEI existente MUST ser recusada com status 401, sem acesso a dados de nenhum MEI.

Atende: RF10, RNF07.

#### Scenario: Token sem identificação do MEI

- **WHEN** um cliente envia `GET /api/v1/conta` com um token assinado pelo sistema que não contém o identificador do MEI
- **THEN** o sistema responde com status 401

#### Scenario: Token de MEI inexistente

- **WHEN** um cliente envia `GET /api/v1/conta` com um token assinado pelo sistema cujo identificador de MEI não corresponde a nenhuma conta
- **THEN** o sistema responde com status 401
