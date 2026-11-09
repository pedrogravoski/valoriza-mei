package br.com.valorizamei.produto;

import static br.com.valorizamei.produto.ApoioDeProdutos.CAMINHO;
import static br.com.valorizamei.produto.ApoioDeProdutos.FURADEIRA;
import static br.com.valorizamei.produto.ApoioDeProdutos.TRENA;
import static br.com.valorizamei.produto.ApoioDeProdutos.produtoJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
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
 * Cadastro de produto, validação dos dados e nome único no MEI (spec produtos; RF01).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CadastroDeProdutoTest {

	private static final String UUID_REGEX = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private ApoioDeProdutos produtos;

	private String tokenDaLoja;

	@BeforeEach
	void cadastrarLoja() throws Exception {
		BancoDeTeste.limpar(jdbcTemplate);
		produtos = new ApoioDeProdutos(mockMvc);
		tokenDaLoja = produtos.tokenDaLoja();
	}

	@Test
	void cadastroDaFuradeiraDaLojaDeFerramentas() throws Exception {
		produtos.cadastrar(tokenDaLoja, produtoJson(FURADEIRA, "180.00", 3))
			.andExpect(status().isCreated())
			.andExpect(header().string(HttpHeaders.LOCATION, matchesPattern(".*" + CAMINHO + "/" + UUID_REGEX)))
			.andExpect(jsonPath("$.id", matchesPattern(UUID_REGEX)))
			.andExpect(jsonPath("$.nome").value(FURADEIRA))
			.andExpect(jsonPath("$.custoDireto").value(180.00))
			.andExpect(jsonPath("$.estoqueMinimo").value(3))
			.andExpect(content().string(containsString("\"custoDireto\":180.00")));
	}

	@Test
	void custoSemCasasDecimaisEhDevolvidoComDuasCasas() throws Exception {
		produtos.cadastrar(tokenDaLoja, produtoJson(FURADEIRA, "180", 3))
			.andExpect(status().isCreated())
			.andExpect(content().string(containsString("\"custoDireto\":180.00")));
	}

	@Test
	void nomeComEspacosNasPontas() throws Exception {
		produtos.cadastrar(tokenDaLoja, produtoJson("  " + TRENA + "  ", "12.00", 5))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.nome").value(TRENA));
	}

	@Test
	void camposObrigatoriosAusentes() throws Exception {
		produtos.cadastrar(tokenDaLoja, "{}")
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.erros[*].campo", containsInAnyOrder("nome", "custoDireto", "estoqueMinimo")));

		assertThat(contarProdutos()).isZero();
	}

	@Test
	void custoDiretoZero() throws Exception {
		produtos.cadastrar(tokenDaLoja, produtoJson(TRENA, "0.00", 5))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros", hasSize(1)))
			.andExpect(jsonPath("$.erros[0].campo").value("custoDireto"));

		assertThat(contarProdutos()).isZero();
	}

	@Test
	void custoDiretoComMaisDeDuasCasasDecimais() throws Exception {
		produtos.cadastrar(tokenDaLoja, produtoJson(TRENA, "12.005", 5))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros", hasSize(1)))
			.andExpect(jsonPath("$.erros[0].campo").value("custoDireto"))
			.andExpect(jsonPath("$.erros[0].mensagem").value("informe o valor com no máximo 2 casas decimais"));

		assertThat(contarProdutos()).isZero();
	}

	@Test
	void estoqueMinimoNegativo() throws Exception {
		produtos.cadastrar(tokenDaLoja, produtoJson(TRENA, "12.00", -1))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros", hasSize(1)))
			.andExpect(jsonPath("$.erros[0].campo").value("estoqueMinimo"));

		assertThat(contarProdutos()).isZero();
	}

	@Test
	void nomeAcimaDe120Caracteres() throws Exception {
		produtos.cadastrar(tokenDaLoja, produtoJson("a".repeat(121), "12.00", 5))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros[0].campo").value("nome"));
	}

	@Test
	void produtoRepetidoComOutraGrafia() throws Exception {
		produtos.cadastrarComSucesso(tokenDaLoja, FURADEIRA, "180.00", 3);

		produtos.cadastrar(tokenDaLoja, produtoJson("FURADEIRA DE IMPACTO 650 W", "180.00", 3))
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.title").value("Produto já cadastrado"))
			.andExpect(jsonPath("$.detail").value("Já existe um produto com este nome."));

		assertThat(contarProdutos()).isEqualTo(1);
	}

	@Test
	void mesmoNomeEmMeisDiferentes() throws Exception {
		produtos.cadastrarComSucesso(tokenDaLoja, TRENA, "12.00", 5);
		String tokenDaPapelaria = produtos.tokenDaPapelaria();

		produtos.cadastrar(tokenDaPapelaria, produtoJson(TRENA, "12.00", 5))
			.andExpect(status().isCreated());
	}

	private int contarProdutos() {
		return jdbcTemplate.queryForObject("select count(*) from produto", Integer.class);
	}

}
