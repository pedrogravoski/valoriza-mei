package br.com.valorizamei.shared.seguranca;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * Emite o token de acesso de um usuário autenticado, com o MEI (tenant) dele no claim
 * {@value ClaimsDoToken#TENANT_ID}.
 */
@Component
public class EmissorDeToken {

	private final JwtEncoder codificador;

	private final PropriedadesJwt propriedades;

	private final Clock relogio;

	public EmissorDeToken(JwtEncoder codificador, PropriedadesJwt propriedades, Clock relogio) {
		this.codificador = codificador;
		this.propriedades = propriedades;
		this.relogio = relogio;
	}

	public TokenEmitido emitir(UUID usuarioId, UUID tenantId) {
		Instant agora = relogio.instant();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(ClaimsDoToken.EMISSOR)
			.subject(usuarioId.toString())
			.claim(ClaimsDoToken.TENANT_ID, tenantId.toString())
			.issuedAt(agora)
			.expiresAt(agora.plus(propriedades.validade()))
			.build();
		JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
		String token = codificador.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
		return new TokenEmitido(token, propriedades.validade().toSeconds());
	}

}
