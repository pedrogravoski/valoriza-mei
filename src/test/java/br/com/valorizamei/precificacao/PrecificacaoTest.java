package br.com.valorizamei.precificacao;

import static br.com.valorizamei.despesa.ApoioDeDespesas.ALUGUEL;
import static br.com.valorizamei.despesa.ApoioDeDespesas.fixaJson;
import static br.com.valorizamei.despesa.ApoioDeDespesas.parametrosJson;
import static br.com.valorizamei.produto.ApoioDeProdutos.ALICATE;
import static br.com.valorizamei.produto.ApoioDeProdutos.CAIXA;
import static br.com.valorizamei.produto.ApoioDeProdutos.FURADEIRA;
import static br.com.valorizamei.produto.ApoioDeProdutos.JOGO_DE_CHAVES;
import static br.com.valorizamei.produto.ApoioDeProdutos.MARTELO;
import static br.com.valorizamei.produto.ApoioDeProdutos.TRENA;
import static br.com.valorizamei.produto.ApoioDeProdutos.produtoJson;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
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
 * Índice de markup e preço de venda sugerido com o estudo de caso da loja de ferramentas
 * (spec precificacao; RF03, RF04).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PrecificacaoTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private ApoioDePrecificacao apoio;

	@BeforeEach
	void limparBanco() {
		BancoDeTeste.limpar(jdbcTemplate);
		apoio = new ApoioDePrecificacao(mockMvc);
	}

	@Test
	void markupDaLojaDeFerramentas() throws Exception {
		String loja = apoio.lojaDoEstudoDeCaso();

		apoio.consultar(loja)
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("\"percentualDespesasVariaveis\":5.00")))
			.andExpect(content().string(containsString("\"percentualDespesasFixas\":10.00")))
			.andExpect(content().string(containsString("\"margemDeLucro\":25.00")))
			.andExpect(content().string(containsString("\"indiceMarkup\":1.6667")));
	}

	@Test
	void markupSemDespesas() throws Exception {
		String loja = apoio.produtos.tokenDaLoja();
		apoio.despesas.gravarParametrosComSucesso(loja, "14000.00", "25.00");

		apoio.consultar(loja)
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("\"percentualDespesasVariaveis\":0.00")))
			.andExpect(content().string(containsString("\"percentualDespesasFixas\":0.00")))
			.andExpect(content().string(containsString("\"indiceMarkup\":1.3333")));
	}

	@Test
	void precosSugeridosDoEstudoDeCaso() throws Exception {
		String loja = apoio.lojaDoEstudoDeCaso();

		apoio.consultar(loja)
			.andExpect(jsonPath("$.produtos", hasSize(6)))
			.andExpect(jsonPath("$.produtos[*].nome", contains(ALICATE, CAIXA, FURADEIRA, JOGO_DE_CHAVES, MARTELO, TRENA)))
			.andExpect(jsonPath("$.produtos[*].custoDireto", contains(28.00, 45.00, 180.00, 25.00, 22.00, 12.00)))
			.andExpect(jsonPath("$.produtos[*].precoSugerido", contains(46.67, 75.00, 300.00, 41.67, 36.67, 20.00)));
	}

	@Test
	void precoCalculadoComOMarkupSemArredondamento() throws Exception {
		String loja = apoio.lojaDoEstudoDeCaso();

		apoio.consultar(loja)
			.andExpect(jsonPath("$.indiceMarkup").value(1.6667))
			.andExpect(jsonPath("$.produtos[2].nome").value(FURADEIRA))
			.andExpect(content().string(containsString("\"precoSugerido\":300.00")));
	}

	@Test
	void precoCalculadoComODfSemArredondamento() throws Exception {
		String loja = apoio.produtos.tokenDaLoja();
		apoio.despesas.registrarComSucesso(loja, fixaJson(ALUGUEL, "1000.00"));
		apoio.despesas.gravarParametrosComSucesso(loja, "3000.00", "25.00");
		apoio.produtos.cadastrarComSucesso(loja, "Kit de brocas", "100.00", 2);

		apoio.consultar(loja)
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("\"percentualDespesasFixas\":33.33")))
			.andExpect(content().string(containsString("\"indiceMarkup\":2.4000")))
			.andExpect(content().string(containsString("\"precoSugerido\":240.00")));
	}

	@Test
	void meiSemProdutos() throws Exception {
		String loja = apoio.produtos.tokenDaLoja();
		apoio.despesas.gravarParametrosComSucesso(loja, "14000.00", "25.00");

		apoio.consultar(loja)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.indiceMarkup").value(1.3333))
			.andExpect(jsonPath("$.produtos", hasSize(0)));
	}

	@Test
	void precoSugeridoDaFuradeira() throws Exception {
		String loja = apoio.lojaDoEstudoDeCaso();
		String furadeira = apoio.idDoProduto(loja, FURADEIRA);

		apoio.consultarProduto(loja, furadeira)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(furadeira))
			.andExpect(jsonPath("$.nome").value(FURADEIRA))
			.andExpect(content().string(containsString("\"custoDireto\":180.00")))
			.andExpect(content().string(containsString("\"indiceMarkup\":1.6667")))
			.andExpect(content().string(containsString("\"precoSugerido\":300.00")));
	}

	@Test
	void precoAcompanhaONovoCusto() throws Exception {
		String loja = apoio.lojaDoEstudoDeCaso();
		String trena = apoio.idDoProduto(loja, TRENA);
		apoio.produtos.alterar(loja, trena, produtoJson(TRENA, "15.00", 5))
			.andExpect(status().isOk());

		apoio.consultarProduto(loja, trena)
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("\"precoSugerido\":25.00")));
	}

	@Test
	void produtoInexistenteNaPrecificacao() throws Exception {
		String loja = apoio.lojaDoEstudoDeCaso();

		apoio.consultarProduto(loja, UUID.randomUUID().toString())
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Produto não encontrado"));
	}

	@Test
	void identificadorInvalidoNaPrecificacao() throws Exception {
		String loja = apoio.lojaDoEstudoDeCaso();

		apoio.consultarProduto(loja, "abc")
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void precificacaoSemParametrosFinanceiros() throws Exception {
		String loja = apoio.produtos.tokenDaLoja();
		apoio.produtos.cadastrarEstudoDeCaso(loja);
		apoio.despesas.registrarEstudoDeCaso(loja);

		apoio.consultar(loja)
			.andExpect(status().is(422))
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(422))
			.andExpect(jsonPath("$.title").value("Parâmetros financeiros não informados"))
			.andExpect(jsonPath("$.detail")
				.value("Informe a receita mensal estimada e a margem de lucro para calcular o preço de venda."))
			.andExpect(jsonPath("$.instance").value(ApoioDePrecificacao.PRECIFICACAO));
	}

	@Test
	void precoDeUmProdutoSemParametrosFinanceiros() throws Exception {
		String loja = apoio.produtos.tokenDaLoja();
		String furadeira = apoio.produtos.cadastrarComSucesso(loja, FURADEIRA, "180.00", 3);

		apoio.consultarProduto(loja, furadeira)
			.andExpect(status().is(422))
			.andExpect(jsonPath("$.title").value("Parâmetros financeiros não informados"));
	}

	@Test
	void produtoInexistenteSemParametrosFinanceirosGera404() throws Exception {
		String loja = apoio.produtos.tokenDaLoja();

		apoio.consultarProduto(loja, UUID.randomUUID().toString())
			.andExpect(status().isNotFound());
	}

	@Test
	void somaDeExatamenteCemPorCento() throws Exception {
		String loja = apoio.lojaDoEstudoDeCaso();
		apoio.despesas.gravarParametros(loja, parametrosJson("14000.00", "85.00")).andExpect(status().isOk());

		apoio.consultar(loja)
			.andExpect(status().is(422))
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Markup inviável"))
			.andExpect(jsonPath("$.detail").value("A soma das despesas variáveis, das despesas fixas rateadas e da "
					+ "margem de lucro é de 100,00% e precisa ser menor que 100%."));
	}

	@Test
	void somaAcimaDeCemPorCento() throws Exception {
		String loja = apoio.lojaDoEstudoDeCaso();
		apoio.despesas.gravarParametros(loja, parametrosJson("14000.00", "90.00")).andExpect(status().isOk());

		apoio.consultarProduto(loja, apoio.idDoProduto(loja, FURADEIRA))
			.andExpect(status().is(422))
			.andExpect(jsonPath("$.detail", containsString("105,00%")));
	}

}
