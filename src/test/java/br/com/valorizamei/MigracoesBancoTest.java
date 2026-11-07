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

	@Test
	void tabelasDeDespesaEParametrosSaoCriadas() {
		Integer tabelas = jdbcTemplate.queryForObject("select count(*) from information_schema.tables "
				+ "where table_name in ('despesa', 'parametros_financeiros')", Integer.class);

		assertThat(tabelas).isEqualTo(2);
	}

	@Test
	void bancoRecusaDespesaFixaComPercentual() {
		UUID mei = inserirMei();

		assertThatThrownBy(() -> inserirDespesa(mei, "Aluguel", "FIXA", "1200.00", "3.00"))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void bancoRecusaDespesaVariavelSemPercentual() {
		UUID mei = inserirMei();

		assertThatThrownBy(() -> inserirDespesa(mei, "Taxa de cartão", "VARIAVEL", null, null))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void bancoRecusaPercentualDeCem() {
		UUID mei = inserirMei();

		assertThatThrownBy(() -> inserirDespesa(mei, "Taxa de cartão", "VARIAVEL", null, "100.00"))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void bancoRecusaTipoDeDespesaDesconhecido() {
		UUID mei = inserirMei();

		assertThatThrownBy(() -> inserirDespesa(mei, "Aluguel", "OUTRA", "1200.00", null))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void descricaoDeDespesaEhUnicaNoMeiSemDiferenciarMaiusculas() {
		UUID mei = inserirMei();
		inserirDespesa(mei, "Aluguel", "FIXA", "1200.00", null);

		assertThatThrownBy(() -> inserirDespesa(mei, "ALUGUEL", "FIXA", "1200.00", null))
			.isInstanceOf(DuplicateKeyException.class);
	}

	@Test
	void mesmaDescricaoDeDespesaEmMeisDiferentes() {
		inserirDespesa(inserirMei(), "Aluguel", "FIXA", "1200.00", null);

		assertThat(inserirDespesa(inserirMei(), "Aluguel", "FIXA", "900.00", null)).isEqualTo(1);
	}

	@Test
	void bancoRecusaReceitaMensalZero() {
		UUID mei = inserirMei();

		assertThatThrownBy(() -> jdbcTemplate.update("insert into parametros_financeiros (tenant_id, "
				+ "receita_mensal_estimada, margem_lucro, atualizado_em) values (?, 0, 25, now())", mei))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void tabelasDeEstoqueECaixaSaoCriadas() {
		Integer tabelas = jdbcTemplate.queryForObject("select count(*) from information_schema.tables "
				+ "where table_name in ('movimentacao_estoque', 'saldo_estoque', 'lancamento_caixa')", Integer.class);

		assertThat(tabelas).isEqualTo(3);
	}

	@Test
	void bancoRecusaMovimentacaoComQuantidadeZero() {
		UUID mei = inserirMei();
		UUID produto = inserirProdutoComId(mei);

		assertThatThrownBy(() -> inserirMovimentacao(mei, produto, "ENTRADA", 0))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void bancoRecusaTipoDeMovimentacaoDesconhecido() {
		UUID mei = inserirMei();
		UUID produto = inserirProdutoComId(mei);

		assertThatThrownBy(() -> inserirMovimentacao(mei, produto, "AJUSTE", 1))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void bancoRecusaSaldoNegativo() {
		UUID mei = inserirMei();
		UUID produto = inserirProdutoComId(mei);

		assertThatThrownBy(() -> jdbcTemplate.update(
				"insert into saldo_estoque (produto_id, tenant_id, quantidade) values (?, ?, -1)", produto, mei))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void movimentacaoTemNoMaximoUmLancamentoDeCaixa() {
		UUID mei = inserirMei();
		UUID movimentacao = inserirMovimentacao(mei, inserirProdutoComId(mei), "ENTRADA", 1);
		String insereLancamento = "insert into lancamento_caixa (id, tenant_id, tipo, descricao, valor, data, "
				+ "movimentacao_estoque_id, criado_em) values (?, ?, 'SAIDA', 'Compra: 1 × Trena 5 m', 12.00, "
				+ "current_date, ?, now())";
		jdbcTemplate.update(insereLancamento, UUID.randomUUID(), mei, movimentacao);

		assertThatThrownBy(() -> jdbcTemplate.update(insereLancamento, UUID.randomUUID(), mei, movimentacao))
			.isInstanceOf(DuplicateKeyException.class);
	}

	private UUID inserirProdutoComId(UUID mei) {
		UUID produto = UUID.randomUUID();
		jdbcTemplate.update("insert into produto (id, tenant_id, nome, custo_direto, estoque_minimo, criado_em, "
				+ "atualizado_em) values (?, ?, 'Trena 5 m', 12.00, 5, now(), now())", produto, mei);
		return produto;
	}

	private UUID inserirMovimentacao(UUID mei, UUID produto, String tipo, int quantidade) {
		UUID movimentacao = UUID.randomUUID();
		jdbcTemplate.update("insert into movimentacao_estoque (id, tenant_id, produto_id, tipo, quantidade, "
				+ "valor_unitario, valor_total, data, criado_em) values (?, ?, ?, ?, ?, 12.00, 12.00, current_date, now())",
				movimentacao, mei, produto, tipo, quantidade);
		return movimentacao;
	}

	private int inserirDespesa(UUID mei, String descricao, String tipo, String valorMensal, String percentual) {
		return jdbcTemplate.update("insert into despesa (id, tenant_id, descricao, tipo, valor_mensal, percentual, "
				+ "criado_em, atualizado_em) values (?, ?, ?, ?, ?, ?, now(), now())", UUID.randomUUID(), mei,
				descricao, tipo, valorMensal == null ? null : new BigDecimal(valorMensal),
				percentual == null ? null : new BigDecimal(percentual));
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
