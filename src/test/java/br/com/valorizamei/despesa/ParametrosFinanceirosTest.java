package br.com.valorizamei.despesa;

import static br.com.valorizamei.despesa.ApoioDeDespesas.parametrosJson;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
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
import org.springframework.test.web.servlet.MockMvc;

import br.com.valorizamei.BancoDeTeste;
import br.com.valorizamei.TestcontainersConfiguration;

/**
 * Receita mensal estimada e margem de lucro do MEI (spec parametros-financeiros; RF03, RF05).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ParametrosFinanceirosTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private ApoioDeDespesas apoio;

	private String tokenDaLoja;

	@BeforeEach
	void cadastrarLoja() throws Exception {
		BancoDeTeste.limpar(jdbcTemplate);
		apoio = new ApoioDeDespesas(mockMvc);
		tokenDaLoja = apoio.tokenDaLoja();
	}

	@Test
	void parametrosDaLojaDeFerramentas() throws Exception {
		apoio.gravarParametros(tokenDaLoja, parametrosJson("14000.00", "25.00"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.receitaMensalEstimada").value(14000.00))
			.andExpect(jsonPath("$.margemDeLucro").value(25.00))
			.andExpect(content().string(containsString("\"receitaMensalEstimada\":14000.00")))
			.andExpect(content().string(containsString("\"margemDeLucro\":25.00")));
	}

	@Test
	void valoresSemCasasDecimaisSaoDevolvidosComDuasCasas() throws Exception {
		apoio.gravarParametros(tokenDaLoja, parametrosJson("14000", "25"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("\"receitaMensalEstimada\":14000.00")))
			.andExpect(content().string(containsString("\"margemDeLucro\":25.00")));
	}

	@Test
	void atualizacaoDaMargemDeLucro() throws Exception {
		apoio.gravarParametrosComSucesso(tokenDaLoja, "14000.00", "25.00");

		apoio.gravarParametros(tokenDaLoja, parametrosJson("14000.00", "30.00"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.margemDeLucro").value(30.00));

		apoio.consultarParametros(tokenDaLoja)
			.andExpect(jsonPath("$.receitaMensalEstimada").value(14000.00))
			.andExpect(jsonPath("$.margemDeLucro").value(30.00));
	}

	@Test
	void consultaAposInformarOsParametros() throws Exception {
		apoio.gravarParametrosComSucesso(tokenDaLoja, "14000.00", "25.00");

		apoio.consultarParametros(tokenDaLoja)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.receitaMensalEstimada").value(14000.00))
			.andExpect(jsonPath("$.margemDeLucro").value(25.00));
	}

	@Test
	void parametrosAindaNaoInformados() throws Exception {
		apoio.consultarParametros(tokenDaLoja)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.receitaMensalEstimada").isEmpty())
			.andExpect(jsonPath("$.margemDeLucro").isEmpty());
	}

	@Test
	void camposObrigatoriosAusentes() throws Exception {
		apoio.gravarParametros(tokenDaLoja, "{}")
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.erros[*].campo", containsInAnyOrder("receitaMensalEstimada", "margemDeLucro")));
	}

	@Test
	void receitaZero() throws Exception {
		apoio.gravarParametros(tokenDaLoja, parametrosJson("0.00", "25.00"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros", hasSize(1)))
			.andExpect(jsonPath("$.erros[0].campo").value("receitaMensalEstimada"));
	}

	@Test
	void margemDeCemOuMais() throws Exception {
		apoio.gravarParametrosComSucesso(tokenDaLoja, "14000.00", "25.00");

		apoio.gravarParametros(tokenDaLoja, parametrosJson("14000.00", "100.00"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros", hasSize(1)))
			.andExpect(jsonPath("$.erros[0].campo").value("margemDeLucro"));

		apoio.consultarParametros(tokenDaLoja)
			.andExpect(jsonPath("$.margemDeLucro").value(25.00));
	}

	@Test
	void margemComMaisDeDuasCasasDecimais() throws Exception {
		apoio.gravarParametros(tokenDaLoja, parametrosJson("14000.00", "25.005"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros[0].campo").value("margemDeLucro"))
			.andExpect(jsonPath("$.erros[0].mensagem").value("informe o valor com no máximo 2 casas decimais"));
	}

	@Test
	void margemZeroEhAceita() throws Exception {
		apoio.gravarParametros(tokenDaLoja, parametrosJson("14000.00", "0.00"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("\"margemDeLucro\":0.00")));
	}

}
