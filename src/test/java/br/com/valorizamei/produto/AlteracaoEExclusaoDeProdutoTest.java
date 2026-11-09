package br.com.valorizamei.produto;

import static br.com.valorizamei.produto.ApoioDeProdutos.CAIXA;
import static br.com.valorizamei.produto.ApoioDeProdutos.FURADEIRA;
import static br.com.valorizamei.produto.ApoioDeProdutos.TRENA;
import static br.com.valorizamei.produto.ApoioDeProdutos.produtoJson;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import br.com.valorizamei.BancoDeTeste;
import br.com.valorizamei.TestcontainersConfiguration;

/**
 * Alteração e exclusão de produto (spec produtos; RF01).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AlteracaoEExclusaoDeProdutoTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private ApoioDeProdutos produtos;

	private String tokenDaLoja;

	private String furadeira;

	@BeforeEach
	void cadastrarLojaEFuradeira() throws Exception {
		BancoDeTeste.limpar(jdbcTemplate);
		produtos = new ApoioDeProdutos(mockMvc);
		tokenDaLoja = produtos.tokenDaLoja();
		furadeira = produtos.cadastrarComSucesso(tokenDaLoja, FURADEIRA, "180.00", 3);
	}

	@Test
	void reajusteDoCustoDaFuradeira() throws Exception {
		produtos.alterar(tokenDaLoja, furadeira, produtoJson(FURADEIRA, "190.00", 4))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(furadeira))
			.andExpect(jsonPath("$.nome").value(FURADEIRA))
			.andExpect(jsonPath("$.custoDireto").value(190.00))
			.andExpect(jsonPath("$.estoqueMinimo").value(4));

		produtos.consultar(tokenDaLoja, furadeira)
			.andExpect(jsonPath("$.custoDireto").value(190.00))
			.andExpect(jsonPath("$.estoqueMinimo").value(4));
	}

	@Test
	void alteracaoParaONomeDeOutroProdutoDoMei() throws Exception {
		String trena = produtos.cadastrarComSucesso(tokenDaLoja, TRENA, "12.00", 5);

		produtos.alterar(tokenDaLoja, trena, produtoJson("furadeira de impacto 650 w", "12.00", 5))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.title").value("Produto já cadastrado"));

		produtos.consultar(tokenDaLoja, trena)
			.andExpect(jsonPath("$.nome").value(TRENA));
	}

	@Test
	void alteracaoMantendoOProprioNomeComOutraGrafia() throws Exception {
		produtos.alterar(tokenDaLoja, furadeira, produtoJson("FURADEIRA DE IMPACTO 650 W", "180.00", 3))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nome").value("FURADEIRA DE IMPACTO 650 W"));
	}

	@Test
	void alteracaoComDadosInvalidos() throws Exception {
		produtos.alterar(tokenDaLoja, furadeira, produtoJson(FURADEIRA, "0.00", 3))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros[0].campo").value("custoDireto"));

		produtos.consultar(tokenDaLoja, furadeira)
			.andExpect(jsonPath("$.custoDireto").value(180.00));
	}

	@Test
	void alteracaoDeProdutoInexistente() throws Exception {
		produtos.alterar(tokenDaLoja, UUID.randomUUID().toString(), produtoJson(TRENA, "12.00", 5))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.title").value("Produto não encontrado"));
	}

	@Test
	void exclusaoDaCaixaDeFerramentas() throws Exception {
		String caixa = produtos.cadastrarComSucesso(tokenDaLoja, CAIXA, "45.00", 3);

		produtos.excluir(tokenDaLoja, caixa)
			.andExpect(status().isNoContent())
			.andExpect(content().string(""));

		produtos.consultar(tokenDaLoja, caixa)
			.andExpect(status().isNotFound());
		produtos.listar(tokenDaLoja)
			.andExpect(jsonPath("$[*].nome", not(hasItem(CAIXA))))
			.andExpect(jsonPath("$[*].nome", hasItem(FURADEIRA)));
	}

	@Test
	void exclusaoDeProdutoInexistente() throws Exception {
		produtos.excluir(tokenDaLoja, UUID.randomUUID().toString())
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.title").value("Produto não encontrado"));
	}

}
