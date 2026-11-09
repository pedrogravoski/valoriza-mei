# Tasks

## 1. Configuração de CORS (RF10, RF11)

- [x] 1.1 Criar em `shared.seguranca` o record `PropriedadesCors` (`valorizamei.cors.origens-permitidas`) e incluir no `application.yaml` a propriedade com `${VALORIZAMEI_CORS_ORIGENS_PERMITIDAS:http://localhost:4200}`; verificar que o contexto sobe
- [x] 1.2 Criar o `ProcessadorDeCors` (estende `DefaultCorsProcessor` e repassa a recusa como `ErroDeNegocioException` 403 "Origem não permitida" ao `HandlerExceptionResolver`) e adicionar à `SegurancaConfig` o `CorsFilter` com a configuração do design (origens, métodos, cabeçalhos, `Location` exposto, sem credenciais, `maxAge` 3600, aplicado a `/api/**`) antes da autenticação; verificar que os testes existentes continuam passando

## 2. Testes de CORS (RF10, RF11)

- [x] 2.1 Teste do cenário "Pré-checagem do cadastro de produto": 200 sem token, com origem, métodos, cabeçalhos e `Max-Age` da spec e sem `Allow-Credentials`
- [x] 2.2 Testes dos cenários "Consulta da conta pelo frontend" (200 com `Allow-Origin`) e "Token ausente lido pelo frontend" (401 em Problem Details com `Allow-Origin`)
- [x] 2.3 Teste do cenário "Location do cadastro visível ao frontend": 201 com `Location` em `Access-Control-Expose-Headers`
- [x] 2.4 Testes dos cenários "Pré-checagem de origem desconhecida" e "Cadastro a partir de origem desconhecida": 403 `application/problem+json` com `title` "Origem não permitida", sem `Allow-Origin`, e nenhum produto gravado
- [x] 2.5 Teste do cenário "Origem padrão de desenvolvimento": `http://localhost:4200` aceita e `http://localhost:4300` recusada
- [x] 2.6 Teste do cenário "Duas origens configuradas" com `@TestPropertySource`
- [x] 2.7 Testes dos cenários "Cliente sem Origin" e "Swagger UI na mesma origem"

## 3. Documentação e fechamento

- [x] 3.1 Atualizar o README (estado, configuração de `VALORIZAMEI_CORS_ORIGENS_PERMITIDAS`, problemas comuns de CORS e total de testes)
- [x] 3.2 Executar `mvnw.cmd verify` com todos os testes passando, incluindo os de arquitetura
- [x] 3.3 Verificar manualmente com a API em execução: pré-checagem com `curl -X OPTIONS` da origem `http://localhost:4200` (200 com cabeçalhos CORS) e de outra origem (403 em Problem Details)

## Workflow follow-up

- Arquivar a change com `openspec archive cors-frontend` após a revisão.
- Confirmar que a spec `cors` foi criada em `openspec/specs/`.
