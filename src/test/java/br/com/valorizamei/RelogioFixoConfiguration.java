package br.com.valorizamei;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Fixa a data atual da aplicação em 15/07/2026, ao meio-dia de São Paulo, para os testes que
 * dependem dela (data padrão das movimentações, mês atual do fluxo de caixa e, depois, os
 * alertas do Dia dos Pais). O token emitido e validado nos testes usa o mesmo relógio.
 */
@TestConfiguration(proxyBeanMethods = false)
public class RelogioFixoConfiguration {

	public static final LocalDate HOJE = LocalDate.of(2026, 7, 15);

	private static final ZoneId FUSO_DO_NEGOCIO = ZoneId.of("America/Sao_Paulo");

	@Bean
	@Primary
	Clock relogioFixo() {
		return Clock.fixed(HOJE.atTime(LocalTime.NOON).atZone(FUSO_DO_NEGOCIO).toInstant(), FUSO_DO_NEGOCIO);
	}

}
