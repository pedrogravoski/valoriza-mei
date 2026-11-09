package br.com.valorizamei.shared.seguranca;

import static br.com.valorizamei.ApoioDeAutenticacao.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

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

import br.com.valorizamei.ApoioDeAutenticacao;
import br.com.valorizamei.TestcontainersConfiguration;

/**
 * Acesso do frontend em outra origem (spec cors; RF10, RF11), com a origem padrão de
 * desenvolvimento {@code http://localhost:4200}.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CorsTest {

	static final String FRONTEND = "http://localhost:4200";

	private static final String DESCONHECIDA = "http://site-desconhecido.com";

	private static final String PRODUTOS = "/api/v1/produtos";

	private static final String PRODUTO = """
			{"nome": "Produto %s", "custoDireto": 10.00, "estoqueMinimo": 1}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private ApoioDeAutenticacao autenticacao;

	@BeforeEach
	void criarApoio() {
		autenticacao = new ApoioDeAutenticacao(mockMvc);
	}

	@Test
	void preChecagemDoCadastroDeProduto() throws Exception {
		preChecagem(FRONTEND)
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND))
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")))
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("authorization")))
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("content-type")))
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"))
			.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
	}

	@Test
	void consultaDaContaPeloFrontend() throws Exception {
		String token = autenticacao.tokenDeNovaConta();

		mockMvc.perform(get("/api/v1/conta").header(HttpHeaders.ORIGIN, FRONTEND)
				.header(HttpHeaders.AUTHORIZATION, bearer(token)))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND));
	}

	@Test
	void tokenAusenteLidoPeloFrontend() throws Exception {
		mockMvc.perform(get("/api/v1/conta").header(HttpHeaders.ORIGIN, FRONTEND))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Não autenticado"))
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND));
	}

	@Test
	void locationDoCadastroVisivelAoFrontend() throws Exception {
		String token = autenticacao.tokenDeNovaConta();

		mockMvc.perform(post(PRODUTOS).header(HttpHeaders.ORIGIN, FRONTEND)
				.header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content(PRODUTO.formatted(UUID.randomUUID())))
			.andExpect(status().isCreated())
			.andExpect(header().exists(HttpHeaders.LOCATION))
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND))
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, containsString("Location")));
	}

	@Test
	void preChecagemDeOrigemDesconhecida() throws Exception {
		preChecagem(DESCONHECIDA)
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(403))
			.andExpect(jsonPath("$.title").value("Origem não permitida"))
			.andExpect(jsonPath("$.detail").value("A origem da requisição não tem permissão para acessar a API."))
			.andExpect(jsonPath("$.instance").value(PRODUTOS))
			.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	void cadastroAPartirDeOrigemDesconhecida() throws Exception {
		String token = autenticacao.tokenDeNovaConta();
		String nome = UUID.randomUUID().toString();

		mockMvc.perform(post(PRODUTOS).header(HttpHeaders.ORIGIN, DESCONHECIDA)
				.header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content(PRODUTO.formatted(nome)))
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Origem não permitida"))
			.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));

		Integer gravados = jdbcTemplate.queryForObject("select count(*) from produto where nome = ?", Integer.class,
				"Produto " + nome);
		assertThat(gravados).isZero();
	}

	@Test
	void origemPadraoDeDesenvolvimento() throws Exception {
		preChecagem(FRONTEND).andExpect(status().isOk());
		preChecagem("http://localhost:4300").andExpect(status().isForbidden());
	}

	@Test
	void clienteSemOrigin() throws Exception {
		String token = autenticacao.tokenDeNovaConta();

		mockMvc.perform(get("/api/v1/conta").header(HttpHeaders.AUTHORIZATION, bearer(token)))
			.andExpect(status().isOk())
			.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	void swaggerUiNaMesmaOrigem() throws Exception {
		String email = "mesma-origem-" + UUID.randomUUID() + "@exemplo.com";
		autenticacao.cadastrar("Loja de Ferramentas", "Carlos Silva", email, ApoioDeAutenticacao.SENHA);

		mockMvc.perform(post("/api/v1/auth/login").header(HttpHeaders.ORIGIN, "http://localhost")
				.contentType(MediaType.APPLICATION_JSON)
				.content(ApoioDeAutenticacao.loginJson(email, ApoioDeAutenticacao.SENHA)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.token").exists());
	}

	private ResultActions preChecagem(String origem) throws Exception {
		return mockMvc.perform(options(PRODUTOS).header(HttpHeaders.ORIGIN, origem)
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"));
	}

}
