package br.com.valorizamei;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
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

	@Test
	void tabelasDeMeiEUsuarioSaoCriadas() {
		Integer tabelas = jdbcTemplate.queryForObject(
				"select count(*) from information_schema.tables where table_name in ('mei', 'usuario')",
				Integer.class);

		assertThat(tabelas).isEqualTo(2);
	}

	@Test
	void emailDeUsuarioEhUnicoNoBanco() {
		UUID mei = UUID.randomUUID();
		String email = "unico-" + UUID.randomUUID() + "@exemplo.com";
		jdbcTemplate.update("insert into mei (id, nome_negocio, criado_em) values (?, 'Loja de Ferramentas', now())", mei);
		String insereUsuario = "insert into usuario (id, tenant_id, nome, email, senha_hash, criado_em) "
				+ "values (?, ?, 'Carlos Silva', ?, 'hash', now())";
		jdbcTemplate.update(insereUsuario, UUID.randomUUID(), mei, email);

		assertThatThrownBy(() -> jdbcTemplate.update(insereUsuario, UUID.randomUUID(), mei, email))
			.isInstanceOf(DuplicateKeyException.class);
	}

}
