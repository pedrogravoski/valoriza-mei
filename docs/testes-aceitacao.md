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
