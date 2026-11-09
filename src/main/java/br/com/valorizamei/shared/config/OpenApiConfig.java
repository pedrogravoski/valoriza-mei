package br.com.valorizamei.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Identificação da API na especificação OpenAPI publicada em {@code /v3/api-docs}.
 * <p>
 * O formato (OpenAPI 3.1) e o filtro de caminhos ({@code /api/v1/**}) ficam no
 * {@code application.yaml}. Todos os endpoints exigem o token Bearer, exceto os marcados
 * com {@code @SecurityRequirements} vazio (cadastro e login); o botão "Authorize" do Swagger UI
 * recebe esse token.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

	static final String ESQUEMA_DE_AUTENTICACAO = "bearerAuth";

	@Bean
	OpenAPI valorizaMeiOpenApi() {
		return new OpenAPI()
			.info(new Info()
				.title("Valoriza MEI API")
				.version("v1")
				.description("API de gestão para o Microempreendedor Individual: precificação por markup, "
						+ "controle de estoque, fluxo de caixa, alertas de ruptura e sugestões sazonais."))
			.components(new Components()
				.addSecuritySchemes(ESQUEMA_DE_AUTENTICACAO, new SecurityScheme()
					.type(SecurityScheme.Type.HTTP)
					.scheme("bearer")
					.bearerFormat("JWT")
					.description("Token obtido em POST /api/v1/auth/login.")))
			.addSecurityItem(new SecurityRequirement().addList(ESQUEMA_DE_AUTENTICACAO));
	}

}
