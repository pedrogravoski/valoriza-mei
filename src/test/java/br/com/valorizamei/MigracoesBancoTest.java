package br.com.valorizamei;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
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

	@Test
	void tabelaDeProdutoEhCriada() {
		Integer tabelas = jdbcTemplate.queryForObject(
				"select count(*) from information_schema.tables where table_name = 'produto'", Integer.class);

		assertThat(tabelas).isEqualTo(1);
	}

	@Test
	void bancoRecusaProdutoComCustoDiretoZero() {
		UUID mei = inserirMei();

		assertThatThrownBy(() -> inserirProduto(mei, "Trena 5 m", "0.00", 5))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void bancoRecusaProdutoComEstoqueMinimoNegativo() {
		UUID mei = inserirMei();

		assertThatThrownBy(() -> inserirProduto(mei, "Trena 5 m", "12.00", -1))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void nomeDeProdutoEhUnicoNoMeiSemDiferenciarMaiusculas() {
		UUID mei = inserirMei();
		inserirProduto(mei, "Trena 5 m", "12.00", 5);

		assertThatThrownBy(() -> inserirProduto(mei, "TRENA 5 M", "12.00", 5))
			.isInstanceOf(DuplicateKeyException.class);
	}

	@Test
	void mesmoNomeDeProdutoEmMeisDiferentes() {
		inserirProduto(inserirMei(), "Trena 5 m", "12.00", 5);

		assertThat(inserirProduto(inserirMei(), "Trena 5 m", "12.00", 5)).isEqualTo(1);
	}

	private UUID inserirMei() {
		UUID mei = UUID.randomUUID();
		jdbcTemplate.update("insert into mei (id, nome_negocio, criado_em) values (?, 'Loja de Ferramentas', now())", mei);
		return mei;
	}

	private int inserirProduto(UUID mei, String nome, String custoDireto, int estoqueMinimo) {
		return jdbcTemplate.update("insert into produto (id, tenant_id, nome, custo_direto, estoque_minimo, criado_em, "
				+ "atualizado_em) values (?, ?, ?, ?, ?, now(), now())", UUID.randomUUID(), mei, nome,
				new BigDecimal(custoDireto), estoqueMinimo);
	}

}
