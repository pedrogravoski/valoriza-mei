package br.com.valorizamei.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * Identificação da API na especificação OpenAPI publicada em {@code /v3/api-docs}.
 * <p>
 * O formato (OpenAPI 3.1) e o filtro de caminhos ({@code /api/v1/**}) ficam no
 * {@code application.yaml}.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

	@Bean
	OpenAPI valorizaMeiOpenApi() {
		return new OpenAPI()
			.info(new Info()
				.title("Valoriza MEI API")
				.version("v1")
				.description("API de gestão para o Microempreendedor Individual: precificação por markup, "
						+ "controle de estoque, fluxo de caixa, alertas de ruptura e sugestões sazonais."));
	}

}
