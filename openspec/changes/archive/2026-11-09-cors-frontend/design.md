# Design

## Context

A cadeia de segurança (`SegurancaConfig`) é stateless, autentica por `Authorization: Bearer` e não tem CORS configurado. As recusas do Spring Security (401 e 403) já são repassadas, pela `RespostaDeAcessoRecusado`, ao `HandlerExceptionResolver` do Spring MVC, que as formata em Problem Details. A spec `erros-api` exige Problem Details em toda resposta de erro sob `/api/v1`. A recusa padrão de CORS do Spring (`DefaultCorsProcessor`) responde 403 com o texto "Invalid CORS request", fora desse formato. A motivação está em proposal.md, e o comportamento esperado, na spec `cors`.

## Goals / Non-Goals

**Goals:**

- Permitir que o Angular em `http://localhost:4200` chame todos os endpoints da API, inclusive lendo os erros 401 e o cabeçalho `Location`.
- Configurar as origens sem alterar código para cada ambiente.
- Manter o formato Problem Details também na recusa de origem.

**Non-Goals:**

- Cookies e credenciais do navegador (`Access-Control-Allow-Credentials`): o token vai no cabeçalho `Authorization`, guardado pelo frontend.
- Origens com curinga (`*` ou padrões): só origens exatas.
- Proteção contra CSRF, que não se aplica a uma API stateless autenticada por cabeçalho.
- Deploy e HTTPS (fora do escopo do Projeto Final II).

## Decisions

1. **Origens configuráveis por propriedade.**
   - O record `PropriedadesCors` (`@ConfigurationProperties("valorizamei.cors")`) tem `origensPermitidas` (lista).
   - No `application.yaml`: `origens-permitidas: ${VALORIZAMEI_CORS_ORIGENS_PERMITIDAS:http://localhost:4200}`. A variável aceita várias origens separadas por vírgula.
   - Lista vazia significa que nenhuma origem externa é aceita.
   - *Alternativa descartada:* `@CrossOrigin` em cada controller. Espalharia a regra e deixaria de fora as respostas do filtro de segurança (401).

2. **Configuração CORS.**
   - Origens exatas da propriedade.
   - Métodos `GET`, `POST`, `PUT`, `DELETE` e `OPTIONS`.
   - Cabeçalhos permitidos `Authorization` e `Content-Type`.
   - Cabeçalho exposto `Location`.
   - Sem credenciais.
   - `maxAge` de 3600 segundos, para o navegador não repetir a pré-checagem a cada chamada.
   - Aplicada a `/api/**`. A documentação (`/v3/api-docs`, Swagger UI) é usada na própria origem da API.

3. **Filtro de CORS na cadeia de segurança, antes da autenticação.**
   - Um `CorsFilter` é criado na `SegurancaConfig` e adicionado à cadeia antes dos filtros de autenticação. O `http.cors()` padrão não permite trocar o processador (decisão 4).
   - Por estar antes da autenticação:
     - a pré-checagem `OPTIONS` é respondida pelo filtro sem exigir token;
     - as respostas 401 e 403 seguintes já levam os cabeçalhos CORS, e o navegador entrega o erro ao frontend.
   - A configuração não é registrada como bean `CorsConfigurationSource`, para que nenhuma configuração automática aplique um segundo filtro.

4. **Recusa de origem em Problem Details.**
   - Um `ProcessadorDeCors` estende o `DefaultCorsProcessor`. Na recusa (origem, método ou cabeçalho não permitidos), em vez de escrever o texto padrão, ele repassa uma `ErroDeNegocioException` 403 "Origem não permitida" / "A origem da requisição não tem permissão para acessar a API." ao `HandlerExceptionResolver`, como já faz a `RespostaDeAcessoRecusado`.
   - Implementação: o `rejectRequest` lança uma exceção interna, que o `processRequest` captura, porque é ele que tem a requisição para preencher o `instance`.
   - *Alternativa descartada:* manter o texto padrão. Contraria a spec `erros-api`, embora o navegador não mostre o corpo ao frontend.

5. **Requisições sem `Origin` ou da mesma origem.** O processador só atua em requisições CORS: com `Origin` diferente do esquema, host e porta da própria API. Por isso curl, testes e o Swagger UI servido pela API não mudam.

6. **Testes.** Integração com MockMvc, enviando o cabeçalho `Origin`:
   - pré-checagem permitida e recusada;
   - requisição autenticada da origem permitida;
   - 401 com cabeçalhos CORS;
   - `Location` exposto;
   - origem não permitida em Problem Details;
   - requisição sem `Origin`;
   - mesma origem;
   - várias origens configuradas, com `@TestPropertySource`.

## Risks / Trade-offs

- [Origem de produção esquecida na configuração] → O README orienta definir `VALORIZAMEI_CORS_ORIGENS_PERMITIDAS`. Sem ela, só o `localhost:4200` é aceito, o que falha de forma visível, e não aberta.
- [Exceção interna usada para desviar a recusa do `DefaultCorsProcessor`] → Fica contida em uma classe pequena e coberta por teste. Evita reimplementar toda a validação de CORS.
- [Cache de pré-checagem de 1 hora] → Uma mudança nas origens leva até 1 hora para refletir no navegador já aberto. Aceitável para a configuração, que muda raramente.

## Migration Plan

- Sem mudanças no banco. Ao subir, a API já aceita o frontend em `http://localhost:4200`.
- Para reverter, basta remover o filtro da cadeia de segurança e a propriedade.
