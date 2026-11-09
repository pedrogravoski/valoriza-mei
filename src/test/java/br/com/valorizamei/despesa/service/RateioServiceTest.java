package br.com.valorizamei.despesa.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/**
 * Cálculo do DF sem o arredondamento de exibição, usado pela precificação (RF05).
 */
class RateioServiceTest {

	@Test
	void dfDoEstudoDeCaso() {
		BigDecimal df = RateioService.percentualSobreReceita(new BigDecimal("1400.00"), new BigDecimal("14000.00"));

		assertThat(df).isEqualByComparingTo("10");
	}

	@Test
	void dfSemArredondamentoParaDuasCasas() {
		BigDecimal df = RateioService.percentualSobreReceita(new BigDecimal("1000.00"), new BigDecimal("3000.00"));

		assertThat(df).isEqualTo(new BigDecimal("33.3333333333"));
		assertThat(df.scale()).isEqualTo(RateioService.ESCALA_DO_CALCULO);
	}

}
