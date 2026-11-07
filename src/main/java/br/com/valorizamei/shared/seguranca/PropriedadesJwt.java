package br.com.valorizamei.shared.seguranca;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do token de acesso ({@code valorizamei.seguranca.jwt.*}).
 *
 * @param segredo chave simétrica usada para assinar e validar o token (HS256), com no mínimo 32 bytes
 * @param validade tempo entre a emissão e a expiração do token
 */
@ConfigurationProperties(prefix = "valorizamei.seguranca.jwt")
public record PropriedadesJwt(String segredo, Duration validade) {

	static final int TAMANHO_MINIMO_DO_SEGREDO = 32;

	public PropriedadesJwt {
		if (segredo == null || segredo.getBytes(StandardCharsets.UTF_8).length < TAMANHO_MINIMO_DO_SEGREDO) {
			throw new IllegalArgumentException(
					"valorizamei.seguranca.jwt.segredo deve ter no mínimo " + TAMANHO_MINIMO_DO_SEGREDO + " bytes");
		}
		if (validade == null || validade.isNegative() || validade.isZero()) {
			throw new IllegalArgumentException("valorizamei.seguranca.jwt.validade deve ser maior que zero");
		}
	}

}
