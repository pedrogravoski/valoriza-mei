# Testes de aceitação

Registro dos testes funcionais executados pelo autor para cada requisito (seção 4.6 do artigo): ação executada, resultado esperado, resultado obtido e se o requisito foi atendido. Os testes automatizados citados rodam com `mvnw.cmd verify`; as verificações manuais foram feitas com a aplicação em execução (`mvnw.cmd spring-boot:test-run`).

## RF01 — Permitir o cadastro de produtos com o respectivo custo direto de aquisição

Change: `cadastro-produtos`. Data: 07/11/2026.

| Ação executada | Resultado esperado | Resultado obtido | Atendido |
|---|---|---|---|
| `POST /api/v1/produtos` com "Furadeira de impacto 650 W", custo direto 180.00 e estoque mínimo 3 | 201 com o produto e o `Location` do novo recurso | 201 com `id`, nome, `custoDireto` 180.00 e `estoqueMinimo` 3; `Location` `/api/v1/produtos/{id}` | Sim |
| Cadastro dos 6 produtos do estudo de caso e `GET /api/v1/produtos` | Lista com os 6 produtos em ordem alfabética | Alicate, Caixa, Furadeira, Jogo de chaves, Martelo e Trena, com custos e estoques mínimos do estudo de caso (`ConsultaDeProdutoTest`) | Sim |
| Cadastro de "FURADEIRA DE IMPACTO 650 W" com a furadeira já cadastrada | 409, sem gravar outro produto | 409 "Produto já cadastrado"; a loja continua com um único produto com esse nome | Sim |
| Cadastro com custo 0.00, custo 12.005, estoque mínimo -1 e corpo vazio | 400 com o campo inválido em `erros`, sem gravar | 400 "Dados inválidos" com `custoDireto`, `estoqueMinimo` ou os três campos obrigatórios (`CadastroDeProdutoTest`) | Sim |
| `PUT /api/v1/produtos/{id}` da furadeira com custo 190.00 e estoque mínimo 4 | 200 com os valores novos, mantidos na consulta seguinte | 200 com `custoDireto` 190.00 e `estoqueMinimo` 4; a consulta devolve os mesmos valores | Sim |
| `DELETE /api/v1/produtos/{id}` da "Caixa de ferramentas plástica" | 204 e o produto deixa de existir | 204; a consulta seguinte responde 404 e a listagem não traz o produto | Sim |
| `GET /api/v1/produtos/{id}` com id inexistente e com `abc` | 404 e 400 no formato padrão de erro | 404 "Produto não encontrado" e 400 "Requisição inválida" | Sim |

Isolamento dos produtos (RNF07):

| Ação executada | Resultado esperado | Resultado obtido | Atendido |
|---|---|---|---|
| "Loja de Ferramentas" e "Papelaria Central" listam os produtos com os próprios tokens | Cada MEI recebe somente os próprios produtos | A loja recebe a furadeira; a papelaria recebe só o caderno (`ProdutoIsolamentoTest`) | Sim |
| A papelaria envia `GET`, `PUT` e `DELETE` para o id da furadeira da loja | 404 nas três requisições, sem alterar a furadeira | 404 nas três; a furadeira continua com os dados da loja | Sim |
| A loja cadastra um produto enviando `tenantId` da papelaria no corpo | O produto é gravado para a loja | Gravado para a loja; a listagem da papelaria continua vazia | Sim |
| `GET /api/v1/produtos` sem token | 401 em Problem Details | 401 "Não autenticado" com `WWW-Authenticate: Bearer` | Sim |

## RF02 — Permitir o registro de despesas, classificadas como fixas ou variáveis

Change: `despesas`. Data: 07/11/2026.

| Ação executada | Resultado esperado | Resultado obtido | Atendido |
|---|---|---|---|
| `POST /api/v1/despesas` com "Aluguel", tipo `FIXA` e valor mensal 1200.00 | 201 com a despesa fixa, sem percentual | 201 com `valorMensal` 1200.00, `percentual` nulo e `Location` `/api/v1/despesas/{id}` | Sim |
| `POST /api/v1/despesas` com "Taxa de cartão", tipo `VARIAVEL` e percentual 3.00 | 201 com a despesa variável, sem valor mensal | 201 com `percentual` 3.00 e `valorMensal` nulo | Sim |
| Despesa fixa com percentual, fixa sem valor mensal, variável com valor mensal e variável sem percentual | 400 apontando o campo incompatível ou ausente | 400 "Dados inválidos" com o item em `erros` no campo indicado (`RegistroDeDespesaTest`) | Sim |
| Valor mensal 1200.005, percentual 100.00 e tipo `OUTRA` | 400, sem gravar | 400 nos três casos; nenhuma despesa gravada | Sim |
| Registro de "ALUGUEL" com o "Aluguel" já cadastrado | 409, sem duplicar | 409 "Despesa já cadastrada"; uma única despesa | Sim |
| Registro das 5 despesas do estudo de caso e `GET /api/v1/despesas` e `?tipo=FIXA` | Lista em ordem alfabética e filtro pelo tipo | 5 despesas em ordem alfabética; o filtro devolve só Aluguel, Honorários e Internet | Sim |
| `PUT` do aluguel para 1300.00, troca da internet para variável 1% e `DELETE` dos honorários | 200, 200 e 204 | Valores alterados confirmados na consulta; após a exclusão, a consulta responde 404 | Sim |

## RF05 — Ratear as despesas fixas sobre a receita mensal estimada do negócio

Change: `despesas`. Data: 07/11/2026.

| Ação executada | Resultado esperado | Resultado obtido | Atendido |
|---|---|---|---|
| `PUT /api/v1/parametros-financeiros` com receita 14000.00 e margem 25.00 | 200 com os valores gravados | 200 com `receitaMensalEstimada` 14000.00 e `margemDeLucro` 25.00 | Sim |
| `GET /api/v1/despesas/rateio` com as 5 despesas do estudo de caso e receita 14000.00 | Total das fixas 1.400,00, DF 10% e DV 5% (seção 4.1) | `totalDespesasFixas` 1400.00, `percentualDespesasFixas` 10.00 e `percentualDespesasVariaveis` 5.00 | Sim |
| Rateio com aluguel 1300.00 após o reajuste e sem os honorários | DF = 1.420,00 × 100 / 14.000,00 = 10,14% | `totalDespesasFixas` 1420.00 e `percentualDespesasFixas` 10.14 | Sim |
| Rateio com fixa 1000.00 e receita 3000.00 | DF exibido com 2 casas (33,33) e mantido sem arredondamento para o markup | 33.33 na API; 33.3333333333 no cálculo interno (`RateioServiceTest`) | Sim |
| Rateio antes de informar a receita | Receita e DF nulos, demais valores calculados | `receitaMensalEstimada` e `percentualDespesasFixas` nulos | Sim |

Isolamento de despesas e parâmetros (RNF07):

| Ação executada | Resultado esperado | Resultado obtido | Atendido |
|---|---|---|---|
| A papelaria envia `GET`, `PUT` e `DELETE` para o id do aluguel da loja | 404 nas três, sem alterar o aluguel | 404 nas três; o aluguel continua com o valor da loja | Sim |
| Loja (5 despesas, receita 14000.00) e papelaria (conta de luz 300.00, receita 6000.00) consultam o rateio | Cada uma recebe o próprio rateio | Loja: DF 10.00 e DV 5.00; papelaria: total 300.00, DF 5.00 e DV 0.00 (`DespesaIsolamentoTest`) | Sim |
| A papelaria grava os parâmetros enviando o `tenantId` da loja no corpo | Só os parâmetros da papelaria mudam | Os parâmetros da loja continuam 14000.00 e 25.00 | Sim |
| `GET /api/v1/despesas` e `GET /api/v1/parametros-financeiros` sem token | 401 em Problem Details | 401 "Não autenticado" com `WWW-Authenticate: Bearer` | Sim |

## RF03 — Calcular o índice de markup a partir das despesas variáveis, das despesas fixas rateadas e da margem de lucro desejada

Change: `precificacao`. Data: 07/11/2026.

| Ação executada | Resultado esperado | Resultado obtido | Atendido |
|---|---|---|---|
| `GET /api/v1/precificacao` com o estudo de caso completo (DV 5%, DF 10%, ML 25%) | Markup = 100 / [100 − 40] = 1,6667 (seção 4.1) | `indiceMarkup` 1.6667, com DV 5.00, DF 10.00 e ML 25.00 | Sim |
| Precificação de um MEI sem despesas, com margem 25% | Markup = 100 / 75 = 1,3333 | `indiceMarkup` 1.3333, DV e DF 0.00 | Sim |
| Precificação antes de informar receita e margem | 422 orientando a informar os parâmetros | 422 "Parâmetros financeiros não informados" | Sim |
| Margem 85% com DV 5% e DF 10% (soma 100%) | 422, porque o markup não existe | 422 "Markup inviável" com "100,00%" no detalhe | Sim |
| Margem 90% (soma 105%) na consulta da furadeira | 422 com a soma no detalhe | 422 com "105,00%" no detalhe (`PrecificacaoTest`) | Sim |

## RF04 — Calcular o preço de venda sugerido a partir do custo direto e do índice de markup

Change: `precificacao`. Data: 07/11/2026.

| Ação executada | Resultado esperado | Resultado obtido | Atendido |
|---|---|---|---|
| `GET /api/v1/precificacao/produtos/{id}` da furadeira (custo 180,00) | PV = 180,00 × 1,6667 = 300,00 (seção 4.1) | `precoSugerido` 300.00 e `indiceMarkup` 1.6667 | Sim |
| `GET /api/v1/precificacao` com os 6 produtos do estudo de caso | PV de cada produto com o markup sem arredondamento | Alicate 46.67, Caixa 75.00, Furadeira 300.00, Jogo de chaves 41.67, Martelo 36.67 e Trena 20.00, em ordem alfabética | Sim |
| Comparação com o markup arredondado (180,00 × 1,6667) | O preço não usa o markup de 4 casas | 300.00 na API; 300.01 seria o resultado com o markup arredondado (`PrecificacaoServiceTest`) | Sim |
| Aluguel 1000,00, receita 3000,00 (DF 33,33%), margem 25% e produto de custo 100,00 | O preço usa o DF sem arredondamento | Markup 2.4000 e PV 240.00; com o DF arredondado seria 239.98 | Sim |
| Alteração do custo da trena de 12,00 para 15,00 e nova consulta | O preço acompanha o novo custo | `precoSugerido` 25.00 | Sim |

Isolamento da precificação (RNF07):

| Ação executada | Resultado esperado | Resultado obtido | Atendido |
|---|---|---|---|
| Loja (estudo de caso) e papelaria (sem despesas, margem 20%, caderno de custo 16,00) consultam a precificação | Cada MEI com o próprio markup e os próprios produtos | Loja: markup 1.6667 e 6 produtos; papelaria: markup 1.2500 e o caderno a 20.00 | Sim |
| A papelaria consulta o preço da furadeira da loja | 404 | 404 "Produto não encontrado" | Sim |
| `GET /api/v1/precificacao` sem token | 401 em Problem Details | 401 "Não autenticado" com `WWW-Authenticate: Bearer` | Sim |

## RF06 — Registrar entradas e saídas de estoque vinculadas a cada produto

Change: `estoque-fluxo-caixa`. Data: 07/11/2026.

| Ação executada | Resultado esperado | Resultado obtido | Atendido |
|---|---|---|---|
| `POST /api/v1/estoque/movimentacoes` com a entrada de 10 furadeiras | 201 com valor unitário igual ao custo direto e estoque 10 | 201 com `valorUnitario` 180.00, `valorTotal` 1800.00 e `estoqueAtual` 10 | Sim |
| Saída de 2 furadeiras com valor unitário 300.00 | 201 com o valor informado e estoque 8 | 201 com `valorTotal` 600.00 e `estoqueAtual` 8 | Sim |
| Entradas e saídas dos 6 produtos do estudo de caso e `GET /api/v1/estoque` | Estoques da seção 4.1: alicate 12, caixa 10, furadeira 5, jogo de chaves 20, martelo 15 e trena 4 | Estoques e mínimos iguais aos da seção 4.1; saldo igual à soma das movimentações (`ConsultaDeEstoqueTest`) | Sim |
| Venda de 5 trenas com 4 em estoque | 422, sem gravar movimentação nem lançamento | 422 "Estoque insuficiente": "O produto Trena 5 m tem 4 unidades em estoque; não é possível registrar a saída de 5."; estoque mantido em 4 | Sim |
| Compra com valor unitário, venda sem valor unitário, quantidade 0 e data futura | 400 apontando o campo, sem gravar | 400 com `valorUnitario`, `quantidade` ou `data` em `erros` (`MovimentacaoEstoqueTest`) | Sim |
| Movimentação sem data e venda com data de dia anterior (data atual 15/07/2026) | Data atual por padrão e data informada mantida | `data` 2026-07-15 e 2026-07-10 | Sim |
| `GET /api/v1/estoque/movimentacoes?produtoId=<furadeira>` | Movimentações da furadeira, da mais recente para a mais antiga | Venda de 10/07 seguida da compra de 01/07 | Sim |
| `DELETE /api/v1/produtos/{id}` da furadeira com movimentações | 409, sem excluir | 409 "Produto com movimentações"; a furadeira continua cadastrada | Sim |

## RF07 — Refletir as entradas e saídas de estoque no fluxo de caixa

Change: `estoque-fluxo-caixa`. Data: 07/11/2026.

| Ação executada | Resultado esperado | Resultado obtido | Atendido |
|---|---|---|---|
| Entrada de 10 furadeiras em 01/07/2026 | Saída de caixa de 10 × 180,00 | Lançamento `SAIDA` "Compra: 10 × Furadeira de impacto 650 W" de 1800.00 em 2026-07-01 | Sim |
| Venda de 2 furadeiras a 300,00 em 10/07/2026 | Entrada de caixa de 2 × 300,00 | Lançamento `ENTRADA` "Venda: 2 × Furadeira de impacto 650 W" de 600.00 em 2026-07-10 | Sim |
| `GET /api/v1/fluxo-de-caixa?inicio=2026-07-01&fim=2026-07-10` com compra em 01/07, venda em 05/07 e venda em 12/07 | Só os lançamentos do período, com totais e saldo | Entradas 600.00, saídas 1800.00, saldo -1200.00 e 2 lançamentos | Sim |
| Fluxo de caixa sem período, com data atual 15/07/2026 | Mês atual (01/07 a 31/07) | `inicio` 2026-07-01, `fim` 2026-07-31, sem o lançamento de 30/06 | Sim |
| Estudo de caso completo na API em execução (mês atual) | Compras de 4.211,00 (soma das 6 entradas pelo custo) e vendas de 2.745,06 | `totalSaidas` 4211.00, `totalEntradas` 2745.06 e `saldo` -1465.94 | Sim |
| Venda recusada por falta de estoque e período invertido | Caixa inalterado; 400 no período invertido | Nenhum lançamento novo; 400 "Período inválido" | Sim |

Isolamento de estoque e caixa (RNF07):

| Ação executada | Resultado esperado | Resultado obtido | Atendido |
|---|---|---|---|
| A papelaria registra uma entrada na furadeira da loja | 404, sem alterar o estoque da loja | 404 "Produto não encontrado"; estoque da loja mantido | Sim |
| Loja (10 furadeiras) e papelaria (50 cadernos) consultam estoque, movimentações e caixa | Cada MEI vê somente os próprios dados | Loja: furadeira 10 e saídas 1800.00; papelaria: caderno 50 e saídas 800.00; movimentação da loja dá 404 para a papelaria | Sim |
| `GET /api/v1/estoque` e `GET /api/v1/fluxo-de-caixa` sem token | 401 em Problem Details | 401 "Não autenticado" com `WWW-Authenticate: Bearer` | Sim |

## RF10 — Autenticar os usuários e garantir que cada MEI acesse somente os próprios dados

Change: `autenticacao-multitenant`. Data: 03/11/2026.

| Ação executada | Resultado esperado | Resultado obtido | Atendido |
|---|---|---|---|
| `POST /api/v1/auth/cadastro` com "Loja de Ferramentas", "Carlos Silva", "carlos@lojaferramentas.com.br" e senha "Senha@2026" | 201 com os dados da conta, sem senha | 201 com nome do negócio, nome e e-mail, sem senha nem hash | Sim |
| Novo cadastro com " Carlos@LojaFerramentas.com.br " | 409, sem criar MEI nem usuário | 409 "E-mail já cadastrado"; contagens de `mei` e `usuario` inalteradas (`CadastroTest`) | Sim |
| `POST /api/v1/auth/login` com e-mail e senha corretos | 200 com token, tipo "Bearer" e validade | 200 com token, tipo "Bearer" e validade de 3600 segundos | Sim |
| Login com senha errada e com e-mail não cadastrado | 401 com a mesma mensagem nos dois casos, sem token | 401 "Credenciais inválidas" / "E-mail ou senha inválidos." nos dois casos (`LoginTest`) | Sim |
| `GET /api/v1/conta` sem token | 401 em Problem Details, em português | 401 "Não autenticado", `instance` `/api/v1/conta` e `WWW-Authenticate: Bearer` | Sim |
| `GET /api/v1/conta` com o token do login | 200 com os dados da Loja de Ferramentas | 200 com "Loja de Ferramentas", "Carlos Silva" e o e-mail cadastrado | Sim |
| `GET /api/v1/conta` com token adulterado, expirado, sem MEI e de MEI inexistente | 401 nos quatro casos | 401 nos quatro casos (`ContaEIsolamentoTest`) | Sim |

## RNF01 — Armazenar as senhas exclusivamente na forma de hash BCrypt

| Ação executada | Resultado esperado | Resultado obtido | Atendido |
|---|---|---|---|
| Cadastro com a senha "Senha@2026" e consulta da coluna `usuario.senha_hash` | Valor diferente da senha, no formato BCrypt e correspondente a ela | Hash com prefixo `$2a$`, diferente da senha e reconhecido pelo `PasswordEncoder` (`CadastroTest`) | Sim |

## RNF07 — Isolar os dados de cada MEI em uma arquitetura multi-tenant

| Ação executada | Resultado esperado | Resultado obtido | Atendido |
|---|---|---|---|
| "Loja de Ferramentas" e "Papelaria Central" consultam `GET /api/v1/conta` com os próprios tokens | Cada MEI recebe somente os próprios dados | Cada um recebe apenas o seu nome de negócio e e-mail (`ContaEIsolamentoTest`) | Sim |
| A loja envia `GET /api/v1/conta?tenantId=<id da Papelaria Central>` | O parâmetro é ignorado e a loja recebe os próprios dados | Resposta com os dados da Loja de Ferramentas (`ContaEIsolamentoTest`) | Sim |
| Token da loja com o `tenant_id` trocado pelo da papelaria após a emissão | 401, sem acesso aos dados da papelaria | 401 (assinatura inválida) (`ContaEIsolamentoTest`) | Sim |
