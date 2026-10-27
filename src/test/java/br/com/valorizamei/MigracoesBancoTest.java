package br.com.valorizamei;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MigracoesBancoTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void liquibaseExecutaNoPostgresDoConteiner() {
		Integer tabelasDeControle = jdbcTemplate.queryForObject(
				"select count(*) from information_schema.tables where table_name = 'databasechangelog'",
				Integer.class);

		assertThat(tabelasDeControle).isEqualTo(1);
	}

	@Test
	void bancoDeTesteEhPostgres15() {
		String versao = jdbcTemplate.queryForObject("show server_version", String.class);

		assertThat(versao).startsWith("15.");
	}

}
