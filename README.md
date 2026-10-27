# Valoriza MEI

SaaS de gestão para o Microempreendedor Individual (MEI): precificação por markup, controle de estoque, fluxo de caixa, alertas de ruptura e sugestões sazonais. Projeto Final do curso de Análise e Desenvolvimento de Sistemas (UNISINOS).

## Estado atual

| Item | Situação |
|---|---|
| Base da API (`fundacao-api`) | Pronta: PostgreSQL 15, Liquibase, Testcontainers, OpenAPI/Swagger e erros padronizados |
| Autenticação e multi-tenant | Próxima etapa |
| Produtos, despesas, precificação, estoque, fluxo de caixa e alertas | Planejados (ver `openspec/`) |

Ainda não há endpoints de negócio: o Swagger abre sem rotas e qualquer caminho em `/api/v1` responde 404 no formato padrão de erro.

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

   O esperado é `BUILD SUCCESS` com 59 testes.

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

### Alternativa: banco persistente com `compose.yaml`

```powershell
.\mvnw.cmd spring-boot:run
```

Usa o PostgreSQL 15 do `compose.yaml` (dados mantidos entre execuções, porta 5432, banco/usuário/senha `valorizamei`). Exige o comando `docker` disponível no Windows, como no Docker Desktop; com o Docker apenas no WSL, use o `spring-boot:test-run`.

## Comandos úteis

| Comando | Para que serve |
|---|---|
| `.\mvnw.cmd verify` | Compila e roda todos os testes |
| `.\mvnw.cmd test -Dtest=ArquiteturaTest` | Só as regras de arquitetura (não precisa de Docker) |
| `.\mvnw.cmd spring-boot:test-run` | Sobe a API com banco descartável |
| `.\mvnw.cmd -U verify` | Força o Maven a tentar de novo downloads que falharam antes |

## Problemas comuns

| Sintoma | Causa provável e solução |
|---|---|
| `Could not find a valid Docker environment` nos testes | Docker não acessível. Abra o terminal do Ubuntu, confira `curl.exe http://localhost:2375/version` e o arquivo `.testcontainers.properties`. |
| `Cannot run program "docker"` ao subir | Foi usado `spring-boot:run` sem o comando `docker` no Windows. Use `spring-boot:test-run`. |
| `status code: 403` ao baixar dependências | O Maven está usando outro repositório (ex.: Artifactory). Confira se `.mvn/maven.config` existe e rode com `-U`. |
| `Port 8080 was already in use` | Outra instância está rodando. Pare-a com `Ctrl+C` ou feche o terminal dela. |

## Organização do código

- `br.com.valorizamei.<feature>.{controller,service,factory,repository,model,dto}`: cada funcionalidade em seu pacote, com camadas verificadas pelos testes de arquitetura (ArchUnit).
- `br.com.valorizamei.shared`: código transversal (configuração da OpenAPI e tratamento de erros); não depende de nenhuma feature.
- `src/main/resources/db/changelog`: migrações Liquibase (o schema só muda por elas; `ddl-auto=validate`).
- `openspec/`: especificações e mudanças planejadas (fluxo spec-driven do OpenSpec).
