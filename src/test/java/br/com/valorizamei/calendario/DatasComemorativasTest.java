package br.com.valorizamei.calendario;

import static br.com.valorizamei.ApoioDeAutenticacao.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import com.jayway.jsonpath.JsonPath;

import br.com.valorizamei.ApoioDeAutenticacao;
import br.com.valorizamei.BancoDeTeste;
import br.com.valorizamei.RelogioFixoConfiguration;
import br.com.valorizamei.TestcontainersConfiguration;

/**
 * Calendário com as datas fixas e as datas personalizadas do MEI (spec datas-comemorativas; RF09).
 * A data atual é 15/07/2026.
 */
@Import({ TestcontainersConfiguration.class, RelogioFixoConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
class DatasComemorativasTest {

	static final String DATAS = "/api/v1/datas-comemorativas";

	private static final String UUID_REGEX = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

	private static final String ANIVERSARIO = "Aniversário da cidade";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private String tokenDaLoja;

	@BeforeEach
	void cadastrarLoja() throws Exception {
		BancoDeTeste.limpar(jdbcTemplate);
		tokenDaLoja = new ApoioDeAutenticacao(mockMvc).tokenDeNovaConta();
	}

	@Test
	void calendarioAPartirDe15DeJulho() throws Exception {
		listar()
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(5)))
			.andExpect(jsonPath("$[*].nome",
					contains("Dia dos Pais", "Dia das Crianças", "Black Friday", "Natal", "Dia das Mães")))
			.andExpect(jsonPath("$[*].proximaOcorrencia",
					contains("2026-08-09", "2026-10-12", "2026-11-27", "2026-12-25", "2027-05-09")))
			.andExpect(jsonPath("$[*].id", everyItem(nullValue())))
			.andExpect(jsonPath("$[*].personalizada", everyItem(org.hamcrest.Matchers.is(false))));
	}

	@Test
	void aniversarioDaCidade() throws Exception {
		cadastrar(dataJson(ANIVERSARIO, 1, 8))
			.andExpect(status().isCreated())
			.andExpect(header().string(HttpHeaders.LOCATION, matchesPattern(".*" + DATAS + "/" + UUID_REGEX)))
			.andExpect(jsonPath("$.id", matchesPattern(UUID_REGEX)))
			.andExpect(jsonPath("$.nome").value(ANIVERSARIO))
			.andExpect(jsonPath("$.dia").value(1))
			.andExpect(jsonPath("$.mes").value(8))
			.andExpect(jsonPath("$.proximaOcorrencia").value("2026-08-01"))
			.andExpect(jsonPath("$.diasRestantes").value(17))
			.andExpect(jsonPath("$.personalizada").value(true));
	}

	@Test
	void proximaOcorrenciaNoAnoSeguinte() throws Exception {
		cadastrar(dataJson("Liquidação de verão", 5, 1))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.proximaOcorrencia").value("2027-01-05"));
	}

	@Test
	void dataQueCaiHoje() throws Exception {
		cadastrar(dataJson("Aniversário da loja", 15, 7))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.proximaOcorrencia").value("2026-07-15"))
			.andExpect(jsonPath("$.diasRestantes").value(0));
	}

	@Test
	void camposObrigatoriosAusentes() throws Exception {
		cadastrar("{}")
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.erros[*].campo", containsInAnyOrder("nome", "dia", "mes")));

		assertThat(contarDatas()).isZero();
	}

	@Test
	void mesInvalido() throws Exception {
		cadastrar(dataJson("Feira do bairro", 10, 13))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros", hasSize(1)))
			.andExpect(jsonPath("$.erros[0].campo").value("mes"));
	}

	@Test
	void diaQueNaoExisteNoMes() throws Exception {
		cadastrar(dataJson("Feira do bairro", 31, 4))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros", hasSize(1)))
			.andExpect(jsonPath("$.erros[0].campo").value("dia"))
			.andExpect(jsonPath("$.erros[0].mensagem").value("o dia não existe em todos os anos neste mês"));

		assertThat(contarDatas()).isZero();
	}

	@Test
	void vinteENoveDeFevereiro() throws Exception {
		cadastrar(dataJson("Feira do bairro", 29, 2))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros[0].campo").value("dia"));
	}

	@Test
	void dataRepetidaComOutraGrafia() throws Exception {
		cadastrar(dataJson(ANIVERSARIO, 1, 8)).andExpect(status().isCreated());

		cadastrar(dataJson("ANIVERSÁRIO DA CIDADE", 2, 8))
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Data comemorativa já cadastrada"));

		assertThat(contarDatas()).isEqualTo(1);
	}

	@Test
	void calendarioComDataPersonalizada() throws Exception {
		cadastrar(dataJson(ANIVERSARIO, 1, 8)).andExpect(status().isCreated());

		listar()
			.andExpect(jsonPath("$", hasSize(6)))
			.andExpect(jsonPath("$[0].nome").value(ANIVERSARIO))
			.andExpect(jsonPath("$[0].proximaOcorrencia").value("2026-08-01"))
			.andExpect(jsonPath("$[1].nome").value("Dia dos Pais"));
	}

	@Test
	void mudancaDoDiaDoAniversarioDaCidade() throws Exception {
		String id = idDe(cadastrar(dataJson(ANIVERSARIO, 1, 8)));

		alterar(id, dataJson(ANIVERSARIO, 5, 8))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.dia").value(5))
			.andExpect(jsonPath("$.proximaOcorrencia").value("2026-08-05"));

		consultar(id)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.dia").value(5));
	}

	@Test
	void exclusaoDeDataPersonalizada() throws Exception {
		String id = idDe(cadastrar(dataJson(ANIVERSARIO, 1, 8)));

		excluir(id)
			.andExpect(status().isNoContent());

		listar()
			.andExpect(jsonPath("$[*].nome", not(hasItem(ANIVERSARIO))));
		mockMvc.perform(get("/api/v1/alertas").header(HttpHeaders.AUTHORIZATION, bearer(tokenDaLoja)))
			.andExpect(jsonPath("$.datasComemorativas[*].nome", contains("Dia dos Pais")));
	}

	@Test
	void dataPersonalizadaInexistente() throws Exception {
		String inexistente = UUID.randomUUID().toString();

		consultar(inexistente)
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Data comemorativa não encontrada"));
		alterar(inexistente, dataJson(ANIVERSARIO, 1, 8))
			.andExpect(status().isNotFound());
		excluir(inexistente)
			.andExpect(status().isNotFound());
	}

	private ResultActions listar() throws Exception {
		return mockMvc.perform(get(DATAS).header(HttpHeaders.AUTHORIZATION, bearer(tokenDaLoja)));
	}

	private ResultActions cadastrar(String json) throws Exception {
		return mockMvc.perform(post(DATAS).header(HttpHeaders.AUTHORIZATION, bearer(tokenDaLoja))
			.contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

	private ResultActions consultar(String id) throws Exception {
		return mockMvc.perform(get(DATAS + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer(tokenDaLoja)));
	}

	private ResultActions alterar(String id, String json) throws Exception {
		return mockMvc.perform(put(DATAS + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer(tokenDaLoja))
			.contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

	private ResultActions excluir(String id) throws Exception {
		return mockMvc.perform(delete(DATAS + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer(tokenDaLoja)));
	}

	private static String idDe(ResultActions cadastro) throws Exception {
		return JsonPath.read(cadastro.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(),
				"$.id");
	}

	static String dataJson(String nome, int dia, int mes) {
		return """
				{"nome": "%s", "dia": %d, "mes": %d}
				""".formatted(nome, dia, mes);
	}

	private int contarDatas() {
		return jdbcTemplate.queryForObject("select count(*) from data_comemorativa", Integer.class);
	}

}
