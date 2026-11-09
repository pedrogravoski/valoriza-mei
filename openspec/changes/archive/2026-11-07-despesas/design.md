# Design

## Context

O padrão de feature isolada por tenant já existe na `cadastro-produtos`:
- tabela com `tenant_id` e índice único por expressão;
- repository só com métodos filtrados por tenant;
- 404 para registro de outro MEI;
- `ErroDeNegocioException` para 404 e 409;
- limpeza dos testes em `BancoDeTeste`.

As fórmulas do artigo (seção 2.2) usam percentuais em pontos percentuais: `Markup = 100 / [100 - (DV + DF + ML)]` e `DF = (soma das despesas fixas mensais / receita mensal estimada) × 100`. A motivação está em proposal.md, e o comportamento esperado, nas specs `despesas` e `parametros-financeiros`.

Decisões tomadas com o autor antes do design:
- a despesa variável é sempre um percentual sobre a venda;
- a receita mensal estimada fica gravada nos parâmetros do MEI;
- a margem de lucro entra nesta change, junto com a receita.

## Goals / Non-Goals

**Goals:**

- Entregar à change de precificação os três componentes do markup (DV, DF e ML) prontos, já validados e isolados por MEI.
- Manter valores em reais e percentuais em `BigDecimal` com escala fixa, sem arredondamento silencioso na entrada.

**Non-Goals:**

- Cálculo do markup e do preço de venda (change de precificação, RF03 e RF04), incluindo a verificação de que DV + DF + ML é menor que 100%.
- Despesa variável em valor por venda (decisão do autor: só percentual).
- Despesas com vigência ou histórico mensal, pagamento de despesas e reflexo no fluxo de caixa.
- Receita realizada a partir das vendas: a receita usada no rateio é a estimada, informada pelo MEI.
- Paginação da listagem de despesas.

## Decisions

1. **Feature `br.com.valorizamei.despesa`.** Despesas, parâmetros financeiros e rateio ficam na mesma feature, porque o rateio combina os três e eles só existem para formar o markup.
   - Controllers: `DespesaController` (`/api/v1/despesas`) e `ParametrosFinanceirosController` (`/api/v1/parametros-financeiros`).
   - Services: `DespesaService`, `ParametrosFinanceirosService` e `RateioService`.
   - Factories, repositories, entidades `Despesa` e `ParametrosFinanceiros`, e os DTOs.
   - A change de precificação vai acessar o rateio e os parâmetros pelo service desta feature, como permitem as regras de arquitetura.
   - *Alternativa descartada:* duas features (`despesa` e `parametro`). A separação criaria dependência entre features só para o rateio, sem ganho.

2. **Uma tabela de despesas com a classificação e o campo de valor de cada tipo.**
   - `despesa`:
     - `id uuid` PK (`pk_despesa`);
     - `tenant_id uuid not null` com FK `fk_despesa_mei` para `mei(id)`;
     - `descricao varchar(120) not null`;
     - `tipo varchar(10) not null`, com os valores `FIXA` e `VARIAVEL`;
     - `valor_mensal numeric(12,2)` e `percentual numeric(5,2)`, ambos podendo ser nulos;
     - `criado_em` e `atualizado_em` (`timestamptz`).
   - Restrições no banco, como segunda barreira:
     - `ck_despesa_tipo` só aceita `FIXA` e `VARIAVEL`;
     - `ck_despesa_valor` exige, para `FIXA`, `valor_mensal > 0` e `percentual` nulo, e, para `VARIAVEL`, `0 < percentual < 100` e `valor_mensal` nulo.
   - Índice único `uk_despesa_tenant_descricao` em `(tenant_id, lower(descricao))`.
   - *Alternativa descartada:* um único campo `valor` com significado dependente do tipo (reais ou percentual). Ele é ambíguo para o frontend e na documentação.
   - *Alternativa descartada:* tabelas e endpoints separados para fixas e variáveis. O RF02 trata as duas como despesas com classificação, e o filtro por tipo atende às telas separadas.

3. **Parâmetros financeiros, um registro por MEI.**
   - `parametros_financeiros`:
     - `tenant_id uuid` PK (`pk_parametros_financeiros`) com FK `fk_parametros_financeiros_mei` para `mei(id)`;
     - `receita_mensal_estimada numeric(12,2) not null` com `> 0`;
     - `margem_lucro numeric(5,2) not null` com `>= 0` e `< 100`;
     - `atualizado_em timestamptz not null`.
   - A chave é o próprio `tenant_id`: não há identificador na URL, e o MEI só alcança o próprio registro.
   - `GET /api/v1/parametros-financeiros` responde 200 com `receitaMensalEstimada` e `margemDeLucro`. Os dois vêm nulos enquanto o MEI não os informar, para o frontend exibir o formulário vazio sem tratar 404.
   - `PUT /api/v1/parametros-financeiros` grava os dois valores (ambos obrigatórios) e responde 200. Ele cria o registro na primeira vez e o substitui nas seguintes.

4. **Endpoints de despesas.**
   - `POST /api/v1/despesas`: 201 com `Location`.
   - `GET /api/v1/despesas`: lista ordenada pela descrição sem diferenciar maiúsculas e minúsculas. O parâmetro opcional `tipo` (`FIXA` ou `VARIAVEL`) filtra a lista.
   - `GET`, `PUT` e `DELETE /api/v1/despesas/{id}`: mesmo comportamento dos produtos (200, 200 e 204; 404 fora do MEI).
   - O corpo e a resposta têm `descricao`, `tipo`, `valorMensal` e `percentual`; a resposta também traz `id`. O campo que não se aplica ao tipo vem nulo.
   - Um valor de `tipo` desconhecido no corpo gera 400 "Requisição inválida" (corpo ilegível). Na query, o mesmo valor gera 400 por erro de conversão; nos dois casos o `TratadorGlobalDeErros` já responde sem código novo.

5. **Validação da despesa.**
   - `descricao`: obrigatória, até 120 caracteres, sem espaços nas pontas.
   - `tipo`: obrigatório.
   - `valorMensal`: maior que zero, com no máximo 2 casas decimais.
   - `percentual`: maior que zero, menor que 100, com no máximo 2 casas decimais.
   - Regra entre campos, por uma restrição de classe no DTO (`@ValorConformeTipo`), que aponta o erro no campo certo dentro de `erros`:
     - `FIXA` exige `valorMensal` e não aceita `percentual`;
     - `VARIAVEL` exige `percentual` e não aceita `valorMensal`.
   - Os valores são gravados com escala 2 (`1200` vira `1200.00`).
   - Mensagens próprias em português: "informe o valor com no máximo 2 casas decimais" (como nos produtos), "informe o valor mensal da despesa fixa", "informe o percentual da despesa variável" e "não se aplica a este tipo de despesa".

6. **Classificação no modelo e no DTO.** As regras de arquitetura não deixam o DTO depender do modelo nem o modelo depender do DTO. Por isso, há um enum `TipoDespesa` em `dto` e um enum `ClassificacaoDespesa` em `model`, com os mesmos valores, convertidos pela factory. A entidade grava o enum como texto (`EnumType.STRING`).

7. **Rateio (`GET /api/v1/despesas/rateio`).**
   - A resposta traz:
     - `totalDespesasFixas`: soma dos valores mensais das fixas, `0.00` sem despesas;
     - `receitaMensalEstimada`: dos parâmetros, ou nulo;
     - `percentualDespesasFixas`: o DF;
     - `percentualDespesasVariaveis`: o DV, soma dos percentuais das variáveis, `0.00` sem despesas.
   - DF = `totalDespesasFixas × 100 / receitaMensalEstimada`, calculado com escala 10 e devolvido com 2 casas (`HALF_UP`). Sem parâmetros informados, `receitaMensalEstimada` e `percentualDespesasFixas` vêm nulos.
   - O `RateioService` expõe o DF sem arredondamento para a change de precificação, que segue a regra do artigo de arredondar só o preço final.
   - O caminho literal `/rateio` tem precedência sobre `/{id}` no Spring MVC.
   - Somas e o DF são feitos em Java sobre as despesas do MEI (poucas dezenas). *Alternativa descartada:* consulta agregada no banco, que adicionaria código para ganho irrelevante.

8. **Isolamento.** Os repositories só expõem métodos filtrados por `tenantId`, e o service obtém o tenant de `TenantAtual`, como nos produtos. Despesas de outro MEI geram 404, e os parâmetros são lidos e gravados pela chave do próprio tenant.

9. **Erros de negócio.**
   - 404: "Despesa não encontrada" / "Não existe despesa com este identificador.".
   - 409: "Despesa já cadastrada" / "Já existe uma despesa com esta descrição.", inclusive por violação do índice único em gravações simultâneas.

10. **Documentação.** `@Tag` "Despesas" e "Parâmetros financeiros", com `@Operation` em português. Os percentuais são descritos como pontos percentuais (`3.00` = 3%).

11. **Testes.**
    - Integração com Testcontainers e MockMvc, usando o cenário da loja de ferramentas:
      - fixas: aluguel 1.200,00, internet e telefone 120,00 e honorários contábeis 80,00;
      - variáveis: taxa de cartão 3% e custo de entrega 2%;
      - receita 14.000,00 e margem 25%.
    - Teste de isolamento entre "Loja de Ferramentas" e "Papelaria Central" nas despesas, no rateio e nos parâmetros.
    - `BancoDeTeste.limpar` passa a apagar `despesa` e `parametros_financeiros` antes de `mei`.
    - `MigracoesBancoTest` verifica as tabelas e as restrições.

## Risks / Trade-offs

- [DF devolvido com 2 casas pode divergir do valor usado no markup] → A resposta é para exibição. A precificação usa o valor sem arredondamento do service, e a spec do rateio fixa o arredondamento de exibição.
- [A soma DV + DF + ML pode chegar a 100% ou mais e tornar o markup impossível] → A validação fica na precificação, onde o markup é calculado. Aqui cada percentual só é limitado a menos de 100.
- [A receita estimada pode ficar desatualizada] → É um parâmetro do MEI, editável a qualquer momento. A receita realizada fica fora do escopo.
- [Descrição única pode incomodar quem tem duas despesas parecidas] → Basta diferenciar a descrição (ex.: "Manutenção do carro" e "Manutenção da loja"). A regra evita somar a mesma despesa duas vezes no rateio.

## Migration Plan

- O changeset `003` cria as tabelas `despesa` e `parametros_financeiros` na inicialização. Não há dados a migrar.
- Para reverter em desenvolvimento, basta recriar o banco (contêiner descartável) e remover o changeset.
