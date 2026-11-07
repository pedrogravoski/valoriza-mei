package br.com.valorizamei.shared.seguranca;

import java.util.List;
import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Converte um token já validado (assinatura, emissor e validade) em autenticação, exigindo
 * que ele identifique um MEI existente. Um token sem MEI válido resulta em 401.
 */
class ConversorDeTokenAutenticado implements Converter<Jwt, AbstractAuthenticationToken> {

	private final VerificadorDeTenant verificadorDeTenant;

	ConversorDeTokenAutenticado(VerificadorDeTenant verificadorDeTenant) {
		this.verificadorDeTenant = verificadorDeTenant;
	}

	@Override
	public AbstractAuthenticationToken convert(Jwt token) {
		UUID tenantId = tenantDo(token);
		if (!verificadorDeTenant.existe(tenantId)) {
			throw new InvalidBearerTokenException("O MEI do token não existe.");
		}
		return new JwtAuthenticationToken(token, List.of(), token.getSubject());
	}

	private static UUID tenantDo(Jwt token) {
		String tenant = token.getClaimAsString(ClaimsDoToken.TENANT_ID);
		if (tenant == null) {
			throw new InvalidBearerTokenException("O token não identifica o MEI.");
		}
		try {
			return UUID.fromString(tenant);
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidBearerTokenException("O token não identifica o MEI.");
		}
	}

}
