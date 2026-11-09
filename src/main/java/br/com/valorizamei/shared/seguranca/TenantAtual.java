package br.com.valorizamei.shared.seguranca;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Único ponto de acesso ao MEI (tenant) da requisição. O valor vem exclusivamente do token
 * de acesso; nenhum identificador de tenant enviado no corpo, na query ou no caminho é lido.
 * <p>
 * Toda feature grava e consulta seus registros filtrando pelo {@link #id()}.
 */
@Component
public class TenantAtual {

	public UUID id() {
		Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacao instanceof JwtAuthenticationToken token) {
			return UUID.fromString(token.getToken().getClaimAsString(ClaimsDoToken.TENANT_ID));
		}
		throw new IllegalStateException("Não há MEI autenticado na requisição.");
	}

}
