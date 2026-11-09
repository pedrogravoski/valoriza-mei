package br.com.valorizamei.alerta.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.valorizamei.alerta.dto.AlertasResponse;
import br.com.valorizamei.alerta.dto.RupturaResponse;
import br.com.valorizamei.alerta.dto.SugestaoResponse;
import br.com.valorizamei.alerta.dto.TipoSugestao;
import br.com.valorizamei.alerta.service.AlertaService.GiroDoProduto;
import br.com.valorizamei.calendario.service.CalendarioService;
import br.com.valorizamei.estoque.dto.EstoqueProdutoResponse;
import br.com.valorizamei.estoque.service.EstoqueService;
import br.com.valorizamei.precificacao.service.PrecificacaoService;

/**
 * Regras de ruptura e de sugestão dos alertas (spec alertas; RF08, RF09).
 */
class AlertaServiceTest {

	private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

	@Test
	void rupturaComEstoqueMenorIgualOuMaiorQueOMinimo() {
		List<RupturaResponse> ruptura = AlertaService.ruptura(List.of(
				estoque("Furadeira de impacto 650 W", 5, 3),
				estoque("Martelo unha 27 mm", 4, 4),
				estoque("Trena 5 m", 4, 5)));

		assertThat(ruptura).extracting(RupturaResponse::nome).containsExactly("Martelo unha 27 mm", "Trena 5 m");
	}

	@Test
	void estoqueMinimoZeroNaoGeraRuptura() {
		assertThat(AlertaService.ruptura(List.of(estoque("Brinde", 0, 0)))).isEmpty();
	}

	@Test
	void produtoSemEstoqueComMinimoEstaEmRuptura() {
		assertThat(AlertaService.ruptura(List.of(estoque("Furadeira de impacto 650 W", 0, 3))))
			.extracting(RupturaResponse::estoqueAtual).containsExactly(0);
	}

	@Test
	void sugestoesDoEstudoDeCaso() {
		List<SugestaoResponse> sugestoes = AlertaService.sugestoes(List.of(
				giro("Alicate universal 8\"", 12, 4, "186.68", "46.67"),
				giro("Caixa de ferramentas plástica", 10, 0, "0", "75.00"),
				giro("Furadeira de impacto 650 W", 5, 6, "1800.00", "300.00"),
				giro("Jogo de chaves de fenda (6 peças)", 20, 9, "375.03", "41.67"),
				giro("Martelo unha 27 mm", 15, 5, "183.35", "36.67"),
				giro("Trena 5 m", 4, 10, "200.00", "20.00")));

		assertThat(sugestoes).extracting(SugestaoResponse::nome, SugestaoResponse::tipo).containsExactly(
				tuple("Caixa de ferramentas plástica", TipoSugestao.PROMOCAO),
				tuple("Furadeira de impacto 650 W", TipoSugestao.REFORCAR_ESTOQUE),
				tuple("Trena 5 m", TipoSugestao.REFORCAR_ESTOQUE));
	}

	@Test
	void motivosDasSugestoes() {
		List<SugestaoResponse> sugestoes = AlertaService.sugestoes(List.of(
				giro("Caixa de ferramentas plástica", 10, 0, "0", null),
				giro("Furadeira de impacto 650 W", 5, 6, "1680.00", "300.00")));

		assertThat(sugestoes).extracting(SugestaoResponse::motivo).containsExactly(
				"Nenhuma venda nos últimos 30 dias, com 10 unidades em estoque.",
				"Vendeu 6 unidades nos últimos 30 dias e tem 5 em estoque.",
				"Preço médio de venda de R$ 280,00 nos últimos 30 dias, abaixo do preço sugerido de R$ 300,00.");
	}

	@Test
	void produtoComReforcoEReajuste() {
		List<SugestaoResponse> sugestoes = AlertaService.sugestoes(List.of(
				giro("Furadeira de impacto 650 W", 5, 6, "1680.00", "300.00")));

		assertThat(sugestoes).extracting(SugestaoResponse::tipo)
			.containsExactly(TipoSugestao.REFORCAR_ESTOQUE, TipoSugestao.REAJUSTE_PRECO);
	}

	@Test
	void precoMedioIgualAoSugeridoNaoGeraReajuste() {
		assertThat(AlertaService.sugestoes(List.of(giro("Martelo unha 27 mm", 15, 5, "183.35", "36.67")))).isEmpty();
	}

	@Test
	void semPrecoSugeridoNaoHaReajuste() {
		assertThat(AlertaService.sugestoes(List.of(giro("Martelo unha 27 mm", 15, 5, "100.00", null)))).isEmpty();
	}

	@Test
	void produtoSemEstoqueESemVendasNaoRecebeSugestao() {
		assertThat(AlertaService.sugestoes(List.of(giro("Brinde", 0, 0, "0", null)))).isEmpty();
	}

	@Test
	void valoresAcimaDeMilReaisComSeparadorDeMilhar() {
		List<SugestaoResponse> sugestoes = AlertaService.sugestoes(List.of(
				giro("Serra circular", 10, 2, "2400.00", "1300.00")));

		assertThat(sugestoes).extracting(SugestaoResponse::motivo).containsExactly(
				"Preço médio de venda de R$ 1.200,00 nos últimos 30 dias, abaixo do preço sugerido de R$ 1.300,00.");
	}

	@Test
	void semDataComemorativaProximaNaoHaSugestoes() {
		EstoqueService estoqueService = mock(EstoqueService.class);
		CalendarioService calendarioService = mock(CalendarioService.class);
		PrecificacaoService precificacaoService = mock(PrecificacaoService.class);
		when(estoqueService.estoqueAtual()).thenReturn(List.of(
				estoque("Caixa de ferramentas plástica", 10, 3),
				estoque("Trena 5 m", 4, 5)));
		when(calendarioService.proximasDatas()).thenReturn(List.of());
		Clock quinzeDeJunho = Clock.fixed(LocalDate.of(2026, 6, 15).atStartOfDay(SAO_PAULO).toInstant(), SAO_PAULO);
		AlertaService alertas = new AlertaService(estoqueService, calendarioService, precificacaoService,
				quinzeDeJunho);

		AlertasResponse resposta = alertas.consultar();

		assertThat(resposta.dataReferencia()).isEqualTo(LocalDate.of(2026, 6, 15));
		assertThat(resposta.datasComemorativas()).isEmpty();
		assertThat(resposta.sugestoes()).isEmpty();
		assertThat(resposta.ruptura()).extracting(RupturaResponse::nome).containsExactly("Trena 5 m");
		verify(estoqueService, never()).vendasPorProduto(any(), any());
		verify(precificacaoService, never()).consultarSeDisponivel();
	}

	private static EstoqueProdutoResponse estoque(String nome, int atual, int minimo) {
		return new EstoqueProdutoResponse(UUID.randomUUID(), nome, atual, minimo);
	}

	private static GiroDoProduto giro(String nome, int estoqueAtual, long vendidas, String valorVendido,
			String precoSugerido) {
		return new GiroDoProduto(UUID.randomUUID(), nome, estoqueAtual, vendidas, new BigDecimal(valorVendido),
				precoSugerido == null ? null : new BigDecimal(precoSugerido));
	}

}
