package br.com.valorizamei.despesa;

import static br.com.valorizamei.despesa.ApoioDeDespesas.ALUGUEL;
import static br.com.valorizamei.despesa.ApoioDeDespesas.DESPESAS;
import static br.com.valorizamei.despesa.ApoioDeDespesas.ENTREGA;
import static br.com.valorizamei.despesa.ApoioDeDespesas.HONORARIOS;
import static br.com.valorizamei.despesa.ApoioDeDespesas.INTERNET;
import static br.com.valorizamei.despesa.ApoioDeDespesas.TAXA_DE_CARTAO;
import static br.com.valorizamei.despesa.ApoioDeDespesas.despesaJson;
import static br.com.valorizamei.despesa.ApoioDeDespesas.fixaJson;
import static br.com.valorizamei.despesa.ApoioDeDespesas.variavelJson;
import static br.com.valorizamei.ApoioDeAutenticacao.bearer;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

import br.com.valorizamei.BancoDeTeste;
import br.com.valorizamei.TestcontainersConfiguration;

/**
 * Listagem, consulta, alteração e exclusão de despesas (spec despesas; RF02).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ConsultaEAlteracaoDeDespesaTest {

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
	void despesasDaLojaDeFerramentas() throws Exception {
		apoio.registrarEstudoDeCaso(tokenDaLoja);

		apoio.listar(tokenDaLoja)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(5)))
			.andExpect(jsonPath("$[*].descricao", contains(ALUGUEL, ENTREGA, HONORARIOS, INTERNET, TAXA_DE_CARTAO)));
	}

	@Test
	void somenteAsDespesasFixas() throws Exception {
		apoio.registrarEstudoDeCaso(tokenDaLoja);

		listarPorTipo("FIXA")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].descricao", contains(ALUGUEL, HONORARIOS, INTERNET)));
	}

	@Test
	void somenteAsDespesasVariaveis() throws Exception {
		apoio.registrarEstudoDeCaso(tokenDaLoja);

		listarPorTipo("VARIAVEL")
			.andExpect(jsonPath("$[*].descricao", contains(ENTREGA, TAXA_DE_CARTAO)));
	}

	@Test
	void filtroComTipoDesconhecido() throws Exception {
		listarPorTipo("OUTRA")
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void meiSemDespesas() throws Exception {
		apoio.listar(tokenDaLoja)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void reajusteDoAluguel() throws Exception {
		String aluguel = apoio.registrarComSucesso(tokenDaLoja, fixaJson(ALUGUEL, "1200.00"));

		apoio.alterar(tokenDaLoja, aluguel, fixaJson(ALUGUEL, "1300.00"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(aluguel))
			.andExpect(jsonPath("$.valorMensal").value(1300.00));

		apoio.consultar(tokenDaLoja, aluguel)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.descricao").value(ALUGUEL))
			.andExpect(jsonPath("$.valorMensal").value(1300.00));
	}

	@Test
	void trocaDeDespesaFixaParaVariavel() throws Exception {
		String internet = apoio.registrarComSucesso(tokenDaLoja, fixaJson(INTERNET, "120.00"));

		apoio.alterar(tokenDaLoja, internet, variavelJson(INTERNET, "1.00"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tipo").value("VARIAVEL"))
			.andExpect(jsonPath("$.percentual").value(1.00))
			.andExpect(jsonPath("$.valorMensal").isEmpty());
	}

	@Test
	void alteracaoComValorIncompativelComOTipo() throws Exception {
		String aluguel = apoio.registrarComSucesso(tokenDaLoja, fixaJson(ALUGUEL, "1200.00"));

		apoio.alterar(tokenDaLoja, aluguel, despesaJson(ALUGUEL, "VARIAVEL", "1200.00", null))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros[*].campo", containsInAnyOrder("percentual", "valorMensal")));

		apoio.consultar(tokenDaLoja, aluguel)
			.andExpect(jsonPath("$.tipo").value("FIXA"))
			.andExpect(jsonPath("$.valorMensal").value(1200.00));
	}

	@Test
	void exclusaoDosHonorarios() throws Exception {
		String honorarios = apoio.registrarComSucesso(tokenDaLoja, fixaJson(HONORARIOS, "80.00"));

		apoio.excluir(tokenDaLoja, honorarios)
			.andExpect(status().isNoContent())
			.andExpect(content().string(""));

		apoio.consultar(tokenDaLoja, honorarios)
			.andExpect(status().isNotFound());
	}

	@Test
	void despesaInexistente() throws Exception {
		String inexistente = UUID.randomUUID().toString();

		apoio.consultar(tokenDaLoja, inexistente)
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Despesa não encontrada"))
			.andExpect(jsonPath("$.detail").value("Não existe despesa com este identificador."));
		apoio.alterar(tokenDaLoja, inexistente, fixaJson(ALUGUEL, "1200.00"))
			.andExpect(status().isNotFound());
		apoio.excluir(tokenDaLoja, inexistente)
			.andExpect(status().isNotFound());
	}

	@Test
	void alteracaoParaADescricaoDeOutraDespesaDoMei() throws Exception {
		apoio.registrarComSucesso(tokenDaLoja, fixaJson(ALUGUEL, "1200.00"));
		String internet = apoio.registrarComSucesso(tokenDaLoja, fixaJson(INTERNET, "120.00"));

		apoio.alterar(tokenDaLoja, internet, fixaJson("aluguel", "120.00"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.title").value("Despesa já cadastrada"));

		apoio.consultar(tokenDaLoja, internet)
			.andExpect(jsonPath("$.descricao").value(INTERNET));
	}

	@Test
	void alteracaoMantendoAPropriaDescricaoComOutraGrafia() throws Exception {
		String aluguel = apoio.registrarComSucesso(tokenDaLoja, fixaJson(ALUGUEL, "1200.00"));

		apoio.alterar(tokenDaLoja, aluguel, fixaJson("ALUGUEL", "1200.00"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.descricao").value("ALUGUEL"));
	}

	private ResultActions listarPorTipo(String tipo) throws Exception {
		return mockMvc.perform(get(DESPESAS).param("tipo", tipo)
			.header(HttpHeaders.AUTHORIZATION, bearer(tokenDaLoja)));
	}

}
