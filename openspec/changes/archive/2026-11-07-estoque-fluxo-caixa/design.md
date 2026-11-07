# Design

## Context

O cadastro de produtos (`produto`) não tem quantidade. A precificação (`precificacao`) mostrou o padrão de uma feature usar outra só pelo service e pelos DTOs dela. O `ProdutoService.consultar(id)` já devolve o produto do MEI com o custo direto, ou 404. A exclusão de produto é física, e a `cadastro-produtos` deixou para esta change a regra de exclusão de produto com movimentações. A motivação está em proposal.md, e o comportamento esperado, nas specs `estoque`, `fluxo-de-caixa` e `produtos`.

Decisões tomadas com o autor antes do design:
- a venda (saída de estoque) tem o valor unitário informado pelo MEI;
- toda entrada de mercadoria lança uma saída no caixa, pelo custo direto do produto;
- o cadastro do produto continua sem quantidade;
- o fluxo de caixa mostra só as movimentações de estoque, sem as despesas;
- uma venda maior que o estoque atual é recusada.

## Goals / Non-Goals

**Goals:**

- Manter o estoque atual sempre consistente com as movimentações e nunca negativo, mesmo com vendas simultâneas.
- Gerar o lançamento de caixa de forma atômica com a movimentação: um não existe sem o outro.
- Deixar disponíveis o estoque atual e as vendas por período, para os alertas (RF08, RF09).

**Non-Goals:**

- Alterar ou excluir movimentações e estornar vendas. As movimentações são imutáveis nesta etapa.
- Lançamentos de caixa avulsos e pagamento de despesas no caixa (decisão do autor: só o estoque).
- Custo médio ponderado ou custo informado por compra: a entrada usa o custo direto atual do produto.
- Saldo inicial de caixa, contas bancárias e conciliação.
- Alertas de ruptura e sugestões sazonais (change `alertas`, RF08 e RF09).
- Paginação das listas de movimentações e de lançamentos.

## Decisions

1. **Duas features: `estoque` e `caixa`.**
   - `estoque`: `MovimentacaoEstoqueController` (`/api/v1/estoque/movimentacoes`) e `EstoqueController` (`/api/v1/estoque`), `EstoqueService`, as entidades `MovimentacaoEstoque` e `SaldoEstoque`, os repositories, a factory e os DTOs.
   - `caixa`: `FluxoDeCaixaController` (`/api/v1/fluxo-de-caixa`), `CaixaService`, a entidade `LancamentoCaixa`, o repository, a factory e os DTOs.
   - O `EstoqueService` usa o `ProdutoService` (valida o produto do MEI e obtém o custo e o nome) e o `CaixaService` (gera o lançamento). O caixa não conhece o estoque, e não há ciclo.
   - *Alternativa descartada:* uma única feature. O fluxo de caixa é um conceito próprio, que poderá receber lançamentos de outras origens, e separá-lo deixa cada feature com uma responsabilidade.

2. **Modelo de dados (changeset `004-criar-tabelas-estoque-caixa`).**
   - `movimentacao_estoque`:
     - `id uuid` PK (`pk_movimentacao_estoque`);
     - `tenant_id` FK `fk_movimentacao_estoque_mei`;
     - `produto_id` FK `fk_movimentacao_estoque_produto` para `produto(id)`;
     - `tipo varchar(7)` com `ck_movimentacao_estoque_tipo` (`ENTRADA`, `SAIDA`);
     - `quantidade integer` com `> 0`;
     - `valor_unitario numeric(12,2)` com `> 0`;
     - `valor_total numeric(14,2)` com `> 0`;
     - `data date not null` e `criado_em timestamptz not null`;
     - índice `idx_movimentacao_estoque_tenant_produto_data` em `(tenant_id, produto_id, data)`, para o filtro por produto e as vendas por período dos alertas.
   - `saldo_estoque`:
     - `produto_id uuid` PK (`pk_saldo_estoque`) com FK `fk_saldo_estoque_produto`;
     - `tenant_id` FK `fk_saldo_estoque_mei`;
     - `quantidade integer not null` com `ck_saldo_estoque_quantidade` `>= 0`.
   - `lancamento_caixa`:
     - `id uuid` PK (`pk_lancamento_caixa`);
     - `tenant_id` FK `fk_lancamento_caixa_mei`;
     - `tipo varchar(7)` com `ck_lancamento_caixa_tipo` (`ENTRADA`, `SAIDA`);
     - `descricao varchar(200) not null`;
     - `valor numeric(14,2)` com `> 0`;
     - `data date not null`;
     - `movimentacao_estoque_id uuid` com FK `fk_lancamento_caixa_movimentacao` e restrição única (`uk_lancamento_caixa_movimentacao`). A coluna aceita nulo, para lançamentos de outras origens no futuro;
     - `criado_em timestamptz not null`;
     - índice `idx_lancamento_caixa_tenant_data` em `(tenant_id, data)`.
   - As FKs para `produto` impedem excluir um produto com movimentações ou saldo (ver decisão 7).

3. **Estoque atual guardado em `saldo_estoque`, atualizado de forma atômica.**
   - Entrada: `insert ... on conflict (produto_id) do update set quantidade = saldo_estoque.quantidade + :quantidade`.
   - Saída: `update saldo_estoque set quantidade = quantidade - :quantidade where produto_id = :produto and tenant_id = :tenant and quantidade >= :quantidade`.
     - Se nenhuma linha for alterada, o estoque é insuficiente: a transação é desfeita e a resposta é 422.
     - Duas vendas simultâneas não deixam o estoque negativo: o banco serializa as duas atualizações da mesma linha, e a segunda só passa se ainda houver quantidade. A restrição `>= 0` é a segunda barreira.
   - Produto sem nenhuma movimentação tem estoque 0 (sem linha em `saldo_estoque`).
   - Consultas nativas no `SaldoEstoqueRepository` (`@Modifying @Query`), sempre filtradas por tenant.
   - *Alternativa descartada:* calcular o estoque somando as movimentações a cada consulta. Seria simples, mas a verificação "venda ≤ estoque" ficaria sujeita a corrida entre vendas simultâneas sem um bloqueio explícito.
   - *Alternativa descartada:* coluna `estoque_atual` na tabela `produto`. Faria a feature de estoque escrever na tabela de outra feature.

4. **Registro da movimentação.** `POST /api/v1/estoque/movimentacoes` com `produtoId`, `tipo` (`ENTRADA` ou `SAIDA`), `quantidade`, `valorUnitario` e `data`.
   - `quantidade`: inteiro de 1 a 1.000.000.
   - `valorUnitario`: obrigatório na `SAIDA` (maior que zero, até 2 casas) e não aceito na `ENTRADA`, que usa o custo direto do produto. A restrição de classe no DTO (`@ValorUnitarioConformeTipo`) segue o padrão das despesas.
   - `data`: opcional; o padrão é a data de hoje do `Clock` (fuso de São Paulo). Data futura gera 400.
   - Na mesma transação, o service:
     1. busca o produto pelo `ProdutoService` (404 se não for do MEI);
     2. atualiza o saldo (422 na venda sem estoque);
     3. grava a movimentação com `valor_total = quantidade × valor unitário`;
     4. chama o `CaixaService` para o lançamento.
   - A resposta (201, com `Location`) traz:
     - `id`, `produtoId`, `nomeProduto`, `tipo`, `quantidade`, `valorUnitario`, `valorTotal` e `data`;
     - `estoqueAtual`: o estoque do produto após a movimentação.
   - Venda sem estoque: 422 "Estoque insuficiente" / "O produto Trena 5 m tem 4 unidades em estoque; não é possível registrar a saída de 5." (nome e quantidades do caso).

5. **Consultas de estoque.**
   - `GET /api/v1/estoque/movimentacoes`: movimentações do MEI, da mais recente para a mais antiga (data e, no mesmo dia, ordem de registro). O parâmetro opcional `produtoId` filtra a lista; um produto de outro MEI devolve lista vazia.
   - `GET /api/v1/estoque/movimentacoes/{id}`: uma movimentação, ou 404.
   - `GET /api/v1/estoque`: todos os produtos do MEI em ordem alfabética, com `produtoId`, `nome`, `estoqueAtual` e `estoqueMinimo`. Produtos sem movimentação aparecem com estoque 0. A lista vem do `ProdutoService.listar()`, combinada com os saldos do MEI.
   - O `EstoqueService` expõe para a change de alertas os métodos `estoqueAtual()` e `quantidadeVendida(produtoId, de, ate)`.

6. **Fluxo de caixa.**
   - O `CaixaService.lancar(tipo, descricao, valor, data, movimentacaoId)` grava o lançamento com o tenant do token. As descrições são geradas pelo estoque:
     - "Compra: 10 × Furadeira de impacto 650 W" na entrada de estoque;
     - "Venda: 2 × Furadeira de impacto 650 W" na saída.
   - `GET /api/v1/fluxo-de-caixa?inicio=AAAA-MM-DD&fim=AAAA-MM-DD`:
     - período opcional, com padrão do primeiro ao último dia do mês atual do `Clock`;
     - `inicio` depois de `fim` gera 400.
   - A resposta traz `inicio`, `fim`, `totalEntradas`, `totalSaidas`, `saldo` (entradas − saídas, pode ser negativo) e `lancamentos` (`id`, `data`, `tipo`, `descricao`, `valor`), em ordem cronológica.
   - Os valores têm 2 casas. Sem lançamentos no período, os totais são `0.00` e a lista é vazia.

7. **Exclusão de produto com movimentações (alteração em `produto`).**
   - O `ProdutoService.excluir` passa a remover com `flush` dentro de `try`. Uma `DataIntegrityViolationException` (FK de movimentação ou de saldo) vira 409 "Produto com movimentações" / "O produto tem movimentações de estoque e não pode ser excluído.".
   - A feature `produto` continua sem conhecer o estoque: a regra vem da integridade do banco.
   - *Alternativa descartada:* exclusão em cascata. Ela apagaria o histórico de estoque e de caixa.

8. **Classificações duplicadas em `dto` e `model`.** Como nas despesas, os enums `TipoMovimentacao` (dto) e `SentidoMovimentacao` (model), e `TipoLancamento` (dto) e `SentidoLancamento` (model), são convertidos pelas factories.

9. **Erros.**
   - 404: "Movimentação não encontrada".
   - 404: "Produto não encontrado", vindo do `ProdutoService`, ao movimentar produto inexistente ou de outro MEI.
   - 422: "Estoque insuficiente".
   - 409: "Produto com movimentações".
   - 400 para validação, data futura e período invertido, com a lista de campos quando aplicável.

10. **Documentação.** `@Tag` "Estoque" e "Fluxo de caixa", com `@Operation` em português e `@ApiResponse` 422 no registro de movimentação.

11. **Testes.**
    - Integração com o estoque do estudo de caso, montado por entradas e saídas. Exemplo da furadeira: entrada de 11 e venda de 6 deixam estoque 5, com 6 vendas registradas.
    - Venda sem estoque (trena: 4 em estoque, venda de 5).
    - Caixa por período.
    - Exclusão de produto com movimentações.
    - Isolamento entre "Loja de Ferramentas" e "Papelaria Central".
    - Data fixa do `Clock` nos testes de período: um `@TestConfiguration` com `Clock.fixed` em 15/07/2026, de acordo com o cenário do Dia dos Pais.
    - `BancoDeTeste.limpar` apaga `lancamento_caixa`, `movimentacao_estoque` e `saldo_estoque` antes de `produto`.

## Risks / Trade-offs

- [Movimentação registrada por engano não pode ser corrigida] → Fora do escopo desta etapa. Uma compra errada pode ser compensada com uma venda e vice-versa, com reflexo no caixa. O estorno fica como evolução.
- [A entrada usa o custo direto atual, não o valor efetivamente pago] → Decisão do autor. Se o custo mudar, o MEI altera o produto antes de registrar a compra.
- [Saldo guardado pode divergir das movimentações] → Saldo e movimentação são gravados na mesma transação, e o teste compara o saldo com a soma das movimentações do estudo de caso.
- [BREAKING: produto com movimentações não pode ser excluído] → Mantém o histórico de estoque e de caixa. Ainda não há clientes da API.

## Migration Plan

- O changeset `004` cria as três tabelas na inicialização. Não há dados a migrar: os produtos existentes ficam com estoque 0.
- Para reverter em desenvolvimento, basta recriar o banco (contêiner descartável) e remover o changeset.
