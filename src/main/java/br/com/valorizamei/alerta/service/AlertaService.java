package br.com.valorizamei.alerta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.valorizamei.alerta.dto.AlertasResponse;
import br.com.valorizamei.alerta.dto.DataProximaResponse;
import br.com.valorizamei.alerta.dto.RupturaResponse;
import br.com.valorizamei.alerta.dto.SugestaoResponse;
import br.com.valorizamei.alerta.dto.TipoSugestao;
import br.com.valorizamei.calendario.dto.DataComemorativaResponse;
import br.com.valorizamei.calendario.service.CalendarioService;
import br.com.valorizamei.estoque.dto.EstoqueProdutoResponse;
import br.com.valorizamei.estoque.dto.VendasDoProdutoResponse;
import br.com.valorizamei.estoque.service.EstoqueService;
import br.com.valorizamei.precificacao.dto.PrecoSugeridoResponse;
import br.com.valorizamei.precificacao.service.PrecificacaoService;

/**
 * Alertas do MEI do token, calculados a cada consulta (RF08, RF09, RNF07):
 * <ul>
 * <li>ruptura: estoque mínimo maior que zero e estoque atual menor ou igual ao mínimo;</li>
 * <li>datas comemorativas dos próximos 30 dias;</li>
 * <li>sugestões pelo giro dos últimos {@value #DIAS_DO_GIRO} dias, só quando há data
 * próxima: reforçar estoque, promoção e reajuste de preço (este, só com a precificação
 * disponível).</li>
 * </ul>
 * As regras ficam em funções estáticas, testáveis com qualquer data e sem banco.
 */
@Service
public class AlertaService {

	static final int DIAS_DO_GIRO = 30;

	private static final Locale PORTUGUES_DO_BRASIL = Locale.forLanguageTag("pt-BR");

	private final EstoqueService estoqueService;

	private final CalendarioService calendarioService;

	private final PrecificacaoService precificacaoService;

	private final Clock relogio;

	public AlertaService(EstoqueService estoqueService, CalendarioService calendarioService,
			PrecificacaoService precificacaoService, Clock relogio) {
		this.estoqueService = estoqueService;
		this.calendarioService = calendarioService;
		this.precificacaoService = precificacaoService;
		this.relogio = relogio;
	}

	@Transactional(readOnly = true)
	public AlertasResponse consultar() {
		LocalDate hoje = LocalDate.now(relogio);
		List<EstoqueProdutoResponse> estoque = estoqueService.estoqueAtual();
		List<DataComemorativaResponse> proximas = calendarioService.proximasDatas();
		List<SugestaoResponse> sugestoes = proximas.isEmpty() ? List.of() : sugestoes(giro(estoque, hoje));
		return new AlertasResponse(hoje, ruptura(estoque), paraDatasProximas(proximas), sugestoes);
	}

	/** Produtos em ruptura, na ordem recebida (alfabética). */
	static List<RupturaResponse> ruptura(List<EstoqueProdutoResponse> estoque) {
		return estoque.stream()
			.filter(produto -> produto.estoqueMinimo() > 0 && produto.estoqueAtual() <= produto.estoqueMinimo())
			.map(produto -> new RupturaResponse(produto.produtoId(), produto.nome(), produto.estoqueAtual(),
					produto.estoqueMinimo()))
			.toList();
	}

	/** Sugestões de cada produto, na ordem recebida e, no produto, na ordem de {@link TipoSugestao}. */
	static List<SugestaoResponse> sugestoes(List<GiroDoProduto> produtos) {
		List<SugestaoResponse> sugestoes = new ArrayList<>();
		for (GiroDoProduto produto : produtos) {
			if (produto.vendidas() > produto.estoqueAtual()) {
				sugestoes.add(sugestao(produto, TipoSugestao.REFORCAR_ESTOQUE, "Vendeu " + unidades(produto.vendidas())
						+ " nos últimos 30 dias e tem " + produto.estoqueAtual() + " em estoque."));
			}
			if (produto.vendidas() == 0 && produto.estoqueAtual() > 0) {
				sugestoes.add(sugestao(produto, TipoSugestao.PROMOCAO,
						"Nenhuma venda nos últimos 30 dias, com " + unidades(produto.estoqueAtual()) + " em estoque."));
			}
			BigDecimal precoMedio = produto.precoMedio();
			if (precoMedio != null && produto.precoSugerido() != null
					&& precoMedio.compareTo(produto.precoSugerido()) < 0) {
				sugestoes.add(sugestao(produto, TipoSugestao.REAJUSTE_PRECO, "Preço médio de venda de "
						+ reais(precoMedio) + " nos últimos 30 dias, abaixo do preço sugerido de "
						+ reais(produto.precoSugerido()) + "."));
			}
		}
		return sugestoes;
	}

	/** Estoque, vendas dos últimos 30 dias (hoje inclusive) e preço sugerido, quando disponível. */
	private List<GiroDoProduto> giro(List<EstoqueProdutoResponse> estoque, LocalDate hoje) {
		Map<UUID, VendasDoProdutoResponse> vendas = estoqueService
			.vendasPorProduto(hoje.minusDays(DIAS_DO_GIRO - 1L), hoje).stream()
			.collect(Collectors.toMap(VendasDoProdutoResponse::produtoId, venda -> venda));
		Map<UUID, BigDecimal> precos = precificacaoService.consultarSeDisponivel()
			.map(precificacao -> precificacao.produtos().stream()
				.collect(Collectors.toMap(PrecoSugeridoResponse::id, PrecoSugeridoResponse::precoSugerido)))
			.orElse(Map.of());
		return estoque.stream().map(produto -> {
			VendasDoProdutoResponse venda = vendas.get(produto.produtoId());
			return new GiroDoProduto(produto.produtoId(), produto.nome(), produto.estoqueAtual(),
					venda == null ? 0 : venda.quantidade(), venda == null ? BigDecimal.ZERO : venda.valorTotal(),
					precos.get(produto.produtoId()));
		}).toList();
	}

	private static List<DataProximaResponse> paraDatasProximas(List<DataComemorativaResponse> datas) {
		return datas.stream()
			.map(data -> new DataProximaResponse(data.nome(), data.proximaOcorrencia(), data.diasRestantes(),
					data.personalizada()))
			.toList();
	}

	private static SugestaoResponse sugestao(GiroDoProduto produto, TipoSugestao tipo, String motivo) {
		return new SugestaoResponse(produto.produtoId(), produto.nome(), tipo, motivo);
	}

	private static String unidades(long quantidade) {
		return quantidade + (quantidade == 1 ? " unidade" : " unidades");
	}

	private static String reais(BigDecimal valor) {
		return "R$ " + String.format(PORTUGUES_DO_BRASIL, "%,.2f", valor);
	}

	/**
	 * Dados de um produto para as sugestões: estoque atual, unidades e valor vendidos nos últimos
	 * 30 dias e preço sugerido (nulo sem precificação disponível).
	 */
	record GiroDoProduto(UUID produtoId, String nome, int estoqueAtual, long vendidas, BigDecimal valorVendido,
			BigDecimal precoSugerido) {

		/** Valor vendido dividido pelas unidades, com 2 casas; nulo sem vendas. */
		BigDecimal precoMedio() {
			return vendidas == 0 ? null
					: valorVendido.divide(BigDecimal.valueOf(vendidas), 2, RoundingMode.HALF_UP);
		}

	}

}
