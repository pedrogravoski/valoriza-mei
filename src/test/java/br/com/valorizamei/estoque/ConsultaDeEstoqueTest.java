package br.com.valorizamei.estoque;

import static br.com.valorizamei.produto.ApoioDeProdutos.ALICATE;
import static br.com.valorizamei.produto.ApoioDeProdutos.CAIXA;
import static br.com.valorizamei.produto.ApoioDeProdutos.FURADEIRA;
import static br.com.valorizamei.produto.ApoioDeProdutos.JOGO_DE_CHAVES;
import static br.com.valorizamei.produto.ApoioDeProdutos.MARTELO;
import static br.com.valorizamei.produto.ApoioDeProdutos.TRENA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import br.com.valorizamei.BancoDeTeste;
import br.com.valorizamei.RelogioFixoConfiguration;
import br.com.valorizamei.TestcontainersConfiguration;

/**
 * Consulta das movimentações e do estoque atual (spec estoque; RF06). A data atual é 15/07/2026.
 */
@Import({ TestcontainersConfiguration.class, RelogioFixoConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
class ConsultaDeEstoqueTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private ApoioDeEstoque apoio;

	private String tokenDaLoja;

	@BeforeEach
	void cadastrarLoja() throws Exception {
		BancoDeTeste.limpar(jdbcTemplate);
		apoio = new ApoioDeEstoque(mockMvc);
		tokenDaLoja = apoio.produtos.tokenDaLoja();
	}

	@Test
	void movimentacoesDaFuradeira() throws Exception {
		String furadeira = movimentarFuradeiraETrena();

		apoio.listarMovimentacoesDoProduto(tokenDaLoja, furadeira)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[*].data", contains("2026-07-10", "2026-07-01")))
			.andExpect(jsonPath("$[*].tipo", contains("SAIDA", "ENTRADA")))
			.andExpect(jsonPath("$[*].nomeProduto", contains(FURADEIRA, FURADEIRA)));
	}

	@Test
	void todasAsMovimentacoes() throws Exception {
		movimentarFuradeiraETrena();

		apoio.listarMovimentacoes(tokenDaLoja)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(3)))
			.andExpect(jsonPath("$[*].data", contains("2026-07-10", "2026-07-05", "2026-07-01")))
			.andExpect(jsonPath("$[1].nomeProduto").value(TRENA));
	}

	@Test
	void consultaDeUmaMovimentacao() throws Exception {
		String furadeira = apoio.produtos.cadastrarComSucesso(tokenDaLoja, FURADEIRA, "180.00", 3);
		String compra = apoio.comprar(tokenDaLoja, furadeira, 10, "2026-07-01");

		apoio.consultarMovimentacao(tokenDaLoja, compra)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(compra))
			.andExpect(jsonPath("$.nomeProduto").value(FURADEIRA))
			.andExpect(jsonPath("$.valorTotal").value(1800.00))
			.andExpect(jsonPath("$.estoqueAtual").doesNotExist());
	}

	@Test
	void movimentacaoInexistente() throws Exception {
		apoio.consultarMovimentacao(tokenDaLoja, UUID.randomUUID().toString())
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Movimentação não encontrada"));
	}

	@Test
	void estoqueDoEstudoDeCaso() throws Exception {
		Map<String, String> ids = apoio.estoqueDoEstudoDeCaso(tokenDaLoja);

		apoio.estoque(tokenDaLoja)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].nome", contains(ALICATE, CAIXA, FURADEIRA, JOGO_DE_CHAVES, MARTELO, TRENA)))
			.andExpect(jsonPath("$[*].estoqueAtual", contains(12, 10, 5, 20, 15, 4)))
			.andExpect(jsonPath("$[*].estoqueMinimo", contains(4, 3, 3, 5, 4, 5)));

		for (String produto : ids.values()) {
			assertThat(saldo(produto)).isEqualTo(somaDasMovimentacoes(produto));
		}
	}

	@Test
	void produtoSemMovimentacoes() throws Exception {
		apoio.produtos.cadastrarComSucesso(tokenDaLoja, FURADEIRA, "180.00", 3);

		apoio.estoque(tokenDaLoja)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].nome").value(FURADEIRA))
			.andExpect(jsonPath("$[0].estoqueAtual").value(0))
			.andExpect(jsonPath("$[0].estoqueMinimo").value(3));
	}

	/** Entrada de 10 furadeiras em 01/07, de 14 trenas em 05/07 e venda de 2 furadeiras em 10/07. */
	private String movimentarFuradeiraETrena() throws Exception {
		String furadeira = apoio.produtos.cadastrarComSucesso(tokenDaLoja, FURADEIRA, "180.00", 3);
		String trena = apoio.produtos.cadastrarComSucesso(tokenDaLoja, TRENA, "12.00", 5);
		apoio.comprar(tokenDaLoja, furadeira, 10, "2026-07-01");
		apoio.comprar(tokenDaLoja, trena, 14, "2026-07-05");
		apoio.vender(tokenDaLoja, furadeira, 2, "300.00", "2026-07-10");
		return furadeira;
	}

	private int saldo(String produtoId) {
		return jdbcTemplate.queryForObject("select quantidade from saldo_estoque where produto_id = ?::uuid",
				Integer.class, produtoId);
	}

	private int somaDasMovimentacoes(String produtoId) {
		return jdbcTemplate.queryForObject("select coalesce(sum(case when tipo = 'ENTRADA' then quantidade "
				+ "else -quantidade end), 0) from movimentacao_estoque where produto_id = ?::uuid", Integer.class,
				produtoId);
	}

}
