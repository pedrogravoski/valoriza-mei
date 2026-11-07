package br.com.valorizamei.shared.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

class EmissorDeTokenTest {

	private static final PropriedadesJwt PROPRIEDADES = new PropriedadesJwt(
			"segredo-de-teste-com-mais-de-32-bytes-de-tamanho", Duration.ofMinutes(60));

	private static final Instant EMISSAO = Instant.parse("2026-07-10T12:00:00Z");

	private final SegurancaConfig config = new SegurancaConfig();

	@Test
	void tokenEmitidoTemEmissorUsuarioTenantEValidade() {
		UUID usuario = UUID.randomUUID();
		UUID tenant = UUID.randomUUID();

		TokenEmitido emitido = emissorEm(EMISSAO).emitir(usuario, tenant);
		Jwt token = decodificadorEm(EMISSAO.plusSeconds(1)).decode(emitido.valor());

		assertThat(token.getClaimAsString("iss")).isEqualTo(ClaimsDoToken.EMISSOR);
		assertThat(token.getSubject()).isEqualTo(usuario.toString());
		assertThat(token.getClaimAsString(ClaimsDoToken.TENANT_ID)).isEqualTo(tenant.toString());
		assertThat(token.getIssuedAt()).isEqualTo(EMISSAO);
		assertThat(token.getExpiresAt()).isEqualTo(EMISSAO.plus(Duration.ofMinutes(60)));
		assertThat(emitido.validadeEmSegundos()).isEqualTo(3600);
	}

	@Test
	void tokenEhRecusadoDepoisDaValidade() {
		TokenEmitido emitido = emissorEm(EMISSAO).emitir(UUID.randomUUID(), UUID.randomUUID());
		JwtDecoder decodificadorDepoisDaValidade = decodificadorEm(EMISSAO.plus(Duration.ofMinutes(61)));

		assertThatThrownBy(() -> decodificadorDepoisDaValidade.decode(emitido.valor()))
			.isInstanceOf(JwtValidationException.class);
	}

	@Test
	void segredoCurtoEhRejeitado() {
		assertThatThrownBy(() -> new PropriedadesJwt("curto", Duration.ofMinutes(60)))
			.isInstanceOf(IllegalArgumentException.class);
	}

	private EmissorDeToken emissorEm(Instant momento) {
		return new EmissorDeToken(config.codificadorDeToken(PROPRIEDADES), PROPRIEDADES, relogio(momento));
	}

	private JwtDecoder decodificadorEm(Instant momento) {
		return config.decodificadorDeToken(PROPRIEDADES, relogio(momento));
	}

	private static Clock relogio(Instant momento) {
		return Clock.fixed(momento, ZoneOffset.UTC);
	}

}
