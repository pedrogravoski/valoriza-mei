package br.com.valorizamei.autenticacao;

import static br.com.valorizamei.ApoioDeAutenticacao.cadastroJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import br.com.valorizamei.ApoioDeAutenticacao;
import br.com.valorizamei.TestcontainersConfiguration;

/**
 * Cadastro de conta (spec autenticacao; RF10, RNF01).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CadastroTest {

	static final String LOJA = "Loja de Ferramentas";

	static final String CARLOS = "Carlos Silva";

	static final String EMAIL_CARLOS = "carlos@lojaferramentas.com.br";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PasswordEncoder codificadorDeSenha;

	@BeforeEach
	void limparContas() {
		BancoDeContas.limpar(jdbcTemplate);
	}

	@Test
	void cadastroBemSucedidoDaLojaDeFerramentas() throws Exception {
		cadastrar(cadastroJson(LOJA, CARLOS, EMAIL_CARLOS, ApoioDeAutenticacao.SENHA))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.nomeNegocio").value(LOJA))
			.andExpect(jsonPath("$.nome").value(CARLOS))
			.andExpect(jsonPath("$.email").value(EMAIL_CARLOS))
			.andExpect(jsonPath("$.senha").doesNotExist())
			.andExpect(jsonPath("$.senhaHash").doesNotExist())
			.andExpect(content().string(not(containsString(ApoioDeAutenticacao.SENHA))));
	}

	@Test
	void senhaCurtaRetorna400() throws Exception {
		cadastrar(cadastroJson(LOJA, CARLOS, EMAIL_CARLOS, "1234567"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros", hasSize(1)))
			.andExpect(jsonPath("$.erros[0].campo").value("senha"));
	}

	@Test
	void emailInvalidoENomeDoNegocioAusenteRetornam400() throws Exception {
		String semNomeDoNegocio = """
				{"nome": "Carlos Silva", "email": "carlos-sem-arroba", "senha": "Senha@2026"}
				""";

		cadastrar(semNomeDoNegocio)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros[*].campo", containsInAnyOrder("email", "nomeNegocio")));
	}

	@Test
	void emailJaCadastradoComOutraGrafiaRetorna409() throws Exception {
		cadastrar(cadastroJson(LOJA, CARLOS, EMAIL_CARLOS, ApoioDeAutenticacao.SENHA))
			.andExpect(status().isCreated());
		int meis = contar("mei");
		int usuarios = contar("usuario");

		cadastrar(cadastroJson("Outra Loja", "Carlos S.", " Carlos@LojaFerramentas.com.br ", "OutraSenha1"))
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.title").value("E-mail já cadastrado"))
			.andExpect(jsonPath("$.detail").value("Já existe uma conta com este e-mail."));

		assertThat(contar("mei")).isEqualTo(meis);
		assertThat(contar("usuario")).isEqualTo(usuarios);
	}

	@Test
	void senhaGravadaComoHashBcrypt() throws Exception {
		cadastrar(cadastroJson(LOJA, CARLOS, EMAIL_CARLOS, ApoioDeAutenticacao.SENHA))
			.andExpect(status().isCreated());

		String hash = jdbcTemplate.queryForObject("select senha_hash from usuario where email = ?", String.class,
				EMAIL_CARLOS);

		assertThat(hash).isNotEqualTo(ApoioDeAutenticacao.SENHA).matches("^\\$2[aby]\\$.+");
		assertThat(codificadorDeSenha.matches(ApoioDeAutenticacao.SENHA, hash)).isTrue();
	}

	private ResultActions cadastrar(String json) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private int contar(String tabela) {
		return jdbcTemplate.queryForObject("select count(*) from " + tabela, Integer.class);
	}

}
