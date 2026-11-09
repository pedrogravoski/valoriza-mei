# Tasks

## 1. Banco de dados: estoque e caixa (RF06, RF07, RNF06, RNF07)

- [x] 1.1 Criar o changeset `db/changelog/changes/004-criar-tabelas-estoque-caixa.yaml` com as tabelas `movimentacao_estoque`, `saldo_estoque` e `lancamento_caixa` (FKs, restrições de verificação com `is not null` explícito onde a coluna aceita nulo, restrição única `uk_lancamento_caixa_movimentacao` e índices do design) e incluí-lo no changelog mestre; verificar com `MigracoesBancoTest` que as tabelas existem após a migração
- [x] 1.2 Testes de integração em `MigracoesBancoTest`: o banco recusa quantidade zero, tipo desconhecido, saldo negativo e dois lançamentos para a mesma movimentação
- [x] 1.3 Incluir `lancamento_caixa`, `movimentacao_estoque` e `saldo_estoque` em `BancoDeTeste.limpar`, antes de `produto`; verificar que os testes existentes continuam passando
- [x] 1.4 Criar em `src/test` a configuração de teste com `Clock` fixo em 15/07/2026 (fuso de São Paulo), usada pelos testes que dependem da data atual

## 2. Fluxo de caixa: lançamentos (RF07)

- [x] 2.1 Criar em `br.com.valorizamei.caixa` a entidade `LancamentoCaixa` com o enum `SentidoLancamento`, o `LancamentoCaixaRepository` filtrado por tenant e o `CaixaService.lancar(...)`, que grava o lançamento com o tenant do token; verificar que o contexto sobe com `ddl-auto=validate` e que `ArquiteturaTest` continua passando

## 3. Movimentações de estoque (RF06, RF07)

- [x] 3.1 Criar em `br.com.valorizamei.estoque` as entidades `MovimentacaoEstoque` (enum `SentidoMovimentacao`) e `SaldoEstoque`, o `SaldoEstoqueRepository` com a entrada (`insert ... on conflict`) e a saída condicional (`update ... where quantidade >= :quantidade`), o `MovimentacaoEstoqueRepository`, os DTOs (enum `TipoMovimentacao` e restrição `@ValorUnitarioConformeTipo`), a factory, o `EstoqueService` (produto pelo `ProdutoService`, saldo, movimentação e lançamento de caixa na mesma transação) e `POST /api/v1/estoque/movimentacoes`; verificar que `ArquiteturaTest` continua passando
- [x] 3.2 Teste do cenário "Compra de furadeiras": 201 com `Location`, valor unitário 180.00, valor total 1800.00 e estoque atual 10
- [x] 3.3 Teste do cenário "Venda de furadeiras": 201 com valor total 600.00 e estoque atual 8
- [x] 3.4 Testes dos cenários "Campos obrigatórios ausentes", "Quantidade zero", "Venda sem valor unitário", "Compra com valor unitário" e "Data futura": 400 com o campo em `erros` e nada gravado
- [x] 3.5 Testes dos cenários "Movimentação sem data" (data 2026-07-15) e "Venda de dia anterior" (data 2026-07-10)
- [x] 3.6 Testes dos cenários "Venda de trenas acima do estoque" (422 com produto e quantidades no detalhe, estoque mantido, sem movimentação nem lançamento), "Venda de produto sem movimentações" e "Venda de todo o estoque" (estoque 0)
- [x] 3.7 Teste do cenário "Produto inexistente": 404 sem gravar nada

## 4. Consultas de estoque (RF06)

- [x] 4.1 Implementar `GET /api/v1/estoque/movimentacoes` (mais recente primeiro, filtro opcional `produtoId`), `GET /api/v1/estoque/movimentacoes/{id}` e `GET /api/v1/estoque` (produtos em ordem alfabética com estoque atual e mínimo); expor no `EstoqueService` o estoque atual e a quantidade vendida por período para os alertas; verificar com os testes abaixo
- [x] 4.2 Testes dos cenários "Movimentações da furadeira", "Todas as movimentações" e "Movimentação inexistente"
- [x] 4.3 Teste do cenário "Estoque do estudo de caso": os 6 produtos com os estoques atuais e mínimos da seção 4.1, e o saldo igual à soma das movimentações de cada produto
- [x] 4.4 Teste do cenário "Produto sem movimentações": estoque atual 0 e estoque mínimo 3

## 5. Consulta do fluxo de caixa (RF07)

- [x] 5.1 Implementar `GET /api/v1/fluxo-de-caixa` (período opcional, padrão o mês atual do `Clock`; 400 para início depois do fim) com totais, saldo e lançamentos em ordem cronológica; verificar com os testes abaixo
- [x] 5.2 Testes dos cenários "Compra de furadeiras no caixa" e "Venda de furadeiras no caixa": tipo, descrição, data e valor dos lançamentos
- [x] 5.3 Teste do cenário "Venda recusada não altera o caixa"
- [x] 5.4 Testes dos cenários "Totais dos primeiros dez dias de julho" (saldo -1200.00) e "Período padrão é o mês atual"
- [x] 5.5 Testes dos cenários "Período sem lançamentos" (zeros) e "Período invertido" (400)

## 6. Exclusão de produto com movimentações (RF01, RF06)

- [x] 6.1 Alterar o `ProdutoService.excluir` para converter a violação de chave estrangeira em 409 "Produto com movimentações"; teste do cenário "Exclusão da furadeira com compras registradas" e verificação de que os testes de exclusão de produtos sem movimentações continuam passando

## 7. Isolamento entre MEIs (RF10, RNF07)

- [x] 7.1 Teste de isolamento, cenário "Movimentação de produto de outro MEI": 404 e o estoque da loja não muda
- [x] 7.2 Teste de isolamento, cenário "Cada MEI consulta o próprio estoque", incluindo a consulta de movimentação de outro MEI (404)
- [x] 7.3 Teste de isolamento, cenário "Cada MEI consulta o próprio caixa"
- [x] 7.4 Testes dos cenários "Estoque sem token" e "Fluxo de caixa sem token": 401 em Problem Details

## 8. Documentação (RF06, RF07, RF11)

- [x] 8.1 Anotar os controllers com `@Tag` "Estoque" e "Fluxo de caixa", `@Operation` em português e `@ApiResponse` 422 no registro de movimentação; teste em `DocumentacaoApiTest`: os caminhos de estoque e de fluxo de caixa aparecem na especificação e exigem `bearerAuth`
- [x] 8.2 Atualizar o README (estado, endpoints, exemplos de compra, venda, estoque e fluxo de caixa pelo Swagger UI, problemas comuns e total de testes); verificar o roteiro com `mvnw.cmd spring-boot:test-run`

## 9. Fechamento

- [x] 9.1 Executar `mvnw.cmd verify` com todos os testes passando, incluindo os de arquitetura
- [x] 9.2 Registrar o teste de aceitação do RF06 e do RF07 (ação executada, resultado esperado, resultado obtido e se o requisito foi atendido) em `docs/testes-aceitacao.md`, incluindo o isolamento (RNF07)
- [x] 9.3 Verificar manualmente pela API em execução: estoque do estudo de caso, venda acima do estoque (422), fluxo de caixa do mês com totais e saldo, exclusão de produto com movimentações (409) e 404 para produto de outro MEI

## Workflow follow-up

- Arquivar a change com `openspec archive estoque-fluxo-caixa` após a revisão.
- Confirmar que as specs `estoque` e `fluxo-de-caixa` foram criadas em `openspec/specs/` e que `produtos` recebeu o novo requisito.
