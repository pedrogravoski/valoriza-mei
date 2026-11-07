package br.com.valorizamei.autenticacao;

import static br.com.valorizamei.ApoioDeAutenticacao.cadastroJson;
import static br.com.valorizamei.ApoioDeAutenticacao.loginJson;
import static br.com.valorizamei.autenticacao.CadastroTest.CARLOS;
import static br.com.valorizamei.autenticacao.CadastroTest.EMAIL_CARLOS;
import static br.com.valorizamei.autenticacao.CadastroTest.LOJA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import br.com.valorizamei.ApoioDeAutenticacao;
import br.com.valorizamei.BancoDeTeste;
import br.com.valorizamei.TestcontainersConfiguration;

/**
 * Login e emissão do token (spec autenticacao; RF10).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class LoginTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cadastrarCarlos() throws Exception {
		BancoDeTeste.limpar(jdbcTemplate);
		new ApoioDeAutenticacao(mockMvc).cadastrar(LOJA, CARLOS, EMAIL_CARLOS, ApoioDeAutenticacao.SENHA);
	}

	@Test
	void loginBemSucedido() throws Exception {
		entrar(EMAIL_CARLOS, ApoioDeAutenticacao.SENHA)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.token", not(emptyOrNullString())))
			.andExpect(jsonPath("$.tipo").value("Bearer"))
			.andExpect(jsonPath("$.expiraEmSegundos", greaterThan(0)));
	}

	@Test
	void emailComOutraGrafiaNoLogin() throws Exception {
		entrar("CARLOS@lojaferramentas.com.br", ApoioDeAutenticacao.SENHA)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.token", not(emptyOrNullString())));
	}

	@Test
	void senhaIncorretaRetorna401SemToken() throws Exception {
		entrar(EMAIL_CARLOS, "SenhaErrada1")
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.title").value("Credenciais inválidas"))
			.andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."))
			.andExpect(jsonPath("$.token").doesNotExist());
	}

	@Test
	void emailNaoCadastradoTemAMesmaRespostaDaSenhaIncorreta() throws Exception {
		MvcResult senhaIncorreta = entrar(EMAIL_CARLOS, "SenhaErrada1").andReturn();
		MvcResult emailInexistente = entrar("naoexiste@exemplo.com", "QualquerSenha1")
			.andExpect(status().isUnauthorized())
			.andReturn();

		assertThat(campo(emailInexistente, "$.title")).isEqualTo(campo(senhaIncorreta, "$.title"));
		assertThat(campo(emailInexistente, "$.detail")).isEqualTo(campo(senhaIncorreta, "$.detail"));
	}

	@Test
	void cadastroELoginSemToken() throws Exception {
		String email = "ana@papelariacentral.com.br";

		mockMvc.perform(post("/api/v1/auth/cadastro")
				.contentType(MediaType.APPLICATION_JSON)
				.content(cadastroJson("Papelaria Central", "Ana Souza", email, ApoioDeAutenticacao.SENHA)))
			.andExpect(status().isCreated())
			.andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
		entrar(email, ApoioDeAutenticacao.SENHA)
			.andExpect(status().isOk());
	}

	private ResultActions entrar(String email, String senha) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/login")
			.contentType(MediaType.APPLICATION_JSON)
			.content(loginJson(email, senha)));
	}

	private static String campo(MvcResult resultado, String caminho) throws Exception {
		return JsonPath.read(resultado.getResponse().getContentAsString(), caminho);
	}

}
