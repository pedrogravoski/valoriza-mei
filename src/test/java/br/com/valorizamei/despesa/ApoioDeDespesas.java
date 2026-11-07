package br.com.valorizamei.despesa;

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
 * Contas, despesas e parâmetros do cenário de referência (estudo de caso da loja de ferramentas)
 * e chamadas aos endpoints de despesas e parâmetros financeiros com o token de cada MEI.
 */
public final class ApoioDeDespesas {

	public static final String DESPESAS = "/api/v1/despesas";

	public static final String RATEIO = DESPESAS + "/rateio";

	public static final String PARAMETROS = "/api/v1/parametros-financeiros";

	public static final String ALUGUEL = "Aluguel";

	public static final String INTERNET = "Internet e telefone";

	public static final String HONORARIOS = "Honorários contábeis";

	public static final String TAXA_DE_CARTAO = "Taxa de cartão";

	public static final String ENTREGA = "Custo de entrega";

	public static final String EMAIL_LOJA = "carlos@lojaferramentas.com.br";

	public static final String EMAIL_PAPELARIA = "ana@papelariacentral.com.br";

	private final MockMvc mockMvc;

	public ApoioDeDespesas(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	public String tokenDaLoja() throws Exception {
		return novaConta("Loja de Ferramentas", "Carlos Silva", EMAIL_LOJA);
	}

	public String tokenDaPapelaria() throws Exception {
		return novaConta("Papelaria Central", "Ana Souza", EMAIL_PAPELARIA);
	}

	/** Registra as 3 despesas fixas (total 1.400,00) e as 2 variáveis (5%) do estudo de caso. */
	public void registrarEstudoDeCaso(String token) throws Exception {
		registrarComSucesso(token, fixaJson(ALUGUEL, "1200.00"));
		registrarComSucesso(token, fixaJson(INTERNET, "120.00"));
		registrarComSucesso(token, fixaJson(HONORARIOS, "80.00"));
		registrarComSucesso(token, variavelJson(TAXA_DE_CARTAO, "3.00"));
		registrarComSucesso(token, variavelJson(ENTREGA, "2.00"));
	}

	/** Registra a despesa, exige 201 e devolve o identificador gerado. */
	public String registrarComSucesso(String token, String json) throws Exception {
		String resposta = registrar(token, json)
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		return JsonPath.read(resposta, "$.id");
	}

	public ResultActions registrar(String token, String json) throws Exception {
		return mockMvc.perform(post(DESPESAS).header(HttpHeaders.AUTHORIZATION, bearer(token))
			.contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

	public ResultActions listar(String token) throws Exception {
		return mockMvc.perform(get(DESPESAS).header(HttpHeaders.AUTHORIZATION, bearer(token)));
	}

	public ResultActions consultar(String token, String id) throws Exception {
		return mockMvc.perform(get(DESPESAS + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)));
	}

	public ResultActions alterar(String token, String id, String json) throws Exception {
		return mockMvc.perform(put(DESPESAS + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token))
			.contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

	public ResultActions excluir(String token, String id) throws Exception {
		return mockMvc.perform(delete(DESPESAS + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)));
	}

	public ResultActions rateio(String token) throws Exception {
		return mockMvc.perform(get(RATEIO).header(HttpHeaders.AUTHORIZATION, bearer(token)));
	}

	public ResultActions consultarParametros(String token) throws Exception {
		return mockMvc.perform(get(PARAMETROS).header(HttpHeaders.AUTHORIZATION, bearer(token)));
	}

	public ResultActions gravarParametros(String token, String json) throws Exception {
		return mockMvc.perform(put(PARAMETROS).header(HttpHeaders.AUTHORIZATION, bearer(token))
			.contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

	public void gravarParametrosComSucesso(String token, String receita, String margem) throws Exception {
		gravarParametros(token, parametrosJson(receita, margem)).andExpect(status().isOk());
	}

	public static String fixaJson(String descricao, String valorMensal) {
		return despesaJson(descricao, "FIXA", valorMensal, null);
	}

	public static String variavelJson(String descricao, String percentual) {
		return despesaJson(descricao, "VARIAVEL", null, percentual);
	}

	/** Os valores são escritos como números JSON exatamente como informados; nulo vira {@code null}. */
	public static String despesaJson(String descricao, String tipo, String valorMensal, String percentual) {
		return """
				{"descricao": "%s", "tipo": "%s", "valorMensal": %s, "percentual": %s}
				""".formatted(descricao, tipo, valorMensal, percentual);
	}

	public static String parametrosJson(String receita, String margem) {
		return """
				{"receitaMensalEstimada": %s, "margemDeLucro": %s}
				""".formatted(receita, margem);
	}

	private String novaConta(String nomeNegocio, String nome, String email) throws Exception {
		ApoioDeAutenticacao autenticacao = new ApoioDeAutenticacao(mockMvc);
		autenticacao.cadastrar(nomeNegocio, nome, email, ApoioDeAutenticacao.SENHA);
		return autenticacao.entrar(email, ApoioDeAutenticacao.SENHA);
	}

}
