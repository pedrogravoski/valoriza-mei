# Design

## Context

A `autenticacao-multitenant` entregou o JWT com `tenant_id`, o componente `TenantAtual` como único acesso ao MEI da requisição, a `ErroDeNegocioException` para erros de negócio em Problem Details e o padrão de isolamento explícito por `tenant_id` (decisão 6 daquela change). O banco tem apenas as tabelas `mei` e `usuario`. Esta é a primeira tabela de negócio, então ela fixa na prática o padrão que as próximas features (despesas, estoque) vão copiar. A motivação está em proposal.md, e o comportamento esperado, na spec `produtos`.

## Goals / Non-Goals

**Goals:**

- Aplicar o padrão de isolamento por `tenant_id` de forma simples de seguir e de testar nas próximas changes.
- Guardar o custo direto com precisão monetária (2 casas, sem arredondamento silencioso), pronto para o cálculo do preço de venda (RF04).

**Non-Goals:**

- Estoque atual e movimentações de entrada e saída (change de estoque, RF06).
- Preço de venda sugerido (change de precificação, RF03 e RF04).
- Paginação, filtros e busca por nome na listagem.
- Categoria, código de barras, unidade de medida, fornecedor e foto do produto.
- Exclusão lógica e histórico de alterações do custo.
- CORS para o frontend Angular (tratado junto com a integração do frontend).

## Decisions

1. **Feature `br.com.valorizamei.produto`.**
   - `ProdutoController` em `/api/v1/produtos`, `ProdutoService`, `ProdutoFactory`, `ProdutoRepository`, a entidade `Produto` e os DTOs `ProdutoRequest` e `ProdutoResponse`.
   - O mesmo `ProdutoRequest` serve para o cadastro e para a alteração, porque os campos e as validações são os mesmos.
   - A feature depende apenas de `shared` (`TenantAtual`, `ErroDeNegocioException`, `Clock`), sem acessar a feature `autenticacao`.

2. **Endpoints REST.**
   - `POST /api/v1/produtos`: 201 com o produto e o cabeçalho `Location` apontando para `/api/v1/produtos/{id}`.
   - `GET /api/v1/produtos`: 200 com a lista de produtos do MEI ordenada pelo nome, sem diferenciar maiúsculas e minúsculas. Lista vazia quando não há produtos.
   - `GET /api/v1/produtos/{id}`: 200 com o produto.
   - `PUT /api/v1/produtos/{id}`: substitui nome, custo direto e estoque mínimo e responde 200 com o produto atualizado.
   - `DELETE /api/v1/produtos/{id}`: 204 sem corpo.
   - A resposta contém `id`, `nome`, `custoDireto` e `estoqueMinimo`.
   - *Alternativa descartada:* `PATCH` para alteração parcial. Com três campos, a substituição completa é mais simples de validar e de documentar.

3. **Modelo de dados (changeset `002-criar-tabela-produto`).**
   - `produto`: `id uuid` PK (`pk_produto`), `tenant_id uuid not null` com FK `fk_produto_mei` para `mei(id)`, `nome varchar(120) not null`, `custo_direto numeric(12,2) not null`, `estoque_minimo integer not null`, `criado_em timestamptz not null` e `atualizado_em timestamptz not null`.
   - Restrições de verificação no banco: `custo_direto > 0` (`ck_produto_custo_direto`) e `estoque_minimo >= 0` (`ck_produto_estoque_minimo`), como segunda barreira além da validação do DTO.
   - Índice único `uk_produto_tenant_nome` em `(tenant_id, lower(nome))`, criado por SQL no changeset porque o Liquibase não declara índice por expressão de forma portável. Ele garante o nome único por MEI e também atende às consultas filtradas por `tenant_id`, sem índice separado.
   - Os ids são UUID gerados pela aplicação (JPA `GenerationType.UUID`), e as datas vêm do `Clock` injetável.
   - *Alternativa descartada:* coluna `nome_normalizado`. Ela duplicaria o dado só para a comparação, e o índice por expressão resolve o mesmo problema.

4. **Validação da entrada.**
   - `nome`: obrigatório, até 120 caracteres; espaços nas pontas são removidos antes de validar unicidade e de gravar.
   - `custoDireto`: obrigatório, maior que zero, com no máximo 10 dígitos inteiros e 2 casas decimais (`@Digits(integer = 10, fraction = 2)`). Um valor com mais casas é recusado com 400, em vez de arredondado sem aviso. Custo zero é recusado porque geraria preço de venda zero no RF04.
   - `estoqueMinimo`: obrigatório, inteiro maior ou igual a zero. Zero significa que o MEI não quer alerta de ruptura para o produto.
   - O custo é `BigDecimal` em todas as camadas e é devolvido com 2 casas (`180.00`).

5. **Isolamento aplicado pelo service e pelo repository.**
   - O service define o `tenant_id` do produto a partir de `TenantAtual` no cadastro; os DTOs não têm campo de tenant.
   - O repository expõe somente métodos filtrados por tenant: busca por id e tenant (`findByIdAndTenantId`), listagem do tenant ordenada pelo nome sem diferenciar maiúsculas e minúsculas, e verificação de nome existente no tenant, com uma variante que desconsidera o próprio produto na alteração. Os métodos herdados de `JpaRepository` sem filtro (`findById`, `findAll`, `deleteById`) não são usados pelo service.
   - Consulta, alteração e exclusão primeiro buscam o produto por id e tenant. Se não encontrar, respondem 404 "Produto não encontrado", sem distinguir um id inexistente de um id de outro MEI.

6. **Nome duplicado no mesmo MEI.**
   - Antes de gravar, o service verifica se já existe produto do mesmo MEI com o mesmo nome, ignorando maiúsculas e minúsculas; na alteração, o próprio produto é desconsiderado. Se existir, responde 409 "Produto já cadastrado".
   - Uma violação do índice único em gravações simultâneas também é convertida em 409, como no cadastro de conta.
   - Nomes iguais em MEIs diferentes são permitidos, porque o índice inclui o `tenant_id`.

7. **Exclusão física.** O produto é removido da tabela. Como ainda não há movimentações de estoque, nada referencia o produto. A change de estoque decidirá o que acontece com a exclusão de um produto com movimentações (ver Riscos).

8. **Erros de negócio com `ErroDeNegocioException`.** 404 "Produto não encontrado" / "Não existe produto com este identificador." e 409 "Produto já cadastrado" / "Já existe um produto com este nome.". Um id que não é UUID no caminho já gera 400 em Problem Details pelo `TratadorGlobalDeErros` (erro de conversão de tipo), sem código novo.

9. **Documentação.** O controller recebe `@Tag` "Produtos" e `@Operation` com resumo em português; os endpoints herdam o requisito global `bearerAuth`.

10. **Testes.**
    - Testes de integração com Testcontainers e MockMvc, usando o `ApoioDeAutenticacao` para obter o token.
    - Um teste de isolamento com dois MEIs ("Loja de Ferramentas" e "Papelaria Central") cobre listagem, consulta, alteração e exclusão do produto de outro MEI.
    - A limpeza do banco entre testes passa a apagar `produto` antes de `usuario` e `mei`, por causa da chave estrangeira.
    - `MigracoesBancoTest` verifica a tabela, as restrições de verificação e o índice único.

## Risks / Trade-offs

- [Exclusão física quebrará a integridade quando houver movimentações de estoque] → A change de estoque vai definir a regra (recusar com 409 a exclusão de produto com movimentações, ou passar a inativar). A chave estrangeira das movimentações impede a exclusão silenciosa até lá.
- [Listagem sem paginação] → Um MEI tem dezenas ou poucas centenas de produtos. A paginação pode ser adicionada depois sem quebrar o contrato, com parâmetros opcionais.
- [Nome único só por maiúsculas e minúsculas: "Trena 5 m" e "Trena 5m" são aceitos como produtos diferentes] → Aceitável para o escopo; normalizar acentos e espaços internos geraria recusas difíceis de entender.
- [Alteração do custo muda o preço sugerido sem histórico] → O histórico de custos está fora do escopo; o preço sempre reflete o custo atual.

## Migration Plan

- O changeset `002` cria a tabela `produto` na inicialização. Não há dados a migrar.
- Para reverter em desenvolvimento, basta recriar o banco (contêiner descartável) e remover o changeset.
