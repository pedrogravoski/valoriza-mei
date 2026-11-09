package br.com.valorizamei;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import br.com.valorizamei.shared.seguranca.ClaimsDoToken;

/**
 * Autentica a thread do teste como um MEI, para chamar services diretamente (sem MockMvc) com o
 * mesmo {@code TenantAtual} de uma requisição. Chame {@link #limpar()} ao fim do teste.
 */
public final class ApoioDeTenant {

	private ApoioDeTenant() {
	}

	public static void autenticarComo(JdbcTemplate jdbcTemplate, String email) {
		UUID tenantId = jdbcTemplate.queryForObject("select tenant_id from usuario where email = ?", UUID.class, email);
		Jwt token = Jwt.withTokenValue("teste")
			.header("alg", "HS256")
			.subject(UUID.randomUUID().toString())
			.claim(ClaimsDoToken.TENANT_ID, tenantId.toString())
			.build();
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(token));
	}

	public static void limpar() {
		SecurityContextHolder.clearContext();
	}

}
