# Valoriza MEI

SaaS de gestão para o Microempreendedor Individual (MEI): precificação por markup, controle de estoque, fluxo de caixa, alertas de ruptura e sugestões sazonais. Projeto Final do curso de Análise e Desenvolvimento de Sistemas (UNISINOS).

## Estado atual

| Item | Situação |
|---|---|
| Base da API (`fundacao-api`) | Pronta: PostgreSQL 15, Liquibase, Testcontainers, OpenAPI/Swagger e erros padronizados |
| Autenticação e multi-tenant (`autenticacao-multitenant`) | Pronta: cadastro, login com JWT, senhas em BCrypt e isolamento por MEI |
| Produtos (`cadastro-produtos`) | Pronta: cadastro, listagem, consulta, alteração e exclusão, com custo direto e estoque mínimo |
| Despesas (`despesas`) | Pronta: despesas fixas e variáveis, receita mensal estimada, margem de lucro e rateio (DF e DV) |
| Precificação (`precificacao`) | Pronta: índice de markup e preço de venda sugerido dos produtos |
| Estoque e fluxo de caixa (`estoque-fluxo-caixa`) | Pronta: compras e vendas, estoque atual e fluxo de caixa por período |
| Alertas (`alertas`) | Pronta: ruptura de estoque, datas comemorativas (fixas e personalizadas) e sugestões de reforço, promoção e reajuste |
| CORS para o frontend Angular (`cors-frontend`) | Pronta: origens configuráveis (padrão `http://localhost:4200`) |

Endpoints disponíveis:

| Endpoint | Acesso |
|---|---|
| `POST /api/v1/auth/cadastro`, `POST /api/v1/auth/login` | Públicos |
| `GET /api/v1/conta` | Exige token |
| `POST /api/v1/produtos`, `GET /api/v1/produtos` | Exige token |
| `GET`, `PUT` e `DELETE /api/v1/produtos/{id}` | Exige token |
| `POST /api/v1/despesas`, `GET /api/v1/despesas` (filtro opcional `?tipo=FIXA` ou `VARIAVEL`) | Exige token |
| `GET`, `PUT` e `DELETE /api/v1/despesas/{id}` | Exige token |
| `GET /api/v1/despesas/rateio` | Exige token |
| `GET` e `PUT /api/v1/parametros-financeiros` | Exige token |
| `GET /api/v1/precificacao`, `GET /api/v1/precificacao/produtos/{id}` | Exige token |
| `POST /api/v1/estoque/movimentacoes`, `GET /api/v1/estoque/movimentacoes` (filtro opcional `?produtoId=`) | Exige token |
| `GET /api/v1/estoque/movimentacoes/{id}`, `GET /api/v1/estoque` | Exige token |
| `GET /api/v1/fluxo-de-caixa` (período opcional `?inicio=AAAA-MM-DD&fim=AAAA-MM-DD`) | Exige token |
| `GET /api/v1/alertas` | Exige token |
| `GET` e `POST /api/v1/datas-comemorativas`; `GET`, `PUT` e `DELETE /api/v1/datas-comemorativas/{id}` | Exige token |

Qualquer outra rota em `/api/v1` exige token e responde no formato padrão de erro.

## Tecnologias

Java 21, Spring Boot 4.1.1, Maven (wrapper), PostgreSQL 15, Liquibase, springdoc-openapi 3 (OpenAPI 3.1 e Swagger UI), Testcontainers, JUnit 5 e ArchUnit.

## Pré-requisitos

1. **Java 21** (JDK) no `PATH`. Confira com `java -version`.
2. **Docker**, de uma das duas formas:
   - **Docker Desktop** para Windows: basta estar aberto.
   - **Docker Engine dentro do WSL (Ubuntu)**: exige a configuração única da seção abaixo.
3. **Acesso ao Maven Central.** O projeto usa o repositório central padrão, configurado em `.mvn/settings.xml` e ativado por `.mvn/maven.config`. As configurações de `~/.m2/settings.xml` (como um Artifactory corporativo) não são usadas neste projeto.

Não é preciso instalar Maven nem PostgreSQL: o wrapper (`mvnw.cmd`) baixa o Maven e o banco roda em contêiner.

## Configuração única: Docker do WSL (sem Docker Desktop)

O Java do Windows não enxerga o Docker do WSL sozinho. É preciso expor o daemon em `127.0.0.1:2375`.

**No terminal do Ubuntu (WSL):**

```bash
sudo mkdir -p /etc/systemd/system/docker.service.d
```

```bash
printf '[Service]\nExecStart=\nExecStart=/usr/bin/dockerd -H fd:// -H tcp://127.0.0.1:2375 --containerd=/run/containerd/containerd.sock\n' | sudo tee /etc/systemd/system/docker.service.d/override.conf
```

```bash
sudo systemctl daemon-reload && sudo systemctl restart docker
```

**No PowerShell (Windows)**, crie o arquivo que indica ao Testcontainers onde está o Docker:

```powershell
Set-Content -Path "$env:USERPROFILE\.testcontainers.properties" -Value "docker.host=tcp://localhost:2375" -Encoding ascii
```

**Confira** no PowerShell; deve aparecer um JSON com a versão do Docker:

```powershell
curl.exe http://localhost:2375/version
```

A porta só aceita conexões da própria máquina (`127.0.0.1`), mas não tem autenticação.

## Passo a passo para rodar

Todos os comandos abaixo são executados na pasta do projeto, no PowerShell.

1. **Se usar o Docker do WSL**, abra um terminal do Ubuntu e deixe-o aberto. Quando todos os terminais do WSL fecham, o WSL desliga e o Docker para junto.

2. **Rode os testes** para confirmar que o ambiente está correto (o primeiro build baixa as dependências e a imagem `postgres:15`):

   ```powershell
   .\mvnw.cmd verify
   ```

   O esperado é `BUILD SUCCESS` com 316 testes.

3. **Suba a aplicação:**

   ```powershell
   .\mvnw.cmd spring-boot:test-run
   ```

   Esse comando sobe a API em <http://localhost:8080> com um PostgreSQL 15 criado pelo Testcontainers (o banco é descartado ao parar a aplicação). Funciona com qualquer uma das opções de Docker. A aplicação está pronta quando aparecer `Started ValorizaMeiApplication`.

4. **Acesse:**

   | Endereço | O que é |
   |---|---|
   | <http://localhost:8080/swagger-ui.html> | Interface Swagger UI da API |
   | <http://localhost:8080/v3/api-docs> | Especificação OpenAPI 3.1 (JSON) |
   | <http://localhost:8080/actuator/health> | Saúde da aplicação (`{"status":"UP"}`) |

5. **Para parar**, use `Ctrl+C` no terminal da aplicação.

### Autenticação pelo Swagger UI

Todos os endpoints em `/api/v1`, exceto o cadastro e o login, exigem o token no cabeçalho `Authorization: Bearer <token>`.

1. Em **Autenticação → `POST /api/v1/auth/cadastro`**, clique em **Try it out** e envie, por exemplo:

   ```json
   {"nomeNegocio": "Loja de Ferramentas", "nome": "Carlos Silva", "email": "carlos@lojaferramentas.com.br", "senha": "Senha@2026"}
   ```

   A senha precisa ter de 8 a 72 caracteres. Com o `spring-boot:test-run` o banco é recriado a cada subida, então o cadastro precisa ser refeito.

2. Em **`POST /api/v1/auth/login`**, envie o e-mail e a senha e copie o valor de `token` da resposta.
3. Clique em **Authorize** (cadeado, no topo da página), cole o token (sem a palavra `Bearer`) e confirme.
4. Em **Conta → `GET /api/v1/conta`**, execute: a resposta traz os dados da conta do token.

O token vale 60 minutos (`valorizamei.seguranca.jwt.validade`). Depois disso, faça login de novo.

### Produtos pelo Swagger UI

Com o token informado no **Authorize**, em **Produtos → `POST /api/v1/produtos`**, envie, por exemplo:

```json
{"nome": "Furadeira de impacto 650 W", "custoDireto": 180.00, "estoqueMinimo": 3}
```

- O custo direto é em reais, maior que zero e com no máximo 2 casas decimais. O estoque mínimo é um inteiro maior ou igual a zero.
- O nome não pode se repetir entre os produtos do mesmo MEI, sem diferenciar maiúsculas e minúsculas (resposta 409).
- A resposta traz o `id` do produto, usado em `GET`, `PUT` e `DELETE /api/v1/produtos/{id}`. O `GET /api/v1/produtos` lista os produtos em ordem alfabética.
- Cada MEI só enxerga os próprios produtos: o `id` de um produto de outro MEI responde 404.

### Despesas e rateio pelo Swagger UI

Os percentuais são em pontos percentuais: `3.00` é 3%.

1. Em **Parâmetros financeiros → `PUT /api/v1/parametros-financeiros`**, informe a receita mensal estimada e a margem de lucro desejada:

   ```json
   {"receitaMensalEstimada": 14000.00, "margemDeLucro": 25.00}
   ```

2. Em **Despesas → `POST /api/v1/despesas`**, registre as despesas. A fixa informa só o `valorMensal`; a variável, só o `percentual` sobre o preço de venda:

   ```json
   {"descricao": "Aluguel", "tipo": "FIXA", "valorMensal": 1200.00}
   ```

   ```json
   {"descricao": "Taxa de cartão", "tipo": "VARIAVEL", "percentual": 3.00}
   ```

3. Em **`GET /api/v1/despesas/rateio`**, consulte o rateio. Com as despesas do estudo de caso (aluguel 1.200,00, internet e telefone 120,00, honorários 80,00, cartão 3% e entrega 2%) e receita 14.000,00, o resultado é `totalDespesasFixas` 1400.00, `percentualDespesasFixas` (DF) 10.00 e `percentualDespesasVariaveis` (DV) 5.00.

- Enquanto a receita não for informada, o rateio traz `receitaMensalEstimada` e `percentualDespesasFixas` nulos.
- A descrição da despesa não pode se repetir no mesmo MEI, sem diferenciar maiúsculas e minúsculas (resposta 409).

### Precificação pelo Swagger UI

Com produtos, despesas e parâmetros financeiros informados, em **Precificação → `GET /api/v1/precificacao`**, a resposta traz os percentuais (DV, DF e ML), o `indiceMarkup` e o `precoSugerido` de cada produto.

- Fórmulas da seção 2.2 do artigo: `Markup = 100 / [100 - (DV + DF + ML)]` e `PV = custo direto × markup`.
- O markup é exibido com 4 casas, mas o preço usa o valor sem arredondamento e só é arredondado ao final. No estudo de caso, o markup é 1,6667 e a furadeira de custo 180,00 tem preço sugerido 300,00, e não 300,01.
- `GET /api/v1/precificacao/produtos/{id}` traz o preço sugerido de um único produto.
- O preço é calculado a cada consulta: ao alterar o custo de um produto, uma despesa ou a margem, o preço sugerido já reflete o novo valor.

### Estoque e fluxo de caixa pelo Swagger UI

1. Em **Estoque → `POST /api/v1/estoque/movimentacoes`**, registre uma compra (entrada). O valor unitário é o custo direto do produto, então não é informado:

   ```json
   {"produtoId": "<id da furadeira>", "tipo": "ENTRADA", "quantidade": 10}
   ```

2. Registre uma venda (saída), informando o valor unitário da venda:

   ```json
   {"produtoId": "<id da furadeira>", "tipo": "SAIDA", "quantidade": 2, "valorUnitario": 300.00}
   ```

   A resposta traz o `estoqueAtual` do produto (8, neste exemplo).

3. Em **`GET /api/v1/estoque`**, veja o estoque atual e o estoque mínimo de todos os produtos.
4. Em **Fluxo de caixa → `GET /api/v1/fluxo-de-caixa`**, veja os lançamentos do mês atual. A compra aparece como saída de caixa de 1800.00 e a venda, como entrada de 600.00, com saldo -1200.00.

Detalhes:
- A data da movimentação é opcional (padrão: hoje), pode ser de um dia anterior e não pode ser futura (`"data": "2026-07-10"`).
- Uma venda maior que o estoque atual é recusada com 422, e nada é gravado.
- As movimentações não podem ser alteradas nem excluídas.
- Um produto com movimentações não pode mais ser excluído (409).

### Alertas e datas comemorativas pelo Swagger UI

Em **Alertas → `GET /api/v1/alertas`**, a resposta traz:
- `ruptura`: produtos com estoque atual menor ou igual ao mínimo. Um produto com estoque mínimo 0 nunca gera esse alerta.
- `datasComemorativas`: as que acontecem nos próximos 30 dias. São fixas o Dia das Mães, o Dia dos Pais, o Dia das Crianças, a Black Friday e o Natal, e entram também as datas cadastradas pelo MEI.
- `sugestoes`: aparecem só quando há data comemorativa próxima e usam as vendas dos últimos 30 dias de cada produto:
  - `REFORCAR_ESTOQUE`: as vendas superaram o estoque atual;
  - `PROMOCAO`: nenhuma venda, com estoque;
  - `REAJUSTE_PRECO`: preço médio de venda abaixo do preço sugerido. Exige receita e margem informadas.

Os alertas são calculados a cada consulta, com a data de hoje (`dataReferencia`).

Para cadastrar uma data própria, que se repete todo ano, use **Datas comemorativas → `POST /api/v1/datas-comemorativas`**:

```json
{"nome": "Aniversário da cidade", "dia": 1, "mes": 8}
```

`GET /api/v1/datas-comemorativas` lista as datas fixas (com `id` nulo, que não podem ser alteradas) e as próprias, ordenadas pela próxima ocorrência. Fevereiro aceita até o dia 28, para que a data exista todo ano.

### Segredo do token

O token é assinado com o segredo `valorizamei.seguranca.jwt.segredo`. O `application.yaml` traz um valor padrão que serve **apenas para desenvolvimento e testes**. Em qualquer outro ambiente, defina a variável de ambiente `VALORIZAMEI_JWT_SEGREDO` com no mínimo 32 bytes; a aplicação não sobe com um segredo menor.

### Acesso pelo frontend (CORS)

O frontend Angular roda em outra origem e chama a API pelo navegador. A API só aceita as origens configuradas em `valorizamei.cors.origens-permitidas`, cujo padrão de desenvolvimento é `http://localhost:4200`. Em outro ambiente, defina a variável de ambiente `VALORIZAMEI_CORS_ORIGENS_PERMITIDAS`, com uma ou mais origens exatas separadas por vírgula:

```powershell
$env:VALORIZAMEI_CORS_ORIGENS_PERMITIDAS = "http://localhost:4200,https://app.valorizamei.com.br"
```

Como funciona:
- A pré-checagem do navegador (`OPTIONS`) não exige token.
- As respostas, inclusive o 401 de token expirado, trazem os cabeçalhos CORS, então o frontend consegue ler o erro e voltar para o login.
- O cabeçalho `Location` dos cadastros fica visível para o frontend.
- Uma origem não configurada recebe 403 "Origem não permitida".
- O Swagger UI e clientes como o curl não são afetados.

### Banco persistente para o dia a dia (`compose.yaml`)

O `spring-boot:test-run` recria o banco a cada subida. Para manter os dados entre execuções, use o PostgreSQL 15 do `compose.yaml`:
- porta 5432;
- banco, usuário e senha `valorizamei`;
- dados no volume `valorizamei-dados`, que sobrevive a `docker compose down`.

**Com o Docker do WSL:**

1. **Abra um terminal do Ubuntu e deixe-o aberto.** O WSL desliga quando nenhum terminal do Ubuntu está aberto, e o Docker e o banco param junto (o DBeaver mostra `Connection to localhost:5432 refused`). Os dados não se perdem: ficam no volume.

2. **Só na primeira vez**, crie o banco. Rode no PowerShell, **na pasta do projeto**, onde está o `compose.yaml`:

   ```powershell
   wsl docker compose up -d
   ```

   O contêiner tem `restart: unless-stopped`. Nas próximas vezes, ele volta sozinho assim que o terminal do Ubuntu é aberto, sem repetir este passo. Confira com `wsl docker ps`: deve aparecer `valoriza-mei-postgres-1` com `0.0.0.0:5432->5432/tcp`.

3. Suba a API com o perfil `local`, que usa esse banco em vez de subir o compose pelo Windows:

   ```powershell
   .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
   ```

Para parar o banco, rode `wsl docker compose stop` na pasta do projeto. Ele só volta com um novo `wsl docker compose up -d`. Já `wsl docker compose down -v` apaga os dados.

O perfil `local` (`application-local.yaml`) aceita outro banco pelas variáveis `VALORIZAMEI_DB_URL`, `VALORIZAMEI_DB_USUARIO` e `VALORIZAMEI_DB_SENHA`.

**Com o Docker Desktop**, basta `.\mvnw.cmd spring-boot:run`: o Spring Boot sobe o `compose.yaml` sozinho.

**DBeaver ou outro cliente SQL:** nova conexão PostgreSQL com host `localhost`, porta `5432`, e `valorizamei` no banco de dados, no usuário e na senha. As tabelas ficam no schema `public`.

## Comandos úteis

| Comando | Para que serve |
|---|---|
| `.\mvnw.cmd verify` | Compila e roda todos os testes |
| `.\mvnw.cmd test -Dtest=ArquiteturaTest` | Só as regras de arquitetura (não precisa de Docker) |
| `.\mvnw.cmd spring-boot:test-run` | Sobe a API com banco descartável |
| `wsl docker compose up -d` | Sobe o banco persistente no Docker do WSL |
| `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"` | Sobe a API com o banco persistente |
| `.\mvnw.cmd -U verify` | Força o Maven a tentar de novo downloads que falharam antes |

## Problemas comuns

| Sintoma | Causa provável e solução |
|---|---|
| `Could not find a valid Docker environment` nos testes | Docker não acessível. Abra o terminal do Ubuntu, confira `curl.exe http://localhost:2375/version` e o arquivo `.testcontainers.properties`. |
| `Cannot run program "docker"` ao subir | Foi usado `spring-boot:run` sem o comando `docker` no Windows. Use o perfil `local` com o banco subido no WSL (`wsl docker compose up -d`), ou o `spring-boot:test-run`. |
| `Connection to localhost:5432 refused` com o perfil `local` ou no DBeaver | O WSL desligou e o banco parou junto. Abra um terminal do Ubuntu e deixe-o aberto: o banco volta sozinho. Se não voltar (ex.: depois de um `docker compose stop`), rode `wsl docker compose up -d` na pasta do projeto. |
| `no configuration file provided: not found` | O `wsl docker compose` foi rodado fora da pasta do projeto. Entre na pasta (`cd` até `valoriza-mei`) ou use `wsl --cd "<pasta do projeto>" docker compose up -d`. |
| `status code: 403` ao baixar dependências | O Maven está usando outro repositório (ex.: Artifactory). Confira se `.mvn/maven.config` existe e rode com `-U`. |
| `Port 8080 was already in use` | Outra instância está rodando. Pare-a com `Ctrl+C` ou feche o terminal dela. |
| `401` com título "Não autenticado" | Token ausente, expirado ou de uma conta que não existe mais (o banco do `test-run` é recriado a cada subida). Faça o cadastro e o login de novo e use o novo token no **Authorize**. |
| `401` com título "Credenciais inválidas" no login | E-mail ou senha errados. |
| `404` "Produto não encontrado" | O `id` não existe ou é de um produto de outro MEI (o token é de outra conta). |
| `409` "Produto já cadastrado" | Já existe um produto do MEI com o mesmo nome, mesmo com outra grafia de maiúsculas e minúsculas. |
| `400` em `valorMensal` ou `percentual` ao registrar despesa | A despesa `FIXA` exige só o `valorMensal`, e a `VARIAVEL`, só o `percentual`. Envie apenas o campo do tipo escolhido. |
| Rateio com `percentualDespesasFixas` nulo | A receita mensal estimada ainda não foi informada em `PUT /api/v1/parametros-financeiros`. |
| `422` "Parâmetros financeiros não informados" na precificação | Informe a receita mensal estimada e a margem de lucro em `PUT /api/v1/parametros-financeiros`. |
| `422` "Estoque insuficiente" | A venda pede mais unidades do que o estoque atual do produto. Registre antes a compra (entrada) ou reduza a quantidade. |
| `409` "Produto com movimentações" ao excluir | O produto tem compras ou vendas registradas; a exclusão apagaria o histórico de estoque e de caixa. |
| `400` em `data` ao movimentar | A data informada é futura. Use a data de hoje ou de um dia anterior, ou não informe a data. |
| Alertas sem `sugestoes` | Não há data comemorativa nos próximos 30 dias. A ruptura continua sendo informada. |
| Alertas sem `REAJUSTE_PRECO` | A precificação não pode ser calculada (receita ou margem não informadas, ou markup inviável), ou os produtos foram vendidos pelo preço sugerido ou acima dele. |
| `400` em `dia` ao cadastrar data comemorativa | O dia não existe naquele mês em todos os anos (ex.: 31/04 ou 29/02). |
| Navegador bloqueia as chamadas do frontend ("blocked by CORS policy") ou `403` "Origem não permitida" | A origem do frontend não está em `VALORIZAMEI_CORS_ORIGENS_PERMITIDAS`. Informe a origem exata (protocolo, host e porta, sem barra no fim) e reinicie a API. |
| `422` "Markup inviável" | A soma DV + DF + ML chegou a 100% ou mais. Reduza a margem de lucro ou as despesas, ou aumente a receita mensal estimada (que reduz o DF). |

## Organização do código

- `br.com.valorizamei.<feature>.{controller,service,factory,repository,model,dto}`: cada funcionalidade em seu pacote, com camadas verificadas pelos testes de arquitetura (ArchUnit).
- `br.com.valorizamei.autenticacao`: cadastro de conta, login e conta autenticada.
- `br.com.valorizamei.produto`: cadastro dos produtos do MEI.
- `br.com.valorizamei.despesa`: despesas fixas e variáveis, parâmetros financeiros (receita e margem) e rateio.
- `br.com.valorizamei.precificacao`: índice de markup e preço de venda sugerido, calculados a partir dos services de `produto` e `despesa` (sem tabelas próprias).
- `br.com.valorizamei.estoque`: compras e vendas (movimentações) e estoque atual; cada movimentação gera o lançamento de caixa pelo service de `caixa`.
- `br.com.valorizamei.caixa`: lançamentos e consulta do fluxo de caixa por período.
- `br.com.valorizamei.calendario`: datas comemorativas fixas e personalizadas.
- `br.com.valorizamei.alerta`: ruptura e sugestões, calculadas a partir dos services de `estoque`, `calendario` e `precificacao`.
- `br.com.valorizamei.shared`: código transversal (configuração da OpenAPI, tratamento de erros, segurança e acesso ao MEI do token em `TenantAtual`); não depende de nenhuma feature.
- Isolamento entre MEIs: toda tabela de negócio tem `tenant_id`, preenchido e filtrado pelo service a partir de `TenantAtual`, nunca a partir da requisição.
- `src/main/resources/db/changelog`: migrações Liquibase (o schema só muda por elas; `ddl-auto=validate`).
- `openspec/`: especificações e mudanças planejadas (fluxo spec-driven do OpenSpec).
