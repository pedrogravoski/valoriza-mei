package br.com.valorizamei.produto;

import static br.com.valorizamei.produto.ApoioDeProdutos.CAMINHO;
import static br.com.valorizamei.produto.ApoioDeProdutos.EMAIL_PAPELARIA;
import static br.com.valorizamei.produto.ApoioDeProdutos.FURADEIRA;
import static br.com.valorizamei.produto.ApoioDeProdutos.TRENA;
import static br.com.valorizamei.produto.ApoioDeProdutos.produtoJson;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import br.com.valorizamei.TestcontainersConfiguration;

/**
 * Isolamento dos produtos entre MEIs (spec produtos; RF10, RNF07).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ProdutoIsolamentoTest {

	private static final String CADERNO = "Caderno universitário 200 folhas";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private ApoioDeProdutos produtos;

	private String tokenDaLoja;

	private String tokenDaPapelaria;

	private String furadeira;

	@BeforeEach
	void cadastrarLojaEPapelaria() throws Exception {
		BancoDeTeste.limpar(jdbcTemplate);
		produtos = new ApoioDeProdutos(mockMvc);
		tokenDaLoja = produtos.tokenDaLoja();
		tokenDaPapelaria = produtos.tokenDaPapelaria();
		furadeira = produtos.cadastrarComSucesso(tokenDaLoja, FURADEIRA, "180.00", 3);
	}

	@Test
	void cadaMeiListaSomenteOsPropriosProdutos() throws Exception {
		produtos.cadastrarComSucesso(tokenDaPapelaria, CADERNO, "15.00", 10);

		produtos.listar(tokenDaLoja)
			.andExpect(jsonPath("$[*].nome", contains(FURADEIRA)));
		produtos.listar(tokenDaPapelaria)
			.andExpect(jsonPath("$[*].nome", contains(CADERNO)));
	}

	@Test
	void consultaAlteracaoEExclusaoDoProdutoDeOutroMei() throws Exception {
		produtos.consultar(tokenDaPapelaria, furadeira)
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Produto não encontrado"));
		produtos.alterar(tokenDaPapelaria, furadeira, produtoJson(CADERNO, "1.00", 0))
			.andExpect(status().isNotFound());
		produtos.excluir(tokenDaPapelaria, furadeira)
			.andExpect(status().isNotFound());

		produtos.consultar(tokenDaLoja, furadeira)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nome").value(FURADEIRA))
			.andExpect(jsonPath("$.custoDireto").value(180.00))
			.andExpect(jsonPath("$.estoqueMinimo").value(3));
	}

	@Test
	void tenantEnviadoNoCorpoEhIgnorado() throws Exception {
		UUID papelaria = jdbcTemplate.queryForObject("select tenant_id from usuario where email = ?", UUID.class,
				EMAIL_PAPELARIA);
		String comTenant = """
				{"nome": "%s", "custoDireto": 12.00, "estoqueMinimo": 5, "tenantId": "%s"}
				""".formatted(TRENA, papelaria);

		produtos.cadastrar(tokenDaLoja, comTenant)
			.andExpect(status().isCreated());

		produtos.listar(tokenDaLoja)
			.andExpect(jsonPath("$[*].nome", contains(FURADEIRA, TRENA)));
		produtos.listar(tokenDaPapelaria)
			.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void tenantEnviadoNaQueryEhIgnorado() throws Exception {
		UUID papelaria = jdbcTemplate.queryForObject("select tenant_id from usuario where email = ?", UUID.class,
				EMAIL_PAPELARIA);

		mockMvc.perform(get(CAMINHO).param("tenantId", papelaria.toString())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDaLoja))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].nome", contains(FURADEIRA)));
	}

	@Test
	void produtosSemToken() throws Exception {
		mockMvc.perform(get(CAMINHO))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
	}

}
