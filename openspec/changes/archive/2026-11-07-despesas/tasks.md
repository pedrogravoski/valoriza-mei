# Tasks

## 1. Banco de dados: despesas e parâmetros (RF02, RF05, RNF06, RNF07)

- [x] 1.1 Criar o changeset `db/changelog/changes/003-criar-tabelas-despesa-parametros.yaml` com as tabelas `despesa` (FK `tenant_id` → `mei(id)`, restrições `ck_despesa_tipo` e `ck_despesa_valor`, índice único `uk_despesa_tenant_descricao` em `(tenant_id, lower(descricao))`) e `parametros_financeiros` (PK e FK `tenant_id`, receita `> 0`, margem `>= 0` e `< 100`) e incluí-lo no changelog mestre; verificar com `MigracoesBancoTest` que as tabelas existem após a migração
- [x] 1.2 Testes de integração em `MigracoesBancoTest`: o banco recusa despesa fixa com percentual, despesa variável sem percentual, percentual 100, tipo desconhecido, descrição repetida no mesmo MEI com outra grafia e receita zero; aceita a mesma descrição em MEIs diferentes
- [x] 1.3 Incluir `despesa` e `parametros_financeiros` em `BancoDeTeste.limpar`, antes de `mei`; verificar que os testes de autenticação e de produtos continuam passando

## 2. Parâmetros financeiros (RF03, RF05)

- [x] 2.1 Criar a entidade `ParametrosFinanceiros`, o repository, os DTOs (validações do design), a factory, o `ParametrosFinanceirosService` (tenant de `TenantAtual`, cria ou substitui) e o `ParametrosFinanceirosController` com `GET` e `PUT /api/v1/parametros-financeiros`; verificar que o contexto sobe com `ddl-auto=validate` e que `ArquiteturaTest` continua passando
- [x] 2.2 Teste do cenário "Parâmetros da loja de ferramentas": 200 com receita 14000.00 e margem 25.00
- [x] 2.3 Teste do cenário "Atualização da margem de lucro": a margem passa a 30.00 e a consulta devolve o valor novo
- [x] 2.4 Testes dos cenários "Consulta após informar os parâmetros" e "Parâmetros ainda não informados" (200 com os dois valores nulos)
- [x] 2.5 Testes dos cenários "Campos obrigatórios ausentes", "Receita zero" e "Margem de 100 ou mais" (400 com o campo em `erros` e os parâmetros anteriores mantidos), e "Margem zero é aceita"

## 3. Registro e validação de despesas (RF02)

- [x] 3.1 Criar a entidade `Despesa` com o enum `ClassificacaoDespesa`, o `DespesaRepository` só com métodos filtrados por tenant, os DTOs `DespesaRequest` (com o enum `TipoDespesa` e a restrição de classe `@ValorConformeTipo`) e `DespesaResponse`, a factory, o `DespesaService` (409 para descrição repetida, inclusive por violação do índice único) e o `DespesaController` com `POST /api/v1/despesas`; verificar que `ArquiteturaTest` continua passando
- [x] 3.2 Teste do cenário "Aluguel da loja de ferramentas": 201 com `Location`, `valorMensal` 1200.00 e `percentual` nulo
- [x] 3.3 Teste do cenário "Taxa de cartão da loja de ferramentas": 201 com `percentual` 3.00 e `valorMensal` nulo
- [x] 3.4 Testes dos cenários "Campos obrigatórios ausentes", "Valor mensal com mais de 2 casas decimais" (com a mensagem própria), "Percentual de 100 ou mais" e "Tipo desconhecido", sem gravar despesa
- [x] 3.5 Testes dos cenários "Despesa fixa sem valor mensal", "Despesa variável com valor mensal" e "Despesa variável sem percentual": 400 com o item de `erros` no campo indicado
- [x] 3.6 Testes dos cenários "Aluguel registrado duas vezes" (409 e uma única despesa) e "Mesma descrição em MEIs diferentes" (201)

## 4. Listagem, consulta, alteração e exclusão (RF02)

- [x] 4.1 Implementar `GET /api/v1/despesas` (ordenado pela descrição, filtro opcional `tipo`) e `GET`, `PUT` e `DELETE /api/v1/despesas/{id}` (404 "Despesa não encontrada" fora do MEI); verificar com os testes abaixo
- [x] 4.2 Testes dos cenários "Despesas da loja de ferramentas" (5 despesas na ordem da spec), "Somente as despesas fixas" e "MEI sem despesas"
- [x] 4.3 Testes dos cenários "Reajuste do aluguel" e "Troca de despesa fixa para variável"
- [x] 4.4 Testes dos cenários "Exclusão dos honorários" e "Despesa inexistente" (404 em `GET`, `PUT` e `DELETE`)
- [x] 4.5 Teste: alterar o "Aluguel" para a descrição de outra despesa do MEI gera 409, e alterar mantendo a própria descrição com outra grafia não gera

## 5. Rateio das despesas fixas (RF05)

- [x] 5.1 Criar o `RateioService` (soma das fixas e das variáveis, DF com escala 10 e exposto sem arredondamento para a precificação) e `GET /api/v1/despesas/rateio` com os valores de 2 casas (`HALF_UP`); verificar com os testes abaixo
- [x] 5.2 Teste do cenário "Rateio da loja de ferramentas": total 1400.00, receita 14000.00, DF 10.00 e DV 5.00
- [x] 5.3 Teste do cenário "DF arredondado para exibição": 1000.00 sobre 3000.00 gera 33.33
- [x] 5.4 Testes dos cenários "Rateio sem receita informada" (receita e DF nulos) e "Rateio sem despesas" (zeros)
- [x] 5.5 Teste unitário do `RateioService`: o DF sem arredondamento de 1000.00 sobre 3000.00 tem mais de 2 casas (33.3333333333)

## 6. Isolamento entre MEIs (RF10, RNF07)

- [x] 6.1 Teste de isolamento, cenário "Cada MEI lista somente as próprias despesas"
- [x] 6.2 Teste de isolamento, cenário "Despesa de outro MEI": `GET`, `PUT` e `DELETE` da papelaria no "Aluguel" da loja retornam 404, e o aluguel mantém 1200.00
- [x] 6.3 Teste de isolamento, cenário "Rateio considera só o próprio MEI"
- [x] 6.4 Testes de isolamento dos parâmetros: cenários "Cada MEI consulta os próprios parâmetros" e "Gravação não altera outro MEI"
- [x] 6.5 Testes dos cenários "Despesas sem token" e "Parâmetros sem token": 401 em Problem Details

## 7. Documentação (RF02, RF05, RF11)

- [x] 7.1 Anotar os controllers com `@Tag` "Despesas" e "Parâmetros financeiros" e `@Operation` em português; teste em `DocumentacaoApiTest`: `/api/v1/despesas`, `/api/v1/despesas/{id}`, `/api/v1/despesas/rateio` e `/api/v1/parametros-financeiros` aparecem na especificação e exigem o esquema `bearerAuth`
- [x] 7.2 Atualizar o README (estado, endpoints, exemplos de despesa fixa, variável e parâmetros pelo Swagger UI, problemas comuns e total de testes); verificar o roteiro com `mvnw.cmd spring-boot:test-run`

## 8. Fechamento

- [x] 8.1 Executar `mvnw.cmd verify` com todos os testes passando, incluindo os de arquitetura
- [x] 8.2 Registrar o teste de aceitação do RF02 e do RF05 (ação executada, resultado esperado, resultado obtido e se o requisito foi atendido) em `docs/testes-aceitacao.md`, incluindo o isolamento (RNF07)
- [x] 8.3 Verificar manualmente pela API em execução: parâmetros da loja, registro das 5 despesas do estudo de caso, rateio com DF 10.00 e DV 5.00, alteração, exclusão e 404 para despesa de outro MEI

## Workflow follow-up

- Arquivar a change com `openspec archive despesas` após a revisão.
- Confirmar que as specs `despesas` e `parametros-financeiros` foram criadas em `openspec/specs/`.
