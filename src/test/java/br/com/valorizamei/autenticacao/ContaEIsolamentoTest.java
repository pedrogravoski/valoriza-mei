package br.com.valorizamei.autenticacao;

import static br.com.valorizamei.ApoioDeAutenticacao.bearer;
import static br.com.valorizamei.autenticacao.CadastroTest.CARLOS;
import static br.com.valorizamei.autenticacao.CadastroTest.EMAIL_CARLOS;
import static br.com.valorizamei.autenticacao.CadastroTest.LOJA;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import br.com.valorizamei.ApoioDeAutenticacao;
import br.com.valorizamei.TestcontainersConfiguration;
import br.com.valorizamei.shared.seguranca.ClaimsDoToken;

/**
 * Conta autenticada, validação do token e isolamento entre MEIs (specs autenticacao e
 * isolamento-tenant; RF10, RNF07).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ContaEIsolamentoTest {

	private static final String CONTA = "/api/v1/conta";

	private static final String PAPELARIA = "Papelaria Central";

	private static final String EMAIL_ANA = "ana@papelariacentral.com.br";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private JwtEncoder codificadorDeToken;

	private String tokenDaLoja;

	private String tokenDaPapelaria;

	@BeforeEach
	void cadastrarLojaEPapelaria() throws Exception {
		BancoDeContas.limpar(jdbcTemplate);
		ApoioDeAutenticacao apoio = new ApoioDeAutenticacao(mockMvc);
		apoio.cadastrar(LOJA, CARLOS, EMAIL_CARLOS, ApoioDeAutenticacao.SENHA);
		apoio.cadastrar(PAPELARIA, "Ana Souza", EMAIL_ANA, ApoioDeAutenticacao.SENHA);
		tokenDaLoja = apoio.entrar(EMAIL_CARLOS, ApoioDeAutenticacao.SENHA);
		tokenDaPapelaria = apoio.entrar(EMAIL_ANA, ApoioDeAutenticacao.SENHA);
	}

	@Test
	void tokenValidoRetorna200() throws Exception {
		consultarConta(tokenDaLoja)
			.andExpect(status().isOk());
	}

	@Test
	void contaConsultadaAPartirDoToken() throws Exception {
		consultarConta(tokenDaLoja)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nomeNegocio").value(LOJA))
			.andExpect(jsonPath("$.nome").value(CARLOS))
			.andExpect(jsonPath("$.email").value(EMAIL_CARLOS));
	}

	@Test
	void doisMeisConsultamAPropriaConta() throws Exception {
		consultarConta(tokenDaLoja)
			.andExpect(jsonPath("$.nomeNegocio").value(LOJA))
			.andExpect(jsonPath("$.email").value(EMAIL_CARLOS));
		consultarConta(tokenDaPapelaria)
			.andExpect(jsonPath("$.nomeNegocio").value(PAPELARIA))
			.andExpect(jsonPath("$.email").value(EMAIL_ANA));
	}

	@Test
	void tenantInformadoNaQueryEhIgnorado() throws Exception {
		UUID papelaria = tenantDe(EMAIL_ANA);

		mockMvc.perform(get(CONTA).param("tenantId", papelaria.toString())
				.header(HttpHeaders.AUTHORIZATION, bearer(tokenDaLoja)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nomeNegocio").value(LOJA))
			.andExpect(jsonPath("$.email").value(EMAIL_CARLOS));
	}

	@Test
	void tokenAdulteradoRetorna401() throws Exception {
		String adulterado = trocarTenantNoPayload(tokenDaLoja, tenantDe(EMAIL_CARLOS), tenantDe(EMAIL_ANA));

		consultarConta(adulterado)
			.andExpect(status().isUnauthorized());
	}

	@Test
	void tokenExpiradoRetorna401() throws Exception {
		Instant duasHorasAtras = Instant.now().minusSeconds(7200);
		String expirado = tokenAssinado(claims -> claims
			.claim(ClaimsDoToken.TENANT_ID, tenantDe(EMAIL_CARLOS).toString())
			.issuedAt(duasHorasAtras)
			.expiresAt(duasHorasAtras.plusSeconds(3600)));

		consultarConta(expirado)
			.andExpect(status().isUnauthorized());
	}

	@Test
	void tokenSemIdentificacaoDoMeiRetorna401() throws Exception {
		consultarConta(tokenAssinado(claims -> {
		}))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void tokenDeMeiInexistenteRetorna401() throws Exception {
		String meiInexistente = tokenAssinado(claims -> claims
			.claim(ClaimsDoToken.TENANT_ID, UUID.randomUUID().toString()));

		consultarConta(meiInexistente)
			.andExpect(status().isUnauthorized());
	}

	private ResultActions consultarConta(String token) throws Exception {
		return mockMvc.perform(get(CONTA).header(HttpHeaders.AUTHORIZATION, bearer(token)));
	}

	private UUID tenantDe(String email) {
		return jdbcTemplate.queryForObject("select tenant_id from usuario where email = ?", UUID.class, email);
	}

	/**
	 * Token assinado com a chave do sistema, válido por uma hora a partir de agora, ajustado
	 * pelo teste (validade, presença e valor do tenant).
	 */
	private String tokenAssinado(Consumer<JwtClaimsSet.Builder> ajuste) {
		Instant agora = Instant.now();
		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
			.issuer(ClaimsDoToken.EMISSOR)
			.subject(UUID.randomUUID().toString())
			.issuedAt(agora)
			.expiresAt(agora.plusSeconds(3600));
		ajuste.accept(claims);
		JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
		return codificadorDeToken.encode(JwtEncoderParameters.from(cabecalho, claims.build())).getTokenValue();
	}

	/** Altera o conteúdo do token depois da emissão, mantendo a assinatura original. */
	private static String trocarTenantNoPayload(String token, UUID de, UUID para) {
		String[] partes = token.split("\\.");
		Base64.Decoder decodificador = Base64.getUrlDecoder();
		Base64.Encoder codificador = Base64.getUrlEncoder().withoutPadding();
		String payload = new String(decodificador.decode(partes[1]), StandardCharsets.UTF_8)
			.replace(de.toString(), para.toString());
		return partes[0] + "." + codificador.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + partes[2];
	}

}
