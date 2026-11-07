package br.com.valorizamei;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Os testes de feature usam os nomes e e-mails fixos do cenário de referência; por isso apagam
 * os dados antes de cada teste. A ordem respeita as chaves estrangeiras para {@code mei}. O
 * banco de teste é descartável (Testcontainers).
 */
public final class BancoDeTeste {

	private BancoDeTeste() {
	}

	public static void limpar(JdbcTemplate jdbcTemplate) {
		jdbcTemplate.update("delete from produto");
		jdbcTemplate.update("delete from usuario");
		jdbcTemplate.update("delete from mei");
	}

}
