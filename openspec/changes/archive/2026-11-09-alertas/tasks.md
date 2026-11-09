# Tasks

## 1. Banco de dados: datas personalizadas (RF09, RNF06, RNF07)

- [x] 1.1 Criar o changeset `db/changelog/changes/005-criar-tabela-data-comemorativa.yaml` com a tabela `data_comemorativa` (FK `tenant_id` → `mei(id)`, `ck_data_comemorativa_dia_mes` e índice único `uk_data_comemorativa_tenant_nome` em `(tenant_id, lower(nome))`) e incluí-lo no changelog mestre; verificar com `MigracoesBancoTest` que a tabela existe após a migração
- [x] 1.2 Testes de integração em `MigracoesBancoTest`: o banco recusa mês 13 e nome repetido no mesmo MEI com outra grafia
- [x] 1.3 Incluir `data_comemorativa` em `BancoDeTeste.limpar`, antes de `mei`; verificar que os testes existentes continuam passando

## 2. Calendário: datas fixas e próxima ocorrência (RF09)

- [x] 2.1 Criar em `br.com.valorizamei.calendario.service` o `CalendarioService` com os métodos estáticos das datas fixas de um ano, da próxima ocorrência de um dia/mês a partir de uma data e da janela de 30 dias; verificar com os testes unitários abaixo
- [x] 2.2 Teste unitário: datas fixas de 2026 (Mães 10/05, Pais 09/08, Crianças 12/10, Black Friday 27/11, Natal 25/12) e do cenário "Datas fixas de 2027" (Mães 09/05, Pais 08/08, Black Friday 26/11)
- [x] 2.3 Teste unitário: próxima ocorrência no mesmo ano, no ano seguinte (05/01 a partir de 15/07/2026 é 2027-01-05) e no próprio dia (0 dias)
- [x] 2.4 Teste unitário do cenário "Limite de 30 dias": o Dia dos Pais é próximo em 10/07/2026 e não é em 09/07/2026

## 3. Datas personalizadas (RF09)

- [x] 3.1 Criar a entidade `DataComemorativa`, o repository só com métodos filtrados por tenant, os DTOs (com a restrição `@DiaValidoNoMes`), a factory, o service (409 para nome repetido, inclusive por violação do índice único) e o `DataComemorativaController` com `GET`, `POST` em `/api/v1/datas-comemorativas` e `GET`, `PUT` e `DELETE` em `/{id}`; verificar que o contexto sobe e que `ArquiteturaTest` continua passando
- [x] 3.2 Teste do cenário "Calendário a partir de 15/07/2026": as 5 datas fixas na ordem e com as próximas ocorrências da spec, `id` nulo e `personalizada` falso
- [x] 3.3 Testes dos cenários "Aniversário da cidade" (201, `Location`, próxima ocorrência 2026-08-01 e 17 dias), "Próxima ocorrência no ano seguinte" e "Data que cai hoje"
- [x] 3.4 Testes dos cenários "Campos obrigatórios ausentes", "Mês inválido", "Dia que não existe no mês" e "29 de fevereiro": 400 com o campo em `erros` e nada gravado
- [x] 3.5 Teste do cenário "Data repetida com outra grafia": 409
- [x] 3.6 Testes dos cenários "Calendário com data personalizada", "Mudança do dia do aniversário da cidade", "Exclusão de data personalizada" e "Data personalizada inexistente"

## 4. Vendas por produto e precificação opcional

- [x] 4.1 Adicionar ao `MovimentacaoEstoqueRepository` a consulta agregada de saídas por produto no período e ao `EstoqueService` o método `vendasPorProduto(de, ate)` (dto com produto, quantidade e valor total); teste de integração com o estoque do estudo de caso: vendas de 15/06 a 15/07/2026 iguais às da seção 4.1
- [x] 4.2 Adicionar ao `PrecificacaoService` o `consultarSeDisponivel()`, vazio sem parâmetros financeiros ou com markup inviável, mantendo o 422 do `consultar()`; testes: vazio sem parâmetros, vazio com margem 85.00 e preenchido com o estudo de caso, e os testes de precificação existentes continuam passando

## 5. Alertas (RF08, RF09)

- [x] 5.1 Criar em `br.com.valorizamei.alerta` o `AlertaService` com as regras de ruptura e de sugestão como funções puras e o `AlertaController` com `GET /api/v1/alertas` (data de referência, ruptura, datas próximas e sugestões); verificar que `ArquiteturaTest` continua passando
- [x] 5.2 Testes unitários das regras: ruptura com estoque menor, igual e maior que o mínimo e com mínimo zero; reforço, promoção e reajuste; produto com duas sugestões; nenhuma sugestão sem data próxima (cenário "Sem data comemorativa próxima", data 15/06/2026); e os textos dos motivos
- [x] 5.3 Testes de integração dos cenários "Ruptura no estudo de caso", "Produto sem estoque", "Estoque igual ao mínimo" e "Estoque mínimo zero não gera alerta"
- [x] 5.4 Testes de integração dos cenários "Dia dos Pais no estudo de caso" e "Data personalizada próxima"
- [x] 5.5 Testes de integração dos cenários "Reforço da furadeira e da trena" (com o motivo da spec) e "Promoção da caixa de ferramentas"
- [x] 5.6 Testes de integração dos cenários "Furadeira vendida abaixo do preço sugerido", "Vendas no preço sugerido" e "Sem parâmetros financeiros"

## 6. Isolamento entre MEIs (RF10, RNF07)

- [x] 6.1 Teste de isolamento, cenário "Cada MEI com os próprios alertas"
- [x] 6.2 Teste de isolamento, cenário "Data de outro MEI": não aparece no calendário da loja, e `GET`, `PUT` e `DELETE` respondem 404
- [x] 6.3 Testes dos cenários "Alertas sem token" e "Calendário sem token": 401 em Problem Details

## 7. Documentação (RF08, RF09, RF11)

- [x] 7.1 Anotar os controllers com `@Tag` "Alertas" e "Datas comemorativas" e `@Operation` em português com as regras; teste em `DocumentacaoApiTest`: os caminhos aparecem na especificação e exigem `bearerAuth`
- [x] 7.2 Atualizar o README (estado, endpoints, exemplos de alertas e de data personalizada pelo Swagger UI, problemas comuns e total de testes); verificar o roteiro com `mvnw.cmd spring-boot:test-run`

## 8. Fechamento

- [x] 8.1 Executar `mvnw.cmd verify` com todos os testes passando, incluindo os de arquitetura
- [x] 8.2 Registrar o teste de aceitação do RF08 e do RF09 (ação executada, resultado esperado, resultado obtido e se o requisito foi atendido) em `docs/testes-aceitacao.md`, incluindo o isolamento (RNF07)
- [x] 8.3 Verificar manualmente pela API em execução: calendário com as datas fixas, cadastro de data personalizada, ruptura e, se houver data comemorativa nos próximos 30 dias a partir da data real, as sugestões

## Workflow follow-up

- Arquivar a change com `openspec archive alertas` após a revisão.
- Confirmar que as specs `alertas` e `datas-comemorativas` foram criadas em `openspec/specs/`.
