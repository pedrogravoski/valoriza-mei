package br.com.valorizamei.despesa;

import static br.com.valorizamei.despesa.ApoioDeDespesas.ALUGUEL;
import static br.com.valorizamei.despesa.ApoioDeDespesas.fixaJson;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import br.com.valorizamei.BancoDeTeste;
import br.com.valorizamei.TestcontainersConfiguration;

/**
 * Rateio das despesas fixas sobre a receita mensal estimada (spec despesas; RF05).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class RateioTest {

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
	void rateioDaLojaDeFerramentas() throws Exception {
		apoio.registrarEstudoDeCaso(tokenDaLoja);
		apoio.gravarParametrosComSucesso(tokenDaLoja, "14000.00", "25.00");

		apoio.rateio(tokenDaLoja)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalDespesasFixas").value(1400.00))
			.andExpect(jsonPath("$.receitaMensalEstimada").value(14000.00))
			.andExpect(jsonPath("$.percentualDespesasFixas").value(10.00))
			.andExpect(jsonPath("$.percentualDespesasVariaveis").value(5.00))
			.andExpect(content().string(containsString("\"percentualDespesasFixas\":10.00")))
			.andExpect(content().string(containsString("\"percentualDespesasVariaveis\":5.00")));
	}

	@Test
	void dfArredondadoParaExibicao() throws Exception {
		apoio.registrarComSucesso(tokenDaLoja, fixaJson(ALUGUEL, "1000.00"));
		apoio.gravarParametrosComSucesso(tokenDaLoja, "3000.00", "25.00");

		apoio.rateio(tokenDaLoja)
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("\"percentualDespesasFixas\":33.33")));
	}

	@Test
	void dfArredondadoMeioParaCima() throws Exception {
		apoio.registrarComSucesso(tokenDaLoja, fixaJson(ALUGUEL, "2000.00"));
		apoio.gravarParametrosComSucesso(tokenDaLoja, "3000.00", "25.00");

		apoio.rateio(tokenDaLoja)
			.andExpect(content().string(containsString("\"percentualDespesasFixas\":66.67")));
	}

	@Test
	void rateioSemReceitaInformada() throws Exception {
		apoio.registrarEstudoDeCaso(tokenDaLoja);

		apoio.rateio(tokenDaLoja)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalDespesasFixas").value(1400.00))
			.andExpect(jsonPath("$.percentualDespesasVariaveis").value(5.00))
			.andExpect(jsonPath("$.receitaMensalEstimada").isEmpty())
			.andExpect(jsonPath("$.percentualDespesasFixas").isEmpty());
	}

	@Test
	void rateioSemDespesas() throws Exception {
		apoio.gravarParametrosComSucesso(tokenDaLoja, "14000.00", "25.00");

		apoio.rateio(tokenDaLoja)
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("\"totalDespesasFixas\":0.00")))
			.andExpect(content().string(containsString("\"percentualDespesasFixas\":0.00")))
			.andExpect(content().string(containsString("\"percentualDespesasVariaveis\":0.00")));
	}

}
