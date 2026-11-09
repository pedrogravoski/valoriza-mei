package br.com.valorizamei.estoque;

import static br.com.valorizamei.estoque.ApoioDeEstoque.MOVIMENTACOES;
import static br.com.valorizamei.estoque.ApoioDeEstoque.movimentacaoJson;
import static br.com.valorizamei.produto.ApoioDeProdutos.CAIXA;
import static br.com.valorizamei.produto.ApoioDeProdutos.FURADEIRA;
import static br.com.valorizamei.produto.ApoioDeProdutos.TRENA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import br.com.valorizamei.BancoDeTeste;
import br.com.valorizamei.RelogioFixoConfiguration;
import br.com.valorizamei.TestcontainersConfiguration;

/**
 * Registro de entradas e saídas de estoque, validação e venda sem estoque (spec estoque; RF06).
 * A data atual é 15/07/2026.
 */
@Import({ TestcontainersConfiguration.class, RelogioFixoConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
class MovimentacaoEstoqueTest {

	private static final String UUID_REGEX = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private ApoioDeEstoque apoio;

	private String tokenDaLoja;

	private String furadeira;

	@BeforeEach
	void cadastrarLojaEFuradeira() throws Exception {
		BancoDeTeste.limpar(jdbcTemplate);
		apoio = new ApoioDeEstoque(mockMvc);
		tokenDaLoja = apoio.produtos.tokenDaLoja();
		furadeira = apoio.produtos.cadastrarComSucesso(tokenDaLoja, FURADEIRA, "180.00", 3);
	}

	@Test
	void compraDeFuradeiras() throws Exception {
		apoio.registrar(tokenDaLoja, movimentacaoJson(furadeira, "ENTRADA", 10, null, null))
			.andExpect(status().isCreated())
			.andExpect(header().string(HttpHeaders.LOCATION, matchesPattern(".*" + MOVIMENTACOES + "/" + UUID_REGEX)))
			.andExpect(jsonPath("$.produtoId").value(furadeira))
			.andExpect(jsonPath("$.nomeProduto").value(FURADEIRA))
			.andExpect(jsonPath("$.tipo").value("ENTRADA"))
			.andExpect(jsonPath("$.quantidade").value(10))
			.andExpect(content().string(containsString("\"valorUnitario\":180.00")))
			.andExpect(content().string(containsString("\"valorTotal\":1800.00")))
			.andExpect(jsonPath("$.estoqueAtual").value(10));
	}

	@Test
	void vendaDeFuradeiras() throws Exception {
		apoio.comprar(tokenDaLoja, furadeira, 10, null);

		apoio.registrar(tokenDaLoja, movimentacaoJson(furadeira, "SAIDA", 2, "300.00", null))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.tipo").value("SAIDA"))
			.andExpect(jsonPath("$.quantidade").value(2))
			.andExpect(content().string(containsString("\"valorUnitario\":300.00")))
			.andExpect(content().string(containsString("\"valorTotal\":600.00")))
			.andExpect(jsonPath("$.estoqueAtual").value(8));
	}

	@Test
	void camposObrigatoriosAusentes() throws Exception {
		apoio.registrar(tokenDaLoja, "{}")
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.erros[*].campo", containsInAnyOrder("produtoId", "tipo", "quantidade")));

		assertThat(contar("movimentacao_estoque")).isZero();
	}

	@Test
	void quantidadeZero() throws Exception {
		apoio.registrar(tokenDaLoja, movimentacaoJson(furadeira, "ENTRADA", 0, null, null))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros", hasSize(1)))
			.andExpect(jsonPath("$.erros[0].campo").value("quantidade"));

		assertThat(contar("movimentacao_estoque")).isZero();
	}

	@Test
	void vendaSemValorUnitario() throws Exception {
		apoio.comprar(tokenDaLoja, furadeira, 10, null);

		apoio.registrar(tokenDaLoja, movimentacaoJson(furadeira, "SAIDA", 1, null, null))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros", hasSize(1)))
			.andExpect(jsonPath("$.erros[0].campo").value("valorUnitario"))
			.andExpect(jsonPath("$.erros[0].mensagem").value("informe o valor unitário da venda"));
	}

	@Test
	void compraComValorUnitario() throws Exception {
		apoio.registrar(tokenDaLoja, movimentacaoJson(furadeira, "ENTRADA", 10, "170.00", null))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros", hasSize(1)))
			.andExpect(jsonPath("$.erros[0].campo").value("valorUnitario"));

		assertThat(contar("movimentacao_estoque")).isZero();
	}

	@Test
	void valorDeVendaComMaisDeDuasCasasDecimais() throws Exception {
		apoio.comprar(tokenDaLoja, furadeira, 10, null);

		apoio.registrar(tokenDaLoja, movimentacaoJson(furadeira, "SAIDA", 1, "300.005", null))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros[0].campo").value("valorUnitario"))
			.andExpect(jsonPath("$.erros[0].mensagem").value("informe o valor com no máximo 2 casas decimais"));
	}

	@Test
	void dataFutura() throws Exception {
		apoio.registrar(tokenDaLoja, movimentacaoJson(furadeira, "ENTRADA", 10, null, "2026-07-16"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros", hasSize(1)))
			.andExpect(jsonPath("$.erros[0].campo").value("data"))
			.andExpect(jsonPath("$.erros[0].mensagem").value("a data não pode ser futura"));

		assertThat(contar("movimentacao_estoque")).isZero();
	}

	@Test
	void movimentacaoSemData() throws Exception {
		apoio.registrar(tokenDaLoja, movimentacaoJson(furadeira, "ENTRADA", 10, null, null))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data").value("2026-07-15"));
	}

	@Test
	void vendaDeDiaAnterior() throws Exception {
		apoio.comprar(tokenDaLoja, furadeira, 10, "2026-07-01");

		apoio.registrar(tokenDaLoja, movimentacaoJson(furadeira, "SAIDA", 1, "300.00", "2026-07-10"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data").value("2026-07-10"));
	}

	@Test
	void vendaDeTrenasAcimaDoEstoque() throws Exception {
		String trena = apoio.produtos.cadastrarComSucesso(tokenDaLoja, TRENA, "12.00", 5);
		apoio.comprar(tokenDaLoja, trena, 4, null);
		int movimentacoes = contar("movimentacao_estoque");
		int lancamentos = contar("lancamento_caixa");

		apoio.registrar(tokenDaLoja, movimentacaoJson(trena, "SAIDA", 5, "20.00", null))
			.andExpect(status().is(422))
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Estoque insuficiente"))
			.andExpect(jsonPath("$.detail").value("O produto Trena 5 m tem 4 unidades em estoque; não é possível "
					+ "registrar a saída de 5."))
			.andExpect(jsonPath("$.detail", allOf(containsString(TRENA), containsString("4"), containsString("5"))));

		assertThat(contar("movimentacao_estoque")).isEqualTo(movimentacoes);
		assertThat(contar("lancamento_caixa")).isEqualTo(lancamentos);
		assertThat(saldo(trena)).isEqualTo(4);
	}

	@Test
	void vendaDeProdutoSemMovimentacoes() throws Exception {
		String caixa = apoio.produtos.cadastrarComSucesso(tokenDaLoja, CAIXA, "45.00", 3);

		apoio.registrar(tokenDaLoja, movimentacaoJson(caixa, "SAIDA", 1, "75.00", null))
			.andExpect(status().is(422))
			.andExpect(jsonPath("$.detail", containsString("tem 0 unidades em estoque")));

		assertThat(contar("movimentacao_estoque")).isZero();
	}

	@Test
	void vendaDeTodoOEstoque() throws Exception {
		String trena = apoio.produtos.cadastrarComSucesso(tokenDaLoja, TRENA, "12.00", 5);
		apoio.comprar(tokenDaLoja, trena, 4, null);

		apoio.registrar(tokenDaLoja, movimentacaoJson(trena, "SAIDA", 4, "20.00", null))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.estoqueAtual").value(0));
	}

	@Test
	void produtoInexistente() throws Exception {
		apoio.registrar(tokenDaLoja, movimentacaoJson(UUID.randomUUID().toString(), "ENTRADA", 10, null, null))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.title").value("Produto não encontrado"));

		assertThat(contar("movimentacao_estoque")).isZero();
		assertThat(contar("saldo_estoque")).isZero();
		assertThat(contar("lancamento_caixa")).isZero();
	}

	private int contar(String tabela) {
		return jdbcTemplate.queryForObject("select count(*) from " + tabela, Integer.class);
	}

	private int saldo(String produtoId) {
		return jdbcTemplate.queryForObject("select quantidade from saldo_estoque where produto_id = ?::uuid",
				Integer.class, produtoId);
	}

}
