package br.com.valorizamei.precificacao;

import static br.com.valorizamei.ApoioDeAutenticacao.bearer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import br.com.valorizamei.despesa.ApoioDeDespesas;
import br.com.valorizamei.produto.ApoioDeProdutos;

/**
 * Monta pela API o estudo de caso completo da loja de ferramentas (6 produtos, 5 despesas,
 * receita 14.000,00 e margem 25%) e consulta a precificação com o token de cada MEI.
 */
final class ApoioDePrecificacao {

	static final String PRECIFICACAO = "/api/v1/precificacao";

	private final MockMvc mockMvc;

	final ApoioDeProdutos produtos;

	final ApoioDeDespesas despesas;

	ApoioDePrecificacao(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
		this.produtos = new ApoioDeProdutos(mockMvc);
		this.despesas = new ApoioDeDespesas(mockMvc);
	}

	/** Cria a "Loja de Ferramentas" com os dados do estudo de caso e devolve o token dela. */
	String lojaDoEstudoDeCaso() throws Exception {
		String token = produtos.tokenDaLoja();
		produtos.cadastrarEstudoDeCaso(token);
		despesas.registrarEstudoDeCaso(token);
		despesas.gravarParametrosComSucesso(token, "14000.00", "25.00");
		return token;
	}

	/** Identificador do produto do MEI com o nome informado, obtido pela listagem de produtos. */
	String idDoProduto(String token, String nome) throws Exception {
		String lista = produtos.listar(token).andReturn().getResponse().getContentAsString();
		Map<String, Object> produto = JsonPath.<java.util.List<Map<String, Object>>>read(lista,
				"$[?(@.nome == '" + nome.replace("'", "\\'") + "')]").get(0);
		return (String) produto.get("id");
	}

	ResultActions consultar(String token) throws Exception {
		return mockMvc.perform(get(PRECIFICACAO).header(HttpHeaders.AUTHORIZATION, bearer(token)));
	}

	ResultActions consultarProduto(String token, String id) throws Exception {
		return mockMvc.perform(get(PRECIFICACAO + "/produtos/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)));
	}

}
