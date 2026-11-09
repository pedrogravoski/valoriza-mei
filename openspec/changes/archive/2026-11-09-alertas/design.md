# Design

## Context

Os dados de entrada já existem:
- `EstoqueService.estoqueAtual()` devolve todos os produtos do MEI com estoque atual e mínimo;
- as movimentações de saída têm data, quantidade e valor total;
- `PrecificacaoService` calcula o preço sugerido de cada produto, mas responde 422 quando faltam os parâmetros financeiros ou a soma DV + DF + ML é de 100% ou mais;
- o `Clock` da aplicação dá a data atual no fuso de São Paulo, e os testes têm a `RelogioFixoConfiguration` em 15/07/2026.

O cenário de referência do `config.yaml` fixa:
- a ruptura quando o estoque atual é menor ou igual ao mínimo;
- o calendário anual com aviso de 30 dias;
- o Dia dos Pais como 2º domingo de agosto (09/08/2026, aviso a partir de 10/07/2026);
- as regras de reforçar estoque (vendas em 30 dias maiores que o estoque) e de promoção (sem vendas e com estoque).

A motivação está em proposal.md, e o comportamento esperado, nas specs `alertas` e `datas-comemorativas`.

Decisões tomadas com o autor antes do design:
- os alertas são calculados a cada consulta;
- as 5 datas principais são fixas, e cada MEI pode cadastrar datas próprias;
- o reajuste é sugerido quando o preço médio praticado está abaixo do preço sugerido;
- o CORS fica numa change separada.

## Goals / Non-Goals

**Goals:**

- Reproduzir o estudo de caso:
  - com a data atual 15/07/2026, a ruptura só da Trena 5 m e o Dia dos Pais como data próxima;
  - sugestões de reforço para a furadeira e a trena, e de promoção para a caixa de ferramentas.
- Manter as regras como funções puras, testáveis sem banco e com qualquer data.
- Não quebrar os alertas quando a precificação não puder ser calculada.

**Non-Goals:**

- Alertas gravados, histórico, "marcar como lido" e notificações (e-mail, push).
- Antecedência configurável: o aviso é sempre de 30 dias.
- Desativar ou alterar as datas fixas.
- Datas personalizadas com ano específico (não recorrentes) ou móveis (ex.: "3º sábado de junho").
- Sugestões por categoria de produto ou por data (as sugestões dependem do giro, não da data).
- CORS para o frontend (change `cors-frontend`).

## Decisions

1. **Duas features: `calendario` e `alerta`.**
   - `calendario`:
     - `DataComemorativaController` (`/api/v1/datas-comemorativas`) e `CalendarioService`;
     - a entidade `DataComemorativa`, o repository, a factory e os DTOs;
     - o cálculo das datas fixas.
   - `alerta`: `AlertaController` (`GET /api/v1/alertas`), `AlertaService` e DTOs, sem persistência.
     - O `AlertaService` usa o `EstoqueService`, o `CalendarioService` e o `PrecificacaoService`, como permite a regra de acesso só por service e dto.
     - Dependências resultantes: alerta → estoque, calendário e precificação; precificação → produto e despesa; estoque → produto e caixa. Não há ciclo.
   - *Alternativa descartada:* uma feature só. O calendário tem cadastro próprio (CRUD por tenant), enquanto os alertas são só leitura e combinam várias features.

2. **Datas fixas calculadas no código.** Em métodos estáticos do `CalendarioService`, para um ano qualquer:
   - Dia das Mães: 2º domingo de maio (10/05/2026, 09/05/2027);
   - Dia dos Pais: 2º domingo de agosto (09/08/2026, 08/08/2027);
   - Dia das Crianças: 12/10;
   - Black Friday: sexta-feira seguinte à 4ª quinta-feira de novembro (27/11/2026, 26/11/2027);
   - Natal: 25/12.
   - *Alternativa descartada:* gravar as datas fixas no banco para cada ano. Exigiria carga anual e não traria ganho.

3. **Datas personalizadas por MEI (changeset `005-criar-tabela-data-comemorativa`).**
   - `data_comemorativa`:
     - `id uuid` PK (`pk_data_comemorativa`);
     - `tenant_id` FK `fk_data_comemorativa_mei`;
     - `nome varchar(80) not null`;
     - `dia smallint not null` e `mes smallint not null`;
     - `criado_em` e `atualizado_em` (`timestamptz`);
     - `ck_data_comemorativa_dia_mes`: mês de 1 a 12 e dia de 1 a 31;
     - índice único `uk_data_comemorativa_tenant_nome` em `(tenant_id, lower(nome))`.
   - A validade do dia no mês é verificada no DTO por uma restrição de classe (`@DiaValidoNoMes`): 31/04 é recusado. Fevereiro aceita até 28, para que a data exista todo ano. O erro é apontado no campo `dia`.
   - Nome repetido no MEI, sem diferenciar maiúsculas e minúsculas, gera 409. A restrição vale só entre as personalizadas; o nome pode coincidir com o de uma data fixa.

4. **Endpoints do calendário.**
   - `GET /api/v1/datas-comemorativas`:
     - devolve as datas fixas e as personalizadas do MEI, ordenadas pela próxima ocorrência a partir da data atual;
     - cada item traz `id` (nulo nas fixas), `nome`, `dia`, `mes`, `proximaOcorrencia`, `diasRestantes` e `personalizada`;
     - uma data que cai hoje tem próxima ocorrência hoje (`diasRestantes` 0).
   - `POST /api/v1/datas-comemorativas` (201 com `Location`), `GET`, `PUT` e `DELETE /api/v1/datas-comemorativas/{id}`: só datas personalizadas do MEI. Um id inexistente ou de outro MEI gera 404.
   - Próxima ocorrência: a data deste ano, se ainda não passou; senão, a do ano seguinte. A conta é feita em métodos estáticos testáveis.

5. **Datas próximas.**
   - `CalendarioService.proximasDatas()` devolve as datas, fixas e personalizadas, cuja próxima ocorrência está entre hoje e hoje + 30 dias (inclusive).
   - Com a data atual 15/07/2026, o Dia dos Pais (09/08) está a 25 dias. Em 09/07 ele estaria a 31 dias e ficaria fora; em 10/07, a 30 dias, entra.

6. **Giro: vendas por produto no período.**
   - O `EstoqueService` ganha `vendasPorProduto(de, ate)`, que devolve, para cada produto com saídas no período, `produtoId`, `quantidade` e `valorTotal`.
   - É uma consulta agregada (`group by produto_id`) filtrada por tenant.
   - O período "últimos 30 dias" vai de hoje − 29 até hoje (30 dias corridos, inclusive).
   - O `quantidadeVendida(produtoId, de, ate)` da change anterior continua disponível.

7. **Regras de sugestão (funções puras no `AlertaService`).** São aplicadas a cada produto, com estoque atual, vendas e valor vendido nos últimos 30 dias e o preço sugerido (opcional).
   - `REFORCAR_ESTOQUE` quando as vendas superam o estoque atual. Motivo: "Vendeu 6 unidades nos últimos 30 dias e tem 5 em estoque."
   - `PROMOCAO` quando não houve vendas e o estoque é maior que zero. Motivo: "Nenhuma venda nos últimos 30 dias, com 10 unidades em estoque."
   - `REAJUSTE_PRECO` quando houve vendas e o preço médio (valor vendido / quantidade, 2 casas, `HALF_UP`) é menor que o preço sugerido. Motivo: "Preço médio de venda de R$ 280,00 nos últimos 30 dias, abaixo do preço sugerido de R$ 300,00."
   - Um produto pode receber mais de uma sugestão, por exemplo reforço e reajuste.
   - As sugestões são ordenadas pelo nome do produto e pela ordem acima.
   - Sem data comemorativa próxima, nenhuma sugestão é gerada: o RF09 liga as sugestões às datas.

8. **Precificação opcional para os alertas.**
   - O `PrecificacaoService` ganha `consultarSeDisponivel()`, que devolve `Optional<PrecificacaoResponse>`, vazio quando os parâmetros não foram informados ou o markup é inviável.
   - O cálculo dos percentuais passa a ter uma versão que devolve `Optional` em vez de lançar a exceção. O `consultar()` mantém as respostas 422.
   - Sem precificação, a regra de reajuste não é aplicada, e as demais sugestões e a ruptura continuam.
   - *Alternativa descartada:* falhar o `GET /api/v1/alertas` com 422. O MEI ficaria sem o alerta de ruptura só por não ter informado a margem.

9. **Ruptura (RF08).**
   - Produtos com estoque mínimo maior que zero e estoque atual menor ou igual ao mínimo, em ordem alfabética, com `produtoId`, `nome`, `estoqueAtual` e `estoqueMinimo`.
   - Estoque mínimo 0 significa que o MEI não quer alerta, como definido na `cadastro-produtos`.
   - Um produto sem movimentações (estoque 0) com mínimo maior que zero está em ruptura.
   - A ruptura não depende de data comemorativa.

10. **Resposta de `GET /api/v1/alertas`.**
    - `dataReferencia`: a data atual;
    - `ruptura`: lista;
    - `datasComemorativas`: as próximas, com `nome`, `data`, `diasRestantes` e `personalizada`, ordenadas pela data;
    - `sugestoes`: lista de `produtoId`, `nome`, `tipo` e `motivo`.
    - Sempre 200; as listas vazias indicam que não há alerta.

11. **Documentação.** `@Tag` "Alertas" e "Datas comemorativas", com `@Operation` em português descrevendo as regras.

12. **Testes.**
    - Unitários (sem Spring):
      - datas fixas de 2026 e 2027;
      - próxima ocorrência, incluindo a virada do ano;
      - janela de 30 dias (limites de 30 e 31 dias);
      - regras de sugestão, incluindo reajuste e produto com duas sugestões.
    - Integração com a `RelogioFixoConfiguration` (15/07/2026) e o estoque do estudo de caso do `ApoioDeEstoque`.
    - Isolamento entre "Loja de Ferramentas" e "Papelaria Central".
    - `BancoDeTeste.limpar` apaga `data_comemorativa` antes de `mei`.

## Risks / Trade-offs

- [Cálculo a cada consulta lê estoque, vendas e precificação] → Volumes pequenos por MEI e consultas indexadas por tenant. Cache só se houver necessidade medida.
- [Sugestões iguais para todas as datas próximas] → Simplifica a regra e segue o artigo, que liga as sugestões ao giro de cada produto. Sugestões por data ficam como evolução.
- [Black Friday definida como a sexta após a 4ª quinta de novembro] → É a regra usada no Brasil, que segue a data americana, e coincide com a última sexta de novembro na maioria dos anos.
- [Fevereiro limitado a 28 nas datas personalizadas] → Evita uma data que não existe em anos não bissextos. 29/02 é recusado com mensagem clara.

## Migration Plan

- O changeset `005` cria a tabela `data_comemorativa` na inicialização. Não há dados a migrar.
- Para reverter em desenvolvimento, basta recriar o banco (contêiner descartável) e remover o changeset.
