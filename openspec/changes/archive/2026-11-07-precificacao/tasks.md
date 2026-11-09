# Tasks

## 1. Fórmulas do markup e do preço (RF03, RF04)

- [x] 1.1 Criar em `br.com.valorizamei.precificacao.service` o `PrecificacaoService` com os métodos estáticos das fórmulas: soma dos percentuais, índice de markup exibido (4 casas, `HALF_UP`), preço sugerido em uma única divisão (`custo × 100 / (100 − soma)`, 2 casas, `HALF_UP`) e verificação da soma menor que 100; verificar com os testes unitários abaixo
- [x] 1.2 Teste unitário: soma 40 gera markup 1.6667, e o custo 180.00 gera PV 300.00 (e não 300.01)
- [x] 1.3 Teste unitário: com DF 33.3333333333, DV 0 e ML 25, o markup é 2.4000 e o custo 100.00 gera PV 240.00 (e não 239.98)
- [x] 1.4 Teste unitário: soma 100 e soma 105 são recusadas com 422 "Markup inviável" e detalhe com "100,00%" e "105,00%"; soma 99.99 é aceita

## 2. Consulta da precificação (RF03, RF04)

- [x] 2.1 Criar os DTOs `PrecificacaoResponse`, `PrecoSugeridoResponse` e `PrecificacaoDeProdutoResponse`, a `PrecificacaoFactory` e o `PrecificacaoController` com `GET /api/v1/precificacao` e `GET /api/v1/precificacao/produtos/{id}`; o service obtém produtos, DF, DV e margem pelos services das features `produto` e `despesa` (422 "Parâmetros financeiros não informados" sem receita ou margem); verificar que `ArquiteturaTest` continua passando
- [x] 2.2 Criar em `src/test` o apoio que monta o estudo de caso completo pela API (6 produtos, 5 despesas, receita 14000.00 e margem 25.00), reutilizando os apoios de produtos e despesas
- [x] 2.3 Teste do cenário "Markup da loja de ferramentas": DV 5.00, DF 10.00, ML 25.00 e markup 1.6667
- [x] 2.4 Teste do cenário "Markup sem despesas": DV 0.00, DF 0.00 e markup 1.3333
- [x] 2.5 Teste do cenário "Preços sugeridos do estudo de caso": os 6 produtos na ordem e com os preços da spec
- [x] 2.6 Testes dos cenários "Preço calculado com o markup sem arredondamento" (300.00) e "Preço calculado com o DF sem arredondamento" (markup 2.4000 e PV 240.00)
- [x] 2.7 Teste do cenário "MEI sem produtos": 200 com markup e lista vazia

## 3. Preço de um produto (RF04)

- [x] 3.1 Teste do cenário "Preço sugerido da furadeira": 200 com nome, custo 180.00, markup 1.6667 e PV 300.00
- [x] 3.2 Teste do cenário "Preço acompanha o novo custo": após alterar a trena para 15.00, o PV é 25.00
- [x] 3.3 Testes dos cenários "Produto inexistente na precificação" (404) e "Identificador inválido na precificação" (400)

## 4. Recusas com 422 (RF03)

- [x] 4.1 Testes dos cenários "Precificação sem parâmetros financeiros" (422 `application/problem+json` com o título da spec) e "Preço de um produto sem parâmetros financeiros"
- [x] 4.2 Testes dos cenários "Soma de exatamente 100%" (margem 85.00, detalhe com "100,00%") e "Soma acima de 100%" (margem 90.00, detalhe com "105,00%")

## 5. Isolamento entre MEIs (RF10, RNF07)

- [x] 5.1 Teste de isolamento, cenário "Cada MEI com o próprio markup": loja com 1.6667 e 6 produtos; papelaria com 1.2500 e o caderno a 20.00
- [x] 5.2 Teste de isolamento, cenário "Preço de produto de outro MEI": 404
- [x] 5.3 Teste do cenário "Precificação sem token": 401 em Problem Details

## 6. Documentação (RF03, RF04, RF11)

- [x] 6.1 Anotar o controller com `@Tag` "Precificação", `@Operation` com as fórmulas e `@ApiResponse` 422; teste em `DocumentacaoApiTest`: os dois caminhos aparecem na especificação, exigem `bearerAuth` e documentam a resposta 422
- [x] 6.2 Atualizar o README (estado, endpoints, exemplo de precificação pelo Swagger UI, problemas comuns com 422 e total de testes); verificar o roteiro com `mvnw.cmd spring-boot:test-run`

## 7. Fechamento

- [x] 7.1 Executar `mvnw.cmd verify` com todos os testes passando, incluindo os de arquitetura
- [x] 7.2 Registrar o teste de aceitação do RF03 e do RF04 (ação executada, resultado esperado, resultado obtido e se o requisito foi atendido) em `docs/testes-aceitacao.md`, incluindo o isolamento (RNF07)
- [x] 7.3 Verificar manualmente pela API em execução: estudo de caso completo, markup 1.6667, PV da furadeira 300.00, 422 sem parâmetros e com margem 85.00, e 404 para produto de outro MEI

## Workflow follow-up

- Arquivar a change com `openspec archive precificacao` após a revisão.
- Confirmar que a spec `precificacao` foi criada em `openspec/specs/`.
