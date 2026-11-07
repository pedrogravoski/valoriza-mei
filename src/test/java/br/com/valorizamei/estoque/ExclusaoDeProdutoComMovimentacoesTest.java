package br.com.valorizamei.estoque;

import static br.com.valorizamei.produto.ApoioDeProdutos.FURADEIRA;
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

/**
 * Produto com movimentações de estoque não pode ser excluído (spec produtos; RF01, RF06).
 */
@Import({ TestcontainersConfiguration.class, RelogioFixoConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
class ExclusaoDeProdutoComMovimentacoesTest {

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
	void exclusaoDaFuradeiraComComprasRegistradas() throws Exception {
		String furadeira = apoio.produtos.cadastrarComSucesso(tokenDaLoja, FURADEIRA, "180.00", 3);
		apoio.comprar(tokenDaLoja, furadeira, 10, null);

		apoio.produtos.excluir(tokenDaLoja, furadeira)
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Produto com movimentações"))
			.andExpect(jsonPath("$.detail").value("O produto tem movimentações de estoque e não pode ser excluído."));

		apoio.produtos.consultar(tokenDaLoja, furadeira)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nome").value(FURADEIRA));
	}

	@Test
	void produtoSemMovimentacoesContinuaPodendoSerExcluido() throws Exception {
		String furadeira = apoio.produtos.cadastrarComSucesso(tokenDaLoja, FURADEIRA, "180.00", 3);

		apoio.produtos.excluir(tokenDaLoja, furadeira)
			.andExpect(status().isNoContent());
	}

}
