package br.com.valorizamei.shared.seguranca;

/**
 * Nomes e valores fixos do token de acesso.
 */
public final class ClaimsDoToken {

	/** Emissor ({@code iss}) de todo token aceito pela API. */
	public static final String EMISSOR = "valoriza-mei";

	/** Claim com o identificador do MEI (tenant) dono da requisição. */
	public static final String TENANT_ID = "tenant_id";

	private ClaimsDoToken() {
	}

}
