package br.com.valorizamei.alerta;

import static br.com.valorizamei.ApoioDeAutenticacao.bearer;
import static br.com.valorizamei.produto.ApoioDeProdutos.TRENA;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import com.jayway.jsonpath.JsonPath;

import br.com.valorizamei.BancoDeTeste;
import br.com.valorizamei.RelogioFixoConfiguration;
import br.com.valorizamei.TestcontainersConfiguration;
import br.com.valorizamei.estoque.ApoioDeEstoque;

/**
 * Isolamento dos alertas e das datas personalizadas entre MEIs (specs alertas e
 * datas-comemorativas; RF10, RNF07). A data atual é 15/07/2026.
 */
@Import({ TestcontainersConfiguration.class, RelogioFixoConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
class AlertaIsolamentoTest {

	private static final String DATAS = "/api/v1/datas-comemorativas";

	private static final String CADERNO = "Caderno universitário 200 folhas";

	private static final String VOLTA_AS_AULAS = "Volta às aulas";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private String tokenDaLoja;

	private String tokenDaPapelaria;

	private String voltaAsAulas;

	@BeforeEach
	void cadastrarLojaEPapelaria() throws Exception {
		BancoDeTeste.limpar(jdbcTemplate);
		ApoioDeEstoque estoque = new ApoioDeEstoque(mockMvc);
		tokenDaLoja = estoque.produtos.tokenDaLoja();
		estoque.estoqueDoEstudoDeCaso(tokenDaLoja);
		tokenDaPapelaria = estoque.produtos.tokenDaPapelaria();
		String caderno = estoque.produtos.cadastrarComSucesso(tokenDaPapelaria, CADERNO, "16.00", 10);
		estoque.comprar(tokenDaPapelaria, caderno, 2, null);
		String resposta = mockMvc.perform(post(DATAS).header(HttpHeaders.AUTHORIZATION, bearer(tokenDaPapelaria))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nome": "Volta às aulas", "dia": 1, "mes": 8}
						"""))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		voltaAsAulas = JsonPath.read(resposta, "$.id");
	}

	@Test
	void cadaMeiComOsPropriosAlertas() throws Exception {
		mockMvc.perform(get(AlertasTest.ALERTAS).header(HttpHeaders.AUTHORIZATION, bearer(tokenDaLoja)))
			.andExpect(jsonPath("$.ruptura[*].nome", contains(TRENA)))
			.andExpect(jsonPath("$.datasComemorativas[*].nome", contains("Dia dos Pais")))
			.andExpect(jsonPath("$.sugestoes[*].nome", not(hasItem(CADERNO))));

		mockMvc.perform(get(AlertasTest.ALERTAS).header(HttpHeaders.AUTHORIZATION, bearer(tokenDaPapelaria)))
			.andExpect(jsonPath("$.ruptura[*].nome", contains(CADERNO)))
			.andExpect(jsonPath("$.datasComemorativas[*].nome", contains(VOLTA_AS_AULAS, "Dia dos Pais")))
			.andExpect(jsonPath("$.sugestoes[*].nome", contains(CADERNO)))
			.andExpect(jsonPath("$.sugestoes[0].tipo").value("PROMOCAO"));
	}

	@Test
	void dataDeOutroMei() throws Exception {
		mockMvc.perform(get(DATAS).header(HttpHeaders.AUTHORIZATION, bearer(tokenDaLoja)))
			.andExpect(jsonPath("$[*].nome", not(hasItem(VOLTA_AS_AULAS))));

		mockMvc.perform(get(DATAS + "/" + voltaAsAulas).header(HttpHeaders.AUTHORIZATION, bearer(tokenDaLoja)))
			.andExpect(status().isNotFound());
		mockMvc.perform(put(DATAS + "/" + voltaAsAulas).header(HttpHeaders.AUTHORIZATION, bearer(tokenDaLoja))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nome": "Outra data", "dia": 2, "mes": 8}
						"""))
			.andExpect(status().isNotFound());
		mockMvc.perform(delete(DATAS + "/" + voltaAsAulas).header(HttpHeaders.AUTHORIZATION, bearer(tokenDaLoja)))
			.andExpect(status().isNotFound());

		mockMvc.perform(get(DATAS + "/" + voltaAsAulas).header(HttpHeaders.AUTHORIZATION, bearer(tokenDaPapelaria)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nome").value(VOLTA_AS_AULAS))
			.andExpect(jsonPath("$.dia").value(1));
	}

	@Test
	void alertasSemToken() throws Exception {
		mockMvc.perform(get(AlertasTest.ALERTAS))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
	}

	@Test
	void calendarioSemToken() throws Exception {
		mockMvc.perform(get(DATAS))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
	}

}
