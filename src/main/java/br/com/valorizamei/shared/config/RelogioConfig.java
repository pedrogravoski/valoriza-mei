package br.com.valorizamei.shared.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Relógio único da aplicação. Toda regra que depende da data ou da hora atual recebe este
 * {@link Clock} por injeção, para que os testes possam fixar o momento.
 */
@Configuration(proxyBeanMethods = false)
public class RelogioConfig {

	static final ZoneId FUSO_DO_NEGOCIO = ZoneId.of("America/Sao_Paulo");

	@Bean
	Clock relogio() {
		return Clock.system(FUSO_DO_NEGOCIO);
	}

}
