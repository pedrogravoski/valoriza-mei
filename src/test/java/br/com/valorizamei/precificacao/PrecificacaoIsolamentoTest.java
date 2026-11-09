package br.com.valorizamei.precificacao;

import static br.com.valorizamei.produto.ApoioDeProdutos.FURADEIRA;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
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
import br.com.valorizamei.TestcontainersConfiguration;

/**
 * Isolamento da precificação entre MEIs (spec precificacao; RF10, RNF07).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PrecificacaoIsolamentoTest {

	private static final String CADERNO = "Caderno universitário 200 folhas";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private ApoioDePrecificacao apoio;

	private String tokenDaLoja;

	private String tokenDaPapelaria;

	@BeforeEach
	void cadastrarLojaEPapelaria() throws Exception {
		BancoDeTeste.limpar(jdbcTemplate);
		apoio = new ApoioDePrecificacao(mockMvc);
		tokenDaLoja = apoio.lojaDoEstudoDeCaso();
		tokenDaPapelaria = apoio.produtos.tokenDaPapelaria();
		apoio.despesas.gravarParametrosComSucesso(tokenDaPapelaria, "6000.00", "20.00");
		apoio.produtos.cadastrarComSucesso(tokenDaPapelaria, CADERNO, "16.00", 10);
	}

	@Test
	void cadaMeiComOProprioMarkup() throws Exception {
		apoio.consultar(tokenDaLoja)
			.andExpect(jsonPath("$.indiceMarkup").value(1.6667))
			.andExpect(jsonPath("$.produtos", hasSize(6)))
			.andExpect(jsonPath("$.produtos[*].nome", not(hasItem(CADERNO))));

		apoio.consultar(tokenDaPapelaria)
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("\"indiceMarkup\":1.2500")))
			.andExpect(jsonPath("$.percentualDespesasFixas").value(0.00))
			.andExpect(jsonPath("$.produtos[*].nome", contains(CADERNO)))
			.andExpect(jsonPath("$.produtos[0].precoSugerido").value(20.00));
	}

	@Test
	void precoDeProdutoDeOutroMei() throws Exception {
		String furadeira = apoio.idDoProduto(tokenDaLoja, FURADEIRA);

		apoio.consultarProduto(tokenDaPapelaria, furadeira)
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Produto não encontrado"));
	}

	@Test
	void precificacaoSemToken() throws Exception {
		mockMvc.perform(get(ApoioDePrecificacao.PRECIFICACAO))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
	}

}
