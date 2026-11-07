package br.com.valorizamei.despesa;

import static br.com.valorizamei.despesa.ApoioDeDespesas.ALUGUEL;
import static br.com.valorizamei.despesa.ApoioDeDespesas.DESPESAS;
import static br.com.valorizamei.despesa.ApoioDeDespesas.EMAIL_LOJA;
import static br.com.valorizamei.despesa.ApoioDeDespesas.PARAMETROS;
import static br.com.valorizamei.despesa.ApoioDeDespesas.fixaJson;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import br.com.valorizamei.BancoDeTeste;
import br.com.valorizamei.TestcontainersConfiguration;

/**
 * Isolamento das despesas, do rateio e dos parâmetros financeiros entre MEIs (specs despesas e
 * parametros-financeiros; RF10, RNF07).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class DespesaIsolamentoTest {

	private static final String CONTA_DE_LUZ = "Conta de luz";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private ApoioDeDespesas apoio;

	private String tokenDaLoja;

	private String tokenDaPapelaria;

	@BeforeEach
	void cadastrarLojaEPapelaria() throws Exception {
		BancoDeTeste.limpar(jdbcTemplate);
		apoio = new ApoioDeDespesas(mockMvc);
		tokenDaLoja = apoio.tokenDaLoja();
		tokenDaPapelaria = apoio.tokenDaPapelaria();
	}

	@Test
	void cadaMeiListaSomenteAsPropriasDespesas() throws Exception {
		apoio.registrarComSucesso(tokenDaLoja, fixaJson(ALUGUEL, "1200.00"));
		apoio.registrarComSucesso(tokenDaPapelaria, fixaJson(CONTA_DE_LUZ, "300.00"));

		apoio.listar(tokenDaLoja)
			.andExpect(jsonPath("$[*].descricao", contains(ALUGUEL)));
		apoio.listar(tokenDaPapelaria)
			.andExpect(jsonPath("$[*].descricao", contains(CONTA_DE_LUZ)));
	}

	@Test
	void despesaDeOutroMei() throws Exception {
		String aluguel = apoio.registrarComSucesso(tokenDaLoja, fixaJson(ALUGUEL, "1200.00"));

		apoio.consultar(tokenDaPapelaria, aluguel)
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Despesa não encontrada"));
		apoio.alterar(tokenDaPapelaria, aluguel, fixaJson(CONTA_DE_LUZ, "1.00"))
			.andExpect(status().isNotFound());
		apoio.excluir(tokenDaPapelaria, aluguel)
			.andExpect(status().isNotFound());

		apoio.consultar(tokenDaLoja, aluguel)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.descricao").value(ALUGUEL))
			.andExpect(jsonPath("$.valorMensal").value(1200.00));
	}

	@Test
	void rateioConsideraSoOProprioMei() throws Exception {
		apoio.registrarEstudoDeCaso(tokenDaLoja);
		apoio.gravarParametrosComSucesso(tokenDaLoja, "14000.00", "25.00");
		apoio.registrarComSucesso(tokenDaPapelaria, fixaJson(CONTA_DE_LUZ, "300.00"));
		apoio.gravarParametrosComSucesso(tokenDaPapelaria, "6000.00", "20.00");

		apoio.rateio(tokenDaLoja)
			.andExpect(jsonPath("$.totalDespesasFixas").value(1400.00))
			.andExpect(jsonPath("$.percentualDespesasFixas").value(10.00))
			.andExpect(jsonPath("$.percentualDespesasVariaveis").value(5.00));
		apoio.rateio(tokenDaPapelaria)
			.andExpect(jsonPath("$.totalDespesasFixas").value(300.00))
			.andExpect(jsonPath("$.receitaMensalEstimada").value(6000.00))
			.andExpect(jsonPath("$.percentualDespesasFixas").value(5.00))
			.andExpect(jsonPath("$.percentualDespesasVariaveis").value(0.00));
	}

	@Test
	void cadaMeiConsultaOsPropriosParametros() throws Exception {
		apoio.gravarParametrosComSucesso(tokenDaLoja, "14000.00", "25.00");
		apoio.gravarParametrosComSucesso(tokenDaPapelaria, "6000.00", "20.00");

		apoio.consultarParametros(tokenDaLoja)
			.andExpect(jsonPath("$.receitaMensalEstimada").value(14000.00))
			.andExpect(jsonPath("$.margemDeLucro").value(25.00));
		apoio.consultarParametros(tokenDaPapelaria)
			.andExpect(jsonPath("$.receitaMensalEstimada").value(6000.00))
			.andExpect(jsonPath("$.margemDeLucro").value(20.00));
	}

	@Test
	void gravacaoNaoAlteraOutroMei() throws Exception {
		apoio.gravarParametrosComSucesso(tokenDaLoja, "14000.00", "25.00");
		UUID loja = jdbcTemplate.queryForObject("select tenant_id from usuario where email = ?", UUID.class,
				EMAIL_LOJA);
		String comTenantDaLoja = """
				{"receitaMensalEstimada": 6000.00, "margemDeLucro": 20.00, "tenantId": "%s"}
				""".formatted(loja);

		apoio.gravarParametros(tokenDaPapelaria, comTenantDaLoja)
			.andExpect(status().isOk());

		apoio.consultarParametros(tokenDaLoja)
			.andExpect(jsonPath("$.receitaMensalEstimada").value(14000.00))
			.andExpect(jsonPath("$.margemDeLucro").value(25.00));
		apoio.consultarParametros(tokenDaPapelaria)
			.andExpect(jsonPath("$.receitaMensalEstimada").value(6000.00));
	}

	@Test
	void despesasSemToken() throws Exception {
		mockMvc.perform(get(DESPESAS))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
	}

	@Test
	void parametrosSemToken() throws Exception {
		mockMvc.perform(get(PARAMETROS))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
	}

}
