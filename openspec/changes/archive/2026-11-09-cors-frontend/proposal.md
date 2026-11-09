# Proposal

## Why

O frontend do Valoriza MEI é uma aplicação Angular (seção 4.4 do artigo) servida em outra origem, `http://localhost:4200` no desenvolvimento, que consome a API em `http://localhost:8080`. Sem CORS, o navegador bloqueia todas as chamadas do frontend à API. Esta change libera, de forma controlada, as origens do frontend. É o último ajuste do backend antes da integração com o Angular (RF10, RF11).

## What Changes

- A API passa a responder às requisições CORS das origens permitidas, configuradas em `valorizamei.cors.origens-permitidas`. O padrão de desenvolvimento é `http://localhost:4200`, e outros ambientes informam a variável de ambiente `VALORIZAMEI_CORS_ORIGENS_PERMITIDAS`.
- A pré-checagem do navegador (`OPTIONS`) é respondida sem exigir token, com:
  - métodos `GET`, `POST`, `PUT`, `DELETE` e `OPTIONS`;
  - cabeçalhos `Authorization` e `Content-Type`;
  - validade de 1 hora.
- As respostas às origens permitidas, inclusive as de erro (401 de token ausente ou expirado), trazem os cabeçalhos CORS. Assim, o frontend consegue ler o erro e redirecionar para o login, como descreve o artigo.
- O cabeçalho `Location` dos cadastros (201) fica visível para o frontend.
- Requisições de uma origem não permitida são recusadas com 403 em Problem Details, em português, como os demais erros da API.
- Requisições sem o cabeçalho `Origin` (curl, testes) e da própria origem da API (Swagger UI) continuam iguais.

## Capabilities

### New Capabilities

- `cors`: acesso à API a partir das origens do frontend configuradas, com pré-checagem sem token, cabeçalhos expostos e recusa padronizada das demais origens (RF10, RF11).

### Modified Capabilities

Nenhuma. A recusa de origem já segue o requisito "Formato padronizado de erro" da spec `erros-api`.

## Impact

- `br.com.valorizamei.shared.seguranca`:
  - propriedades de CORS;
  - filtro de CORS na cadeia de segurança, com um processador que repassa a recusa ao tratamento de erros do Spring MVC (Problem Details).
- `application.yaml`: `valorizamei.cors.origens-permitidas`.
- `TratadorGlobalDeErros`: nada muda; a recusa usa a `ErroDeNegocioException`.
- Testes de integração de CORS com MockMvc.
- README: configuração das origens e problemas comuns de CORS.
