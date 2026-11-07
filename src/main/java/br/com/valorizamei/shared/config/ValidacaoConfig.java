package br.com.valorizamei.shared.config;

import java.time.Clock;

import org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Faz as validações de data ({@code @PastOrPresent}, {@code @Future} etc.) usarem o mesmo
 * {@link Clock} da aplicação, no fuso do negócio, em vez do relógio do sistema.
 */
@Configuration(proxyBeanMethods = false)
public class ValidacaoConfig {

	@Bean
	ValidationConfigurationCustomizer relogioDaValidacao(Clock relogio) {
		return configuracao -> configuracao.clockProvider(() -> relogio);
	}

}
