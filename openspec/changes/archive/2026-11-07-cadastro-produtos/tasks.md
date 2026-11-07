# Tasks

## 1. Banco de dados: tabela de produtos (RF01, RNF06, RNF07)

- [x] 1.1 Criar o changeset `db/changelog/changes/002-criar-tabela-produto.yaml` com a tabela `produto` (FK `tenant_id` → `mei(id)`, `custo_direto numeric(12,2)`, `estoque_minimo integer`, restrições `ck_produto_custo_direto` e `ck_produto_estoque_minimo`, índice único `uk_produto_tenant_nome` em `(tenant_id, lower(nome))`) e incluí-lo no changelog mestre; verificar com `MigracoesBancoTest` que a tabela existe após a migração
- [x] 1.2 Testes de integração em `MigracoesBancoTest`: o banco recusa custo direto zero, estoque mínimo negativo e dois produtos do mesmo MEI com o mesmo nome em grafias diferentes, e aceita o mesmo nome em MEIs diferentes
- [x] 1.3 Mover a limpeza do banco dos testes para uma classe de apoio pública em `src/test` (`br.com.valorizamei`), apagando `produto`, `usuario` e `mei` nessa ordem, e passar os testes da feature `autenticacao` a usá-la; verificar que os testes de cadastro, login e conta continuam passando

## 2. Cadastro e validação (RF01)

- [x] 2.1 Criar em `br.com.valorizamei.produto` a entidade `Produto`, o `ProdutoRepository` só com métodos filtrados por tenant, os DTOs `ProdutoRequest` (validações do design, nome sem espaços nas pontas) e `ProdutoResponse`, a `ProdutoFactory`, o `ProdutoService` (tenant de `TenantAtual`, datas do `Clock`, 409 para nome repetido no MEI, inclusive por violação do índice único) e o `ProdutoController` com `POST /api/v1/produtos`; verificar que o contexto sobe com `ddl-auto=validate` e que `ArquiteturaTest` continua passando
- [x] 2.2 Teste do cenário "Cadastro da furadeira da loja de ferramentas": 201, `Location` com `/api/v1/produtos/{id}` e corpo com `id`, `nome`, `custoDireto` 180.00 e `estoqueMinimo` 3
- [x] 2.3 Teste do cenário "Nome com espaços nas pontas": 201 com `nome` "Trena 5 m"
- [x] 2.4 Teste do cenário "Campos obrigatórios ausentes": 400 com itens em `erros` para `nome`, `custoDireto` e `estoqueMinimo`
- [x] 2.5 Testes dos cenários "Custo direto zero", "Custo direto com mais de 2 casas decimais" e "Estoque mínimo negativo": 400 com item em `erros` para o campo e nenhum produto gravado
- [x] 2.6 Teste do cenário "Produto repetido com outra grafia": 409 em Problem Details em português e a loja continua com um único produto com esse nome
- [x] 2.7 Teste do cenário "Mesmo nome em MEIs diferentes": a "Papelaria Central" cadastra "Trena 5 m" com 201

## 3. Listagem e consulta (RF01)

- [x] 3.1 Implementar `GET /api/v1/produtos` (ordenado pelo nome, sem diferenciar maiúsculas e minúsculas) e `GET /api/v1/produtos/{id}` (404 "Produto não encontrado" quando não houver produto do MEI com o id); verificar com os testes abaixo
- [x] 3.2 Teste do cenário "Produtos da loja de ferramentas em ordem alfabética": os 6 produtos do estudo de caso na ordem da spec, com a "Trena 5 m" com `custoDireto` 12.00 e `estoqueMinimo` 5
- [x] 3.3 Teste do cenário "MEI sem produtos": 200 com lista vazia
- [x] 3.4 Teste do cenário "Consulta da furadeira": 200 com nome, custo direto 180.00 e estoque mínimo 3
- [x] 3.5 Teste do cenário "Produto inexistente": 404 `application/problem+json` com `title` e `detail` em português
- [x] 3.6 Teste do cenário "Identificador em formato inválido": `GET /api/v1/produtos/abc` retorna 400 em Problem Details

## 4. Alteração e exclusão (RF01)

- [x] 4.1 Implementar `PUT /api/v1/produtos/{id}` (mesmas validações do cadastro, nome repetido desconsiderando o próprio produto, `atualizado_em` do `Clock`) e `DELETE /api/v1/produtos/{id}` (204); verificar com os testes abaixo
- [x] 4.2 Teste do cenário "Reajuste do custo da furadeira": 200 com custo direto 190.00 e estoque mínimo 4, confirmados por nova consulta
- [x] 4.3 Teste do cenário "Alteração para o nome de outro produto do MEI": 409 e a trena mantém o nome "Trena 5 m"
- [x] 4.4 Teste: alterar a furadeira mantendo o próprio nome com outra grafia de maiúsculas não gera 409
- [x] 4.5 Teste do cenário "Alteração de produto inexistente": 404
- [x] 4.6 Teste do cenário "Exclusão da caixa de ferramentas": 204 sem corpo, consulta posterior 404 e produto fora da listagem
- [x] 4.7 Teste do cenário "Exclusão de produto inexistente": 404

## 5. Isolamento entre MEIs (RF10, RNF07)

- [x] 5.1 Teste de isolamento, cenário "Cada MEI lista somente os próprios produtos": "Loja de Ferramentas" e "Papelaria Central" recebem somente os próprios produtos
- [x] 5.2 Teste de isolamento, cenário "Consulta, alteração e exclusão do produto de outro MEI": `GET`, `PUT` e `DELETE` da papelaria na furadeira da loja retornam 404, e a furadeira continua com os dados originais
- [x] 5.3 Teste de isolamento, cenário "Tenant enviado no corpo é ignorado": o produto é gravado para a loja e não aparece para a papelaria
- [x] 5.4 Teste do cenário "Produtos sem token": `GET /api/v1/produtos` sem token retorna 401 em Problem Details

## 6. Documentação (RF01, RF11)

- [x] 6.1 Anotar o `ProdutoController` com `@Tag` "Produtos" e `@Operation` em português; teste em `DocumentacaoApiTest`: `/api/v1/produtos` e `/api/v1/produtos/{id}` aparecem na especificação e exigem o esquema `bearerAuth` (sem `security` próprio)
- [x] 6.2 Atualizar o README (tabela de estado, lista de endpoints, total de testes e exemplo de cadastro de produto pelo Swagger UI); verificar o roteiro com `mvnw.cmd spring-boot:test-run`

## 7. Fechamento

- [x] 7.1 Executar `mvnw.cmd verify` com todos os testes passando, incluindo os de arquitetura
- [x] 7.2 Registrar o teste de aceitação do RF01 (ação executada, resultado esperado, resultado obtido e se o requisito foi atendido) em `docs/testes-aceitacao.md`, incluindo o isolamento de produtos (RNF07)
- [x] 7.3 Verificar manualmente pelo Swagger UI: login, cadastro dos 6 produtos do estudo de caso, listagem em ordem alfabética, alteração, exclusão e 404 para produto de outro MEI

## Workflow follow-up

- Arquivar a change com `openspec archive cadastro-produtos` após a revisão.
- Confirmar que a spec `produtos` foi criada em `openspec/specs/`.
