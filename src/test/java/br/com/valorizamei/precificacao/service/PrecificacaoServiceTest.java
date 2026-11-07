package br.com.valorizamei.precificacao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import br.com.valorizamei.shared.erro.ErroDeNegocioException;

/**
 * Fórmulas do markup e do preço de venda sugerido (seção 2.2 do artigo; RF03, RF04).
 */
class PrecificacaoServiceTest {

	@Test
	void markupEPrecoDoEstudoDeCaso() {
		BigDecimal soma = PrecificacaoService.somar(new BigDecimal("5.00"), new BigDecimal("10"), new BigDecimal("25.00"));

		assertThat(soma).isEqualByComparingTo("40");
		assertThat(PrecificacaoService.indiceMarkup(soma)).isEqualTo(new BigDecimal("1.6667"));
		assertThat(PrecificacaoService.precoSugerido(new BigDecimal("180.00"), soma)).isEqualTo(new BigDecimal("300.00"));
	}

	@Test
	void precoNaoUsaOMarkupArredondado() {
		BigDecimal markupExibido = PrecificacaoService.indiceMarkup(new BigDecimal("40"));
		BigDecimal precoComMarkupArredondado = new BigDecimal("180.00").multiply(markupExibido)
			.setScale(2, RoundingMode.HALF_UP);

		assertThat(precoComMarkupArredondado).isEqualTo(new BigDecimal("300.01"));
		assertThat(PrecificacaoService.precoSugerido(new BigDecimal("180.00"), new BigDecimal("40")))
			.isEqualTo(new BigDecimal("300.00"));
	}

	@Test
	void precoUsaODfSemArredondamento() {
		BigDecimal soma = PrecificacaoService.somar(BigDecimal.ZERO, new BigDecimal("33.3333333333"),
				new BigDecimal("25.00"));

		assertThat(PrecificacaoService.indiceMarkup(soma)).isEqualTo(new BigDecimal("2.4000"));
		assertThat(PrecificacaoService.precoSugerido(new BigDecimal("100.00"), soma)).isEqualTo(new BigDecimal("240.00"));

		BigDecimal somaComDfArredondado = new BigDecimal("58.33");
		assertThat(PrecificacaoService.precoSugerido(new BigDecimal("100.00"), somaComDfArredondado))
			.isEqualTo(new BigDecimal("239.98"));
	}

	@Test
	void precosDosDemaisProdutosDoEstudoDeCaso() {
		BigDecimal soma = new BigDecimal("40");

		assertThat(PrecificacaoService.precoSugerido(new BigDecimal("25.00"), soma)).isEqualTo(new BigDecimal("41.67"));
		assertThat(PrecificacaoService.precoSugerido(new BigDecimal("22.00"), soma)).isEqualTo(new BigDecimal("36.67"));
		assertThat(PrecificacaoService.precoSugerido(new BigDecimal("12.00"), soma)).isEqualTo(new BigDecimal("20.00"));
		assertThat(PrecificacaoService.precoSugerido(new BigDecimal("28.00"), soma)).isEqualTo(new BigDecimal("46.67"));
		assertThat(PrecificacaoService.precoSugerido(new BigDecimal("45.00"), soma)).isEqualTo(new BigDecimal("75.00"));
	}

	@Test
	void somaDeCemEhRecusada() {
		assertThatThrownBy(() -> PrecificacaoService.somar(new BigDecimal("5.00"), new BigDecimal("10"),
				new BigDecimal("85.00")))
			.isInstanceOfSatisfying(ErroDeNegocioException.class, erro -> {
				assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
				assertThat(erro.getBody().getTitle()).isEqualTo("Markup inviável");
				assertThat(erro.getBody().getDetail()).contains("100,00%");
			});
	}

	@Test
	void somaAcimaDeCemEhRecusada() {
		assertThatThrownBy(() -> PrecificacaoService.somar(new BigDecimal("5.00"), new BigDecimal("10"),
				new BigDecimal("90.00")))
			.isInstanceOfSatisfying(ErroDeNegocioException.class,
					erro -> assertThat(erro.getBody().getDetail()).contains("105,00%"));
	}

	@Test
	void somaLogoAbaixoDeCemEhAceita() {
		BigDecimal soma = PrecificacaoService.somar(new BigDecimal("5.00"), new BigDecimal("10"),
				new BigDecimal("84.99"));

		assertThat(PrecificacaoService.indiceMarkup(soma)).isEqualTo(new BigDecimal("10000.0000"));
	}

}
