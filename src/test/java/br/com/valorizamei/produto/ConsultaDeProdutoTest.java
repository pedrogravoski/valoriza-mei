package br.com.valorizamei.produto;

import static br.com.valorizamei.produto.ApoioDeProdutos.ALICATE;
import static br.com.valorizamei.produto.ApoioDeProdutos.CAIXA;
import static br.com.valorizamei.produto.ApoioDeProdutos.FURADEIRA;
import static br.com.valorizamei.produto.ApoioDeProdutos.JOGO_DE_CHAVES;
import static br.com.valorizamei.produto.ApoioDeProdutos.MARTELO;
import static br.com.valorizamei.produto.ApoioDeProdutos.TRENA;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import br.com.valorizamei.TestcontainersConfiguration;

/**
 * Listagem e consulta dos produtos do MEI (spec produtos; RF01).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ConsultaDeProdutoTest {

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
	void produtosDaLojaDeFerramentasEmOrdemAlfabetica() throws Exception {
		produtos.cadastrarEstudoDeCaso(tokenDaLoja);

		produtos.listar(tokenDaLoja)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(6)))
			.andExpect(jsonPath("$[*].nome", contains(ALICATE, CAIXA, FURADEIRA, JOGO_DE_CHAVES, MARTELO, TRENA)))
			.andExpect(jsonPath("$[5].custoDireto").value(12.00))
			.andExpect(jsonPath("$[5].estoqueMinimo").value(5));
	}

	@Test
	void ordemNaoDiferenciaMaiusculasEMinusculas() throws Exception {
		produtos.cadastrarComSucesso(tokenDaLoja, "trena 5 m", "12.00", 5);
		produtos.cadastrarComSucesso(tokenDaLoja, "Alicate universal 8\"", "28.00", 4);
		produtos.cadastrarComSucesso(tokenDaLoja, "martelo unha 27 mm", "22.00", 4);

		produtos.listar(tokenDaLoja)
			.andExpect(jsonPath("$[*].nome", contains(ALICATE, "martelo unha 27 mm", "trena 5 m")));
	}

	@Test
	void meiSemProdutos() throws Exception {
		produtos.listar(tokenDaLoja)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void consultaDaFuradeira() throws Exception {
		String id = produtos.cadastrarComSucesso(tokenDaLoja, FURADEIRA, "180.00", 3);

		produtos.consultar(tokenDaLoja, id)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(id))
			.andExpect(jsonPath("$.nome").value(FURADEIRA))
			.andExpect(jsonPath("$.custoDireto").value(180.00))
			.andExpect(jsonPath("$.estoqueMinimo").value(3));
	}

	@Test
	void produtoInexistente() throws Exception {
		String inexistente = UUID.randomUUID().toString();

		produtos.consultar(tokenDaLoja, inexistente)
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.title").value("Produto não encontrado"))
			.andExpect(jsonPath("$.detail").value("Não existe produto com este identificador."))
			.andExpect(jsonPath("$.instance").value(ApoioDeProdutos.CAMINHO + "/" + inexistente));
	}

	@Test
	void identificadorEmFormatoInvalido() throws Exception {
		produtos.consultar(tokenDaLoja, "abc")
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.title").value("Requisição inválida"));
	}

}
