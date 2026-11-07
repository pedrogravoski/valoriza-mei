package br.com.valorizamei.precificacao.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.valorizamei.despesa.service.ParametrosFinanceirosService;
import br.com.valorizamei.despesa.service.RateioService;
import br.com.valorizamei.precificacao.dto.PrecificacaoDeProdutoResponse;
import br.com.valorizamei.precificacao.dto.PrecificacaoResponse;
import br.com.valorizamei.precificacao.dto.PrecoSugeridoResponse;
import br.com.valorizamei.precificacao.factory.PrecificacaoFactory;
import br.com.valorizamei.produto.dto.ProdutoResponse;
import br.com.valorizamei.produto.service.ProdutoService;
import br.com.valorizamei.shared.erro.ErroDeNegocioException;

/**
 * Índice de markup (RF03) e preço de venda sugerido (RF04) do MEI do token, pelas fórmulas da
 * seção 2.2 do artigo, em pontos percentuais:
 * <ul>
 * <li>{@code Markup = 100 / [100 - (DV + DF + ML)]}, exibido com 4 casas;</li>
 * <li>{@code PV = Custo Direto × Markup}, calculado em uma única divisão
 * ({@code custo × 100 / (100 - soma)}) e arredondado só ao final, para 2 casas.</li>
 * </ul>
 * O DF vem do rateio sem arredondamento, para que o preço não dependa da exibição.
 */
@Service
public class PrecificacaoService {

	private static final BigDecimal CEM = BigDecimal.valueOf(100);

	private static final int CASAS_DO_MARKUP = 4;

	private static final int CASAS_DO_PRECO = 2;

	private static final Locale PORTUGUES_DO_BRASIL = Locale.forLanguageTag("pt-BR");

	private final ProdutoService produtoService;

	private final RateioService rateioService;

	private final ParametrosFinanceirosService parametrosService;

	private final PrecificacaoFactory precificacaoFactory;

	public PrecificacaoService(ProdutoService produtoService, RateioService rateioService,
			ParametrosFinanceirosService parametrosService, PrecificacaoFactory precificacaoFactory) {
		this.produtoService = produtoService;
		this.rateioService = rateioService;
		this.parametrosService = parametrosService;
		this.precificacaoFactory = precificacaoFactory;
	}

	@Transactional(readOnly = true)
	public PrecificacaoResponse consultar() {
		Percentuais percentuais = percentuaisDoMei();
		List<PrecoSugeridoResponse> produtos = produtoService.listar().stream()
			.map(produto -> precificacaoFactory.paraPrecoSugerido(produto,
					precoSugerido(produto.custoDireto(), percentuais.soma())))
			.toList();
		return precificacaoFactory.paraPrecificacao(percentuais.despesasVariaveis(), percentuais.despesasFixas(),
				percentuais.margemDeLucro(), indiceMarkup(percentuais.soma()), produtos);
	}

	/** O produto é buscado antes dos percentuais: um id fora do MEI gera 404 mesmo sem parâmetros. */
	@Transactional(readOnly = true)
	public PrecificacaoDeProdutoResponse consultarProduto(UUID id) {
		ProdutoResponse produto = produtoService.consultar(id);
		BigDecimal soma = percentuaisDoMei().soma();
		return precificacaoFactory.paraPrecificacaoDeProduto(produto, indiceMarkup(soma),
				precoSugerido(produto.custoDireto(), soma));
	}

	private Percentuais percentuaisDoMei() {
		BigDecimal margemDeLucro = parametrosService.consultar().margemDeLucro();
		Optional<BigDecimal> despesasFixas = rateioService.percentualDespesasFixas();
		if (margemDeLucro == null || despesasFixas.isEmpty()) {
			throw new ErroDeNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "Parâmetros financeiros não informados",
					"Informe a receita mensal estimada e a margem de lucro para calcular o preço de venda.");
		}
		BigDecimal despesasVariaveis = rateioService.percentualDespesasVariaveis();
		BigDecimal soma = somar(despesasVariaveis, despesasFixas.get(), margemDeLucro);
		return new Percentuais(despesasVariaveis, despesasFixas.get(), margemDeLucro, soma);
	}

	/** DV + DF + ML; recusa com 422 a soma de 100 ou mais, para a qual o markup não existe. */
	static BigDecimal somar(BigDecimal despesasVariaveis, BigDecimal despesasFixas, BigDecimal margemDeLucro) {
		BigDecimal soma = despesasVariaveis.add(despesasFixas).add(margemDeLucro);
		if (soma.compareTo(CEM) >= 0) {
			String somaExibida = String.format(PORTUGUES_DO_BRASIL, "%.2f",
					soma.setScale(CASAS_DO_PRECO, RoundingMode.HALF_UP));
			throw new ErroDeNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "Markup inviável",
					"A soma das despesas variáveis, das despesas fixas rateadas e da margem de lucro é de "
							+ somaExibida + "% e precisa ser menor que 100%.");
		}
		return soma;
	}

	/** Markup = 100 / (100 - soma), com 4 casas, para exibição. */
	static BigDecimal indiceMarkup(BigDecimal soma) {
		return CEM.divide(CEM.subtract(soma), CASAS_DO_MARKUP, RoundingMode.HALF_UP);
	}

	/** PV = custo × 100 / (100 - soma): o markup sem arredondamento, com o preço arredondado só no fim. */
	static BigDecimal precoSugerido(BigDecimal custoDireto, BigDecimal soma) {
		return custoDireto.multiply(CEM).divide(CEM.subtract(soma), CASAS_DO_PRECO, RoundingMode.HALF_UP);
	}

	private record Percentuais(BigDecimal despesasVariaveis, BigDecimal despesasFixas, BigDecimal margemDeLucro,
			BigDecimal soma) {
	}

}
