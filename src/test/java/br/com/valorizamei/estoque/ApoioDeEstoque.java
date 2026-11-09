package br.com.valorizamei.estoque;

import static br.com.valorizamei.ApoioDeAutenticacao.bearer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import br.com.valorizamei.produto.ApoioDeProdutos;

/**
 * Compras, vendas, estoque e fluxo de caixa pela API, com o estoque do estudo de caso da loja de
 * ferramentas (seção 4.1).
 */
public final class ApoioDeEstoque {

	public static final String MOVIMENTACOES = "/api/v1/estoque/movimentacoes";

	public static final String ESTOQUE = "/api/v1/estoque";

	public static final String FLUXO_DE_CAIXA = "/api/v1/fluxo-de-caixa";

	private final MockMvc mockMvc;

	public final ApoioDeProdutos produtos;

	public ApoioDeEstoque(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
		this.produtos = new ApoioDeProdutos(mockMvc);
	}

	/**
	 * Cadastra os 6 produtos do estudo de caso e registra entradas e saídas que deixam o estoque
	 * atual da seção 4.1 (furadeira 5, jogo de chaves 20, martelo 15, trena 4, alicate 12 e caixa
	 * 10), com as vendas dos últimos 30 dias (6, 9, 5, 10, 4 e 0). Devolve os ids pelo nome.
	 */
	public Map<String, String> estoqueDoEstudoDeCaso(String token) throws Exception {
		Map<String, String> ids = new LinkedHashMap<>();
		ids.put(ApoioDeProdutos.FURADEIRA, comprarEVender(token, ApoioDeProdutos.FURADEIRA, "180.00", 3, 11, 6, "300.00"));
		ids.put(ApoioDeProdutos.JOGO_DE_CHAVES,
				comprarEVender(token, ApoioDeProdutos.JOGO_DE_CHAVES, "25.00", 5, 29, 9, "41.67"));
		ids.put(ApoioDeProdutos.MARTELO, comprarEVender(token, ApoioDeProdutos.MARTELO, "22.00", 4, 20, 5, "36.67"));
		ids.put(ApoioDeProdutos.TRENA, comprarEVender(token, ApoioDeProdutos.TRENA, "12.00", 5, 14, 10, "20.00"));
		ids.put(ApoioDeProdutos.ALICATE, comprarEVender(token, ApoioDeProdutos.ALICATE, "28.00", 4, 16, 4, "46.67"));
		ids.put(ApoioDeProdutos.CAIXA, comprarEVender(token, ApoioDeProdutos.CAIXA, "45.00", 3, 10, 0, null));
		return ids;
	}

	public String comprar(String token, String produtoId, int quantidade, String data) throws Exception {
		return registrarComSucesso(token, movimentacaoJson(produtoId, "ENTRADA", quantidade, null, data));
	}

	public String vender(String token, String produtoId, int quantidade, String valorUnitario, String data)
			throws Exception {
		return registrarComSucesso(token, movimentacaoJson(produtoId, "SAIDA", quantidade, valorUnitario, data));
	}

	public String registrarComSucesso(String token, String json) throws Exception {
		String resposta = registrar(token, json)
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		return JsonPath.read(resposta, "$.id");
	}

	public ResultActions registrar(String token, String json) throws Exception {
		return mockMvc.perform(post(MOVIMENTACOES).header(HttpHeaders.AUTHORIZATION, bearer(token))
			.contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

	public ResultActions listarMovimentacoes(String token) throws Exception {
		return mockMvc.perform(get(MOVIMENTACOES).header(HttpHeaders.AUTHORIZATION, bearer(token)));
	}

	public ResultActions listarMovimentacoesDoProduto(String token, String produtoId) throws Exception {
		return mockMvc.perform(get(MOVIMENTACOES).param("produtoId", produtoId)
			.header(HttpHeaders.AUTHORIZATION, bearer(token)));
	}

	public ResultActions consultarMovimentacao(String token, String id) throws Exception {
		return mockMvc.perform(get(MOVIMENTACOES + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)));
	}

	public ResultActions estoque(String token) throws Exception {
		return mockMvc.perform(get(ESTOQUE).header(HttpHeaders.AUTHORIZATION, bearer(token)));
	}

	/** Fluxo de caixa no período; datas nulas não são enviadas. */
	public ResultActions fluxoDeCaixa(String token, String inicio, String fim) throws Exception {
		MockHttpServletRequestBuilder requisicao = get(FLUXO_DE_CAIXA).header(HttpHeaders.AUTHORIZATION, bearer(token));
		if (inicio != null) {
			requisicao.param("inicio", inicio);
		}
		if (fim != null) {
			requisicao.param("fim", fim);
		}
		return mockMvc.perform(requisicao);
	}

	/** Valores nulos viram {@code null} no JSON; o valor unitário é escrito como número. */
	public static String movimentacaoJson(String produtoId, String tipo, int quantidade, String valorUnitario,
			String data) {
		return """
				{"produtoId": %s, "tipo": "%s", "quantidade": %d, "valorUnitario": %s, "data": %s}
				""".formatted(texto(produtoId), tipo, quantidade, valorUnitario, texto(data));
	}

	private static String texto(String valor) {
		return valor == null ? "null" : "\"" + valor + "\"";
	}

	private String comprarEVender(String token, String nome, String custo, int estoqueMinimo, int compradas,
			int vendidas, String precoDeVenda) throws Exception {
		String id = produtos.cadastrarComSucesso(token, nome, custo, estoqueMinimo);
		comprar(token, id, compradas, "2026-06-20");
		if (vendidas > 0) {
			vender(token, id, vendidas, precoDeVenda, "2026-07-10");
		}
		return id;
	}

}
