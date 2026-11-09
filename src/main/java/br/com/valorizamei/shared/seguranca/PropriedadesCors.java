package br.com.valorizamei.shared.seguranca;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Origens do frontend autorizadas a chamar a API pelo navegador (CORS), exatas, como
 * {@code http://localhost:4200}. Lista vazia: nenhuma origem externa é aceita.
 *
 * @param origensPermitidas propriedade {@code valorizamei.cors.origens-permitidas}
 */
@ConfigurationProperties("valorizamei.cors")
public record PropriedadesCors(List<String> origensPermitidas) {

	public PropriedadesCors {
		origensPermitidas = origensPermitidas == null ? List.of() : List.copyOf(origensPermitidas);
	}

}
