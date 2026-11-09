package br.com.valorizamei.caixa;

import static br.com.valorizamei.estoque.ApoioDeEstoque.movimentacaoJson;
import static br.com.valorizamei.produto.ApoioDeProdutos.FURADEIRA;
import static br.com.valorizamei.produto.ApoioDeProdutos.TRENA;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import br.com.valorizamei.estoque.ApoioDeEstoque;

/**
 * Lançamentos gerados pelas compras e vendas e consulta do fluxo de caixa por período (spec
 * fluxo-de-caixa; RF07). A data atual é 15/07/2026.
 */
@Import({ TestcontainersConfiguration.class, RelogioFixoConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
class FluxoDeCaixaTest {

	private static final String JULHO_INICIO = "2026-07-01";

	private static final String JULHO_FIM = "2026-07-31";

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
	void compraDeFuradeirasNoCaixa() throws Exception {
		apoio.comprar(tokenDaLoja, furadeira, 10, "2026-07-01");

		apoio.fluxoDeCaixa(tokenDaLoja, JULHO_INICIO, JULHO_FIM)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.lancamentos", hasSize(1)))
			.andExpect(jsonPath("$.lancamentos[0].data").value("2026-07-01"))
			.andExpect(jsonPath("$.lancamentos[0].tipo").value("SAIDA"))
			.andExpect(jsonPath("$.lancamentos[0].descricao").value("Compra: 10 × Furadeira de impacto 650 W"))
			.andExpect(content().string(containsString("\"valor\":1800.00")));
	}

	@Test
	void vendaDeFuradeirasNoCaixa() throws Exception {
		apoio.comprar(tokenDaLoja, furadeira, 10, "2026-07-01");
		apoio.vender(tokenDaLoja, furadeira, 2, "300.00", "2026-07-10");

		apoio.fluxoDeCaixa(tokenDaLoja, JULHO_INICIO, JULHO_FIM)
			.andExpect(jsonPath("$.lancamentos", hasSize(2)))
			.andExpect(jsonPath("$.lancamentos[1].data").value("2026-07-10"))
			.andExpect(jsonPath("$.lancamentos[1].tipo").value("ENTRADA"))
			.andExpect(jsonPath("$.lancamentos[1].descricao").value("Venda: 2 × Furadeira de impacto 650 W"))
			.andExpect(content().string(containsString("\"valor\":600.00")));
	}

	@Test
	void vendaRecusadaNaoAlteraOCaixa() throws Exception {
		String trena = apoio.produtos.cadastrarComSucesso(tokenDaLoja, TRENA, "12.00", 5);
		apoio.comprar(tokenDaLoja, trena, 4, "2026-07-01");

		apoio.registrar(tokenDaLoja, movimentacaoJson(trena, "SAIDA", 5, "20.00", "2026-07-02"))
			.andExpect(status().is(422));

		apoio.fluxoDeCaixa(tokenDaLoja, JULHO_INICIO, JULHO_FIM)
			.andExpect(jsonPath("$.lancamentos", hasSize(1)))
			.andExpect(jsonPath("$.lancamentos[0].tipo").value("SAIDA"))
			.andExpect(content().string(containsString("\"totalEntradas\":0.00")));
	}

	@Test
	void totaisDosPrimeirosDezDiasDeJulho() throws Exception {
		apoio.comprar(tokenDaLoja, furadeira, 10, "2026-07-01");
		apoio.vender(tokenDaLoja, furadeira, 2, "300.00", "2026-07-05");
		apoio.vender(tokenDaLoja, furadeira, 1, "300.00", "2026-07-12");

		apoio.fluxoDeCaixa(tokenDaLoja, "2026-07-01", "2026-07-10")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.inicio").value("2026-07-01"))
			.andExpect(jsonPath("$.fim").value("2026-07-10"))
			.andExpect(content().string(containsString("\"totalEntradas\":600.00")))
			.andExpect(content().string(containsString("\"totalSaidas\":1800.00")))
			.andExpect(content().string(containsString("\"saldo\":-1200.00")))
			.andExpect(jsonPath("$.lancamentos[*].data", contains("2026-07-01", "2026-07-05")));
	}

	@Test
	void periodoPadraoEhOMesAtual() throws Exception {
		apoio.comprar(tokenDaLoja, furadeira, 5, "2026-06-30");
		apoio.comprar(tokenDaLoja, furadeira, 5, "2026-07-01");

		apoio.fluxoDeCaixa(tokenDaLoja, null, null)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.inicio").value("2026-07-01"))
			.andExpect(jsonPath("$.fim").value("2026-07-31"))
			.andExpect(jsonPath("$.lancamentos[*].data", contains("2026-07-01")));
	}

	@Test
	void periodoSemLancamentos() throws Exception {
		apoio.fluxoDeCaixa(tokenDaLoja, "2026-05-01", "2026-05-31")
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("\"totalEntradas\":0.00")))
			.andExpect(content().string(containsString("\"totalSaidas\":0.00")))
			.andExpect(content().string(containsString("\"saldo\":0.00")))
			.andExpect(jsonPath("$.lancamentos", hasSize(0)));
	}

	@Test
	void periodoInvertido() throws Exception {
		apoio.fluxoDeCaixa(tokenDaLoja, JULHO_FIM, JULHO_INICIO)
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Período inválido"));
	}

	@Test
	void dataEmFormatoInvalido() throws Exception {
		apoio.fluxoDeCaixa(tokenDaLoja, "01/07/2026", null)
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

}
