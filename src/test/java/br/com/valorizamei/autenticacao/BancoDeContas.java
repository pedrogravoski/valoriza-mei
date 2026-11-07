package br.com.valorizamei.autenticacao;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Os testes desta feature usam e-mails fixos do cenário de referência; por isso apagam as
 * contas antes de cada teste. O banco de teste é descartável (Testcontainers).
 */
final class BancoDeContas {

	private BancoDeContas() {
	}

	static void limpar(JdbcTemplate jdbcTemplate) {
		jdbcTemplate.update("delete from usuario");
		jdbcTemplate.update("delete from mei");
	}

}
