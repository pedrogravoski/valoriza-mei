package br.com.valorizamei;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

/**
 * Cria contas e obtém tokens pelos endpoints reais de cadastro e login, para os testes que
 * precisam chamar endpoints protegidos.
 */
public final class ApoioDeAutenticacao {

	public static final String SENHA = "Senha@2026";

	private final MockMvc mockMvc;

	public ApoioDeAutenticacao(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	/** Cria uma conta com e-mail único e devolve o token dela. */
	public String tokenDeNovaConta() throws Exception {
		String email = "mei-" + UUID.randomUUID() + "@exemplo.com";
		cadastrar("Loja de Ferramentas", "Carlos Silva", email, SENHA);
		return entrar(email, SENHA);
	}

	public void cadastrar(String nomeNegocio, String nome, String email, String senha) throws Exception {
		mockMvc.perform(post("/api/v1/auth/cadastro")
				.contentType(MediaType.APPLICATION_JSON)
				.content(cadastroJson(nomeNegocio, nome, email, senha)))
			.andExpect(status().isCreated());
	}

	public String entrar(String email, String senha) throws Exception {
		String resposta = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginJson(email, senha)))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		return JsonPath.read(resposta, "$.token");
	}

	public static String bearer(String token) {
		return "Bearer " + token;
	}

	public static String cadastroJson(String nomeNegocio, String nome, String email, String senha) {
		return """
				{"nomeNegocio": "%s", "nome": "%s", "email": "%s", "senha": "%s"}
				""".formatted(nomeNegocio, nome, email, senha);
	}

	public static String loginJson(String email, String senha) {
		return """
				{"email": "%s", "senha": "%s"}
				""".formatted(email, senha);
	}

}
