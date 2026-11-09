# Design

## Context

Os dados de entrada já existem nas features anteriores:
- `produto`: `ProdutoService.listar()` e `consultar(id)` devolvem o custo direto com 2 casas, ordenados pelo nome e filtrados pelo MEI do token. Um produto de outro MEI gera 404.
- `despesa`:
  - `RateioService.percentualDespesasFixas()` devolve o DF sem arredondamento (escala 10), vazio sem receita informada;
  - `RateioService.percentualDespesasVariaveis()` devolve o DV;
  - `ParametrosFinanceirosService.consultar()` devolve a margem de lucro, nula enquanto não informada.

O `config.yaml` fixa as regras do artigo (seção 2.2):
- `Markup = 100 / [100 - (DV + DF + ML)]` e `PV = Custo Direto × Markup`, com percentuais em pontos percentuais;
- o PV é calculado com o markup sem arredondamento e arredondado ao final para 2 casas (`HALF_UP`);
- o markup é exibido com 4 casas.

A motivação está em proposal.md, e o comportamento esperado, na spec `precificacao`.

Decisões tomadas com o autor antes do design:
- soma DV + DF + ML de 100% ou mais gera 422 com o motivo;
- parâmetros financeiros não informados geram 422;
- o preço sugerido fica num endpoint próprio, sem mudar a resposta dos produtos.

## Goals / Non-Goals

**Goals:**

- Reproduzir exatamente os valores do estudo de caso: markup 1,6667 e PV 300,00 para a furadeira de custo 180,00.
- Evitar que arredondamentos intermediários (do DF ou do markup) alterem o preço final.
- Não criar dependência das features `produto` e `despesa` em relação à precificação.

**Non-Goals:**

- Gravar o preço de venda praticado ou o histórico de preços.
- Simular preços com outra margem sem gravar os parâmetros.
- Markup por produto ou por categoria: o índice é único para o MEI, como no artigo.
- Sugestões de reajuste de preço (RF09, change de alertas).

## Decisions

1. **Feature `br.com.valorizamei.precificacao` sem persistência.**
   - `PrecificacaoController` (`/api/v1/precificacao`), `PrecificacaoService`, `PrecificacaoFactory` e DTOs; sem repository nem model.
   - O preço é calculado a cada consulta, a partir dos dados atuais, e por isso nunca fica desatualizado em relação ao custo, às despesas ou à margem.
   - O service usa `ProdutoService`, `RateioService` e `ParametrosFinanceirosService`, como permite a regra "outras features só pelas camadas service e dto". As features `produto` e `despesa` não conhecem a precificação, e não há ciclo.
   - *Alternativa descartada:* gravar o preço sugerido no produto. Ele ficaria desatualizado a cada mudança de custo, despesa ou margem, e exigiria recalcular em todas essas operações.

2. **Fórmulas e precisão.**
   - `soma = DV + DF + ML`, com o DF sem arredondamento (escala 10) vindo do `RateioService`.
   - O preço é calculado em uma única divisão: `PV = custoDireto × 100 / (100 − soma)`, arredondado para 2 casas (`HALF_UP`). Matematicamente, é `Custo × Markup` com o markup sem arredondamento, e evita qualquer arredondamento intermediário.
   - O índice exibido é `100 / (100 − soma)` com 4 casas (`HALF_UP`).
   - Os percentuais exibidos (DV, DF e ML) têm 2 casas, como no rateio.
   - Verificação no estudo de caso:
     - soma = 5 + 10 + 25 = 40, markup = 100 / 60 = 1,6667;
     - PV da furadeira = 180 × 100 / 60 = 300,00;
     - Jogo de chaves: 25 × 100 / 60 = 41,67.
   - O efeito do DF sem arredondamento: com fixas 1.000,00, receita 3.000,00, DV 0 e ML 25, a soma é 58,333…, o markup é 2,4000 e o PV de um produto de custo 100,00 é 240,00. Com o DF arredondado (33,33), o PV seria 239,98.
   - As fórmulas ficam em métodos estáticos do `PrecificacaoService`, cobertos por testes unitários.
   - *Alternativa descartada:* calcular o markup com 4 casas e multiplicar pelo custo. Daria 1,6667 × 180 = 300,01, diferente do artigo.

3. **Recusa com 422 quando o markup não pode ser calculado.** O 422 (Unprocessable Content) indica que a requisição é válida, mas os dados do MEI não permitem o cálculo. A resposta é gerada pela `ErroDeNegocioException`.
   - Sem parâmetros financeiros (receita ou margem não informadas): "Parâmetros financeiros não informados" / "Informe a receita mensal estimada e a margem de lucro para calcular o preço de venda.".
   - Soma DV + DF + ML maior ou igual a 100: "Markup inviável" / "A soma das despesas variáveis, das despesas fixas rateadas e da margem de lucro é de X% e precisa ser menor que 100%.". X é exibido com 2 casas e vírgula decimal (ex.: "105,00%").
   - A verificação vale para a lista e para a consulta de um produto.

4. **Endpoints.**
   - `GET /api/v1/precificacao`: devolve
     - `percentualDespesasVariaveis`, `percentualDespesasFixas` e `margemDeLucro` (2 casas);
     - `indiceMarkup` (4 casas);
     - `produtos`: lista com `id`, `nome`, `custoDireto` e `precoSugerido`, na ordem alfabética da listagem de produtos.
   - Sem produtos, `produtos` é uma lista vazia, e o markup é calculado do mesmo jeito.
   - `GET /api/v1/precificacao/produtos/{id}`: devolve `id`, `nome`, `custoDireto`, `indiceMarkup` e `precoSugerido`.
     - O produto é buscado primeiro: um id que não seja de produto do MEI gera 404 "Produto não encontrado", vindo do `ProdutoService`, antes de verificar os parâmetros.
     - Um id fora do formato UUID gera 400 pelo tratador global.

5. **Isolamento.** Todos os dados vêm de services que já filtram pelo MEI do token. A precificação não recebe tenant nem identificador de MEI na requisição.

6. **Documentação.** `@Tag` "Precificação" e `@Operation` em português, com as fórmulas na descrição, e `@ApiResponse` 422 documentado nos dois endpoints.

7. **Testes.**
   - Integração com Testcontainers e MockMvc, montando o estudo de caso completo pela própria API: os 6 produtos, as 5 despesas, receita 14.000,00 e margem 25%. Assim, o teste também verifica a integração entre as três features.
   - Testes unitários das fórmulas: markup, PV, efeito do DF sem arredondamento e formatação da soma no detalhe do 422.
   - Isolamento entre "Loja de Ferramentas" e "Papelaria Central" com parâmetros diferentes.

## Risks / Trade-offs

- [Cada consulta lê produtos, despesas e parâmetros] → São poucas consultas indexadas por tenant e volumes pequenos (dezenas de registros por MEI). Cache fica para quando houver necessidade medida.
- [O preço muda sozinho quando o MEI altera custo, despesa ou margem] → É o comportamento desejado para um preço *sugerido*; o preço praticado não é registrado nesta change.
- [Soma muito próxima de 100% gera markups enormes (ex.: 99,99% → 10.000)] → Matematicamente correto pela fórmula do artigo. O front pode destacar markups altos; a regra de negócio só recusa a partir de 100%.

## Migration Plan

- Sem mudanças no banco. A change adiciona apenas código e endpoints novos.
- Para reverter, basta remover o pacote da feature.
