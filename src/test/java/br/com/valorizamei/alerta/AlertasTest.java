package br.com.valorizamei.alerta;

import static br.com.valorizamei.ApoioDeAutenticacao.bearer;
import static br.com.valorizamei.produto.ApoioDeProdutos.CAIXA;
import static br.com.valorizamei.produto.ApoioDeProdutos.FURADEIRA;
import static br.com.valorizamei.produto.ApoioDeProdutos.TRENA;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.ResultActions;

import br.com.valorizamei.BancoDeTeste;
import br.com.valorizamei.RelogioFixoConfiguration;
import br.com.valorizamei.TestcontainersConfiguration;
import br.com.valorizamei.despesa.ApoioDeDespesas;
import br.com.valorizamei.estoque.ApoioDeEstoque;

/**
 * Alertas de ruptura e de datas comemorativas com sugestões (spec alertas; RF08, RF09). A data
 * atual é 15/07/2026: o Dia dos Pais (09/08/2026) está a 25 dias.
 */
@Import({ TestcontainersConfiguration.class, RelogioFixoConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
class AlertasTest {

	static final String ALERTAS = "/api/v1/alertas";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private ApoioDeEstoque estoque;

	private ApoioDeDespesas despesas;

	private String tokenDaLoja;

	@BeforeEach
	void cadastrarLoja() throws Exception {
		BancoDeTeste.limpar(jdbcTemplate);
		estoque = new ApoioDeEstoque(mockMvc);
		despesas = new ApoioDeDespesas(mockMvc);
		tokenDaLoja = estoque.produtos.tokenDaLoja();
	}

	@Test
	void rupturaNoEstudoDeCaso() throws Exception {
		estoque.estoqueDoEstudoDeCaso(tokenDaLoja);

		alertas()
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.ruptura", hasSize(1)))
			.andExpect(jsonPath("$.ruptura[0].nome").value(TRENA))
			.andExpect(jsonPath("$.ruptura[0].estoqueAtual").value(4))
			.andExpect(jsonPath("$.ruptura[0].estoqueMinimo").value(5));
	}

	@Test
	void produtoSemEstoque() throws Exception {
		estoque.produtos.cadastrarComSucesso(tokenDaLoja, FURADEIRA, "180.00", 3);

		alertas()
			.andExpect(jsonPath("$.ruptura[*].nome", contains(FURADEIRA)))
			.andExpect(jsonPath("$.ruptura[0].estoqueAtual").value(0));
	}

	@Test
	void estoqueIgualAoMinimo() throws Exception {
		String trena = estoque.produtos.cadastrarComSucesso(tokenDaLoja, TRENA, "12.00", 5);
		estoque.comprar(tokenDaLoja, trena, 5, null);

		alertas()
			.andExpect(jsonPath("$.ruptura[*].nome", contains(TRENA)));
	}

	@Test
	void estoqueMinimoZeroNaoGeraAlerta() throws Exception {
		estoque.produtos.cadastrarComSucesso(tokenDaLoja, "Brinde para clientes", "1.00", 0);

		alertas()
			.andExpect(jsonPath("$.ruptura", hasSize(0)));
	}

	@Test
	void diaDosPaisNoEstudoDeCaso() throws Exception {
		alertas()
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.dataReferencia").value("2026-07-15"))
			.andExpect(jsonPath("$.datasComemorativas", hasSize(1)))
			.andExpect(jsonPath("$.datasComemorativas[0].nome").value("Dia dos Pais"))
			.andExpect(jsonPath("$.datasComemorativas[0].data").value("2026-08-09"))
			.andExpect(jsonPath("$.datasComemorativas[0].diasRestantes").value(25))
			.andExpect(jsonPath("$.datasComemorativas[0].personalizada").value(false));
	}

	@Test
	void dataPersonalizadaProxima() throws Exception {
		mockMvc.perform(post("/api/v1/datas-comemorativas").header(HttpHeaders.AUTHORIZATION, bearer(tokenDaLoja))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nome": "Aniversário da cidade", "dia": 1, "mes": 8}
						"""))
			.andExpect(status().isCreated());

		alertas()
			.andExpect(jsonPath("$.datasComemorativas[*].nome", contains("Aniversário da cidade", "Dia dos Pais")))
			.andExpect(jsonPath("$.datasComemorativas[0].data").value("2026-08-01"))
			.andExpect(jsonPath("$.datasComemorativas[0].diasRestantes").value(17))
			.andExpect(jsonPath("$.datasComemorativas[0].personalizada").value(true));
	}

	@Test
	void reforcoDaFuradeiraEDaTrena() throws Exception {
		estoque.estoqueDoEstudoDeCaso(tokenDaLoja);

		alertas()
			.andExpect(jsonPath("$.sugestoes[?(@.tipo == 'REFORCAR_ESTOQUE')].nome", contains(FURADEIRA, TRENA)))
			.andExpect(jsonPath("$.sugestoes[?(@.nome == '" + FURADEIRA + "')].motivo",
					contains("Vendeu 6 unidades nos últimos 30 dias e tem 5 em estoque.")));
	}

	@Test
	void promocaoDaCaixaDeFerramentas() throws Exception {
		estoque.estoqueDoEstudoDeCaso(tokenDaLoja);

		alertas()
			.andExpect(jsonPath("$.sugestoes[?(@.tipo == 'PROMOCAO')].nome", contains(CAIXA)))
			.andExpect(jsonPath("$.sugestoes[?(@.tipo == 'PROMOCAO')].motivo",
					contains("Nenhuma venda nos últimos 30 dias, com 10 unidades em estoque.")));
	}

	@Test
	void furadeiraVendidaAbaixoDoPrecoSugerido() throws Exception {
		despesas.registrarEstudoDeCaso(tokenDaLoja);
		despesas.gravarParametrosComSucesso(tokenDaLoja, "14000.00", "25.00");
		String furadeira = estoque.produtos.cadastrarComSucesso(tokenDaLoja, FURADEIRA, "180.00", 3);
		estoque.comprar(tokenDaLoja, furadeira, 11, "2026-06-20");
		estoque.vender(tokenDaLoja, furadeira, 6, "280.00", "2026-07-10");

		alertas()
			.andExpect(jsonPath("$.sugestoes[*].tipo", contains("REFORCAR_ESTOQUE", "REAJUSTE_PRECO")))
			.andExpect(jsonPath("$.sugestoes[1].nome").value(FURADEIRA))
			.andExpect(jsonPath("$.sugestoes[1].motivo").value("Preço médio de venda de R$ 280,00 nos últimos 30 dias, "
					+ "abaixo do preço sugerido de R$ 300,00."));
	}

	@Test
	void vendasNoPrecoSugerido() throws Exception {
		despesas.registrarEstudoDeCaso(tokenDaLoja);
		despesas.gravarParametrosComSucesso(tokenDaLoja, "14000.00", "25.00");
		estoque.estoqueDoEstudoDeCaso(tokenDaLoja);

		alertas()
			.andExpect(jsonPath("$.sugestoes", hasSize(3)))
			.andExpect(jsonPath("$.sugestoes[*].tipo", not(hasItem("REAJUSTE_PRECO"))));
	}

	@Test
	void semParametrosFinanceiros() throws Exception {
		estoque.estoqueDoEstudoDeCaso(tokenDaLoja);

		alertas()
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.ruptura[*].nome", contains(TRENA)))
			.andExpect(jsonPath("$.sugestoes[*].tipo", contains("PROMOCAO", "REFORCAR_ESTOQUE", "REFORCAR_ESTOQUE")))
			.andExpect(jsonPath("$.sugestoes[*].tipo", not(hasItem("REAJUSTE_PRECO"))));
	}

	private ResultActions alertas() throws Exception {
		return mockMvc.perform(get(ALERTAS).header(HttpHeaders.AUTHORIZATION, bearer(tokenDaLoja)));
	}

}
