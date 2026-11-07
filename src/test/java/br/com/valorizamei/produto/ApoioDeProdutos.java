package br.com.valorizamei.produto;

import static br.com.valorizamei.ApoioDeAutenticacao.bearer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import br.com.valorizamei.ApoioDeAutenticacao;

/**
 * Contas e produtos do cenário de referência (estudo de caso da loja de ferramentas) e chamadas
 * aos endpoints de produtos com o token de cada MEI.
 */
public final class ApoioDeProdutos {

	public static final String CAMINHO = "/api/v1/produtos";

	public static final String FURADEIRA = "Furadeira de impacto 650 W";

	public static final String JOGO_DE_CHAVES = "Jogo de chaves de fenda (6 peças)";

	public static final String MARTELO = "Martelo unha 27 mm";

	public static final String TRENA = "Trena 5 m";

	public static final String ALICATE = "Alicate universal 8\"";

	public static final String CAIXA = "Caixa de ferramentas plástica";

	public static final String EMAIL_LOJA = "carlos@lojaferramentas.com.br";

	public static final String EMAIL_PAPELARIA = "ana@papelariacentral.com.br";

	private final MockMvc mockMvc;

	public ApoioDeProdutos(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	public String tokenDaLoja() throws Exception {
		return novaConta("Loja de Ferramentas", "Carlos Silva", EMAIL_LOJA);
	}

	public String tokenDaPapelaria() throws Exception {
		return novaConta("Papelaria Central", "Ana Souza", EMAIL_PAPELARIA);
	}

	/** Cadastra os 6 produtos do estudo de caso (custo direto e estoque mínimo da seção 4.1). */
	public void cadastrarEstudoDeCaso(String token) throws Exception {
		cadastrarComSucesso(token, FURADEIRA, "180.00", 3);
		cadastrarComSucesso(token, JOGO_DE_CHAVES, "25.00", 5);
		cadastrarComSucesso(token, MARTELO, "22.00", 4);
		cadastrarComSucesso(token, TRENA, "12.00", 5);
		cadastrarComSucesso(token, ALICATE, "28.00", 4);
		cadastrarComSucesso(token, CAIXA, "45.00", 3);
	}

	/** Cadastra o produto, exige 201 e devolve o identificador gerado. */
	public String cadastrarComSucesso(String token, String nome, String custoDireto, int estoqueMinimo) throws Exception {
		String resposta = cadastrar(token, produtoJson(nome, custoDireto, estoqueMinimo))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		return JsonPath.read(resposta, "$.id");
	}

	public ResultActions cadastrar(String token, String json) throws Exception {
		return mockMvc.perform(post(CAMINHO).header(HttpHeaders.AUTHORIZATION, bearer(token))
			.contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

	public ResultActions listar(String token) throws Exception {
		return mockMvc.perform(get(CAMINHO).header(HttpHeaders.AUTHORIZATION, bearer(token)));
	}

	public ResultActions consultar(String token, String id) throws Exception {
		return mockMvc.perform(get(CAMINHO + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)));
	}

	public ResultActions alterar(String token, String id, String json) throws Exception {
		return mockMvc.perform(put(CAMINHO + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token))
			.contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

	public ResultActions excluir(String token, String id) throws Exception {
		return mockMvc.perform(delete(CAMINHO + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)));
	}

	/** O custo é escrito como número JSON exatamente como informado (ex.: {@code 12.005}). */
	public static String produtoJson(String nome, String custoDireto, int estoqueMinimo) {
		return """
				{"nome": "%s", "custoDireto": %s, "estoqueMinimo": %d}
				""".formatted(nome.replace("\"", "\\\""), custoDireto, estoqueMinimo);
	}

	private String novaConta(String nomeNegocio, String nome, String email) throws Exception {
		ApoioDeAutenticacao autenticacao = new ApoioDeAutenticacao(mockMvc);
		autenticacao.cadastrar(nomeNegocio, nome, email, ApoioDeAutenticacao.SENHA);
		return autenticacao.entrar(email, ApoioDeAutenticacao.SENHA);
	}

}
