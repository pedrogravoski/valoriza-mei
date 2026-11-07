package br.com.valorizamei.estoque;

import static br.com.valorizamei.estoque.ApoioDeEstoque.ESTOQUE;
import static br.com.valorizamei.estoque.ApoioDeEstoque.FLUXO_DE_CAIXA;
import static br.com.valorizamei.estoque.ApoioDeEstoque.movimentacaoJson;
import static br.com.valorizamei.produto.ApoioDeProdutos.FURADEIRA;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 * Isolamento do estoque e do fluxo de caixa entre MEIs (specs estoque e fluxo-de-caixa; RF10,
 * RNF07). A data atual é 15/07/2026.
 */
@Import({ TestcontainersConfiguration.class, RelogioFixoConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
class EstoqueIsolamentoTest {

	private static final String CADERNO = "Caderno universitário 200 folhas";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private ApoioDeEstoque apoio;

	private String tokenDaLoja;

	private String tokenDaPapelaria;

	private String furadeira;

	private String compraDaLoja;

	@BeforeEach
	void cadastrarLojaEPapelaria() throws Exception {
		BancoDeTeste.limpar(jdbcTemplate);
		apoio = new ApoioDeEstoque(mockMvc);
		tokenDaLoja = apoio.produtos.tokenDaLoja();
		tokenDaPapelaria = apoio.produtos.tokenDaPapelaria();
		furadeira = apoio.produtos.cadastrarComSucesso(tokenDaLoja, FURADEIRA, "180.00", 3);
		compraDaLoja = apoio.comprar(tokenDaLoja, furadeira, 10, "2026-07-01");
		String caderno = apoio.produtos.cadastrarComSucesso(tokenDaPapelaria, CADERNO, "16.00", 10);
		apoio.comprar(tokenDaPapelaria, caderno, 50, "2026-07-01");
	}

	@Test
	void movimentacaoDeProdutoDeOutroMei() throws Exception {
		apoio.registrar(tokenDaPapelaria, movimentacaoJson(furadeira, "ENTRADA", 10, null, null))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.title").value("Produto não encontrado"));

		apoio.estoque(tokenDaLoja)
			.andExpect(jsonPath("$[0].estoqueAtual").value(10));
	}

	@Test
	void cadaMeiConsultaOProprioEstoque() throws Exception {
		apoio.estoque(tokenDaLoja)
			.andExpect(jsonPath("$[*].nome", contains(FURADEIRA)))
			.andExpect(jsonPath("$[*].estoqueAtual", contains(10)));
		apoio.estoque(tokenDaPapelaria)
			.andExpect(jsonPath("$[*].nome", contains(CADERNO)))
			.andExpect(jsonPath("$[*].estoqueAtual", contains(50)));

		apoio.consultarMovimentacao(tokenDaPapelaria, compraDaLoja)
			.andExpect(status().isNotFound());
		apoio.listarMovimentacoes(tokenDaPapelaria)
			.andExpect(jsonPath("$[*].nomeProduto", contains(CADERNO)));
		apoio.listarMovimentacoesDoProduto(tokenDaPapelaria, furadeira)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isEmpty());
	}

	@Test
	void cadaMeiConsultaOProprioCaixa() throws Exception {
		apoio.fluxoDeCaixa(tokenDaLoja, "2026-07-01", "2026-07-31")
			.andExpect(content().string(containsString("\"totalSaidas\":1800.00")))
			.andExpect(jsonPath("$.lancamentos[*].descricao", contains("Compra: 10 × " + FURADEIRA)));
		apoio.fluxoDeCaixa(tokenDaPapelaria, "2026-07-01", "2026-07-31")
			.andExpect(content().string(containsString("\"totalSaidas\":800.00")))
			.andExpect(jsonPath("$.lancamentos[*].descricao", contains("Compra: 50 × " + CADERNO)));
	}

	@Test
	void estoqueSemToken() throws Exception {
		mockMvc.perform(get(ESTOQUE))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
	}

	@Test
	void fluxoDeCaixaSemToken() throws Exception {
		mockMvc.perform(get(FLUXO_DE_CAIXA))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
	}

}
