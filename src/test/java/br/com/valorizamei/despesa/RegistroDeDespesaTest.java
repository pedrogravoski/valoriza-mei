package br.com.valorizamei.despesa;

import static br.com.valorizamei.despesa.ApoioDeDespesas.ALUGUEL;
import static br.com.valorizamei.despesa.ApoioDeDespesas.DESPESAS;
import static br.com.valorizamei.despesa.ApoioDeDespesas.ENTREGA;
import static br.com.valorizamei.despesa.ApoioDeDespesas.TAXA_DE_CARTAO;
import static br.com.valorizamei.despesa.ApoioDeDespesas.despesaJson;
import static br.com.valorizamei.despesa.ApoioDeDespesas.fixaJson;
import static br.com.valorizamei.despesa.ApoioDeDespesas.variavelJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
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

import br.com.valorizamei.BancoDeTeste;
import br.com.valorizamei.TestcontainersConfiguration;

/**
 * Registro de despesas fixas e variáveis, validação e descrição única no MEI (spec despesas;
 * RF02).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class RegistroDeDespesaTest {

	private static final String UUID_REGEX = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

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
	void aluguelDaLojaDeFerramentas() throws Exception {
		apoio.registrar(tokenDaLoja, fixaJson(ALUGUEL, "1200.00"))
			.andExpect(status().isCreated())
			.andExpect(header().string(HttpHeaders.LOCATION, matchesPattern(".*" + DESPESAS + "/" + UUID_REGEX)))
			.andExpect(jsonPath("$.id", matchesPattern(UUID_REGEX)))
			.andExpect(jsonPath("$.descricao").value(ALUGUEL))
			.andExpect(jsonPath("$.tipo").value("FIXA"))
			.andExpect(jsonPath("$.valorMensal").value(1200.00))
			.andExpect(jsonPath("$.percentual").isEmpty())
			.andExpect(content().string(containsString("\"valorMensal\":1200.00")));
	}

	@Test
	void taxaDeCartaoDaLojaDeFerramentas() throws Exception {
		apoio.registrar(tokenDaLoja, variavelJson(TAXA_DE_CARTAO, "3"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.descricao").value(TAXA_DE_CARTAO))
			.andExpect(jsonPath("$.tipo").value("VARIAVEL"))
			.andExpect(jsonPath("$.percentual").value(3.00))
			.andExpect(jsonPath("$.valorMensal").isEmpty())
			.andExpect(content().string(containsString("\"percentual\":3.00")));
	}

	@Test
	void descricaoComEspacosNasPontas() throws Exception {
		apoio.registrar(tokenDaLoja, fixaJson("  " + ALUGUEL + "  ", "1200.00"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.descricao").value(ALUGUEL));
	}

	@Test
	void camposObrigatoriosAusentes() throws Exception {
		apoio.registrar(tokenDaLoja, "{}")
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.erros[*].campo", containsInAnyOrder("descricao", "tipo")));

		assertThat(contarDespesas()).isZero();
	}

	@Test
	void valorMensalComMaisDeDuasCasasDecimais() throws Exception {
		apoio.registrar(tokenDaLoja, fixaJson(ALUGUEL, "1200.005"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros", hasSize(1)))
			.andExpect(jsonPath("$.erros[0].campo").value("valorMensal"))
			.andExpect(jsonPath("$.erros[0].mensagem").value("informe o valor com no máximo 2 casas decimais"));

		assertThat(contarDespesas()).isZero();
	}

	@Test
	void percentualDeCemOuMais() throws Exception {
		apoio.registrar(tokenDaLoja, variavelJson(TAXA_DE_CARTAO, "100.00"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros", hasSize(1)))
			.andExpect(jsonPath("$.erros[0].campo").value("percentual"));

		assertThat(contarDespesas()).isZero();
	}

	@Test
	void valorMensalZero() throws Exception {
		apoio.registrar(tokenDaLoja, fixaJson(ALUGUEL, "0.00"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros[0].campo").value("valorMensal"));
	}

	@Test
	void tipoDesconhecido() throws Exception {
		apoio.registrar(tokenDaLoja, despesaJson(ALUGUEL, "OUTRA", "1200.00", null))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400));

		assertThat(contarDespesas()).isZero();
	}

	@Test
	void despesaFixaSemValorMensal() throws Exception {
		apoio.registrar(tokenDaLoja, despesaJson(ALUGUEL, "FIXA", null, null))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros", hasSize(1)))
			.andExpect(jsonPath("$.erros[0].campo").value("valorMensal"))
			.andExpect(jsonPath("$.erros[0].mensagem").value("informe o valor mensal da despesa fixa"));
	}

	@Test
	void despesaFixaComPercentual() throws Exception {
		apoio.registrar(tokenDaLoja, despesaJson(ALUGUEL, "FIXA", "1200.00", "3.00"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros", hasSize(1)))
			.andExpect(jsonPath("$.erros[0].campo").value("percentual"))
			.andExpect(jsonPath("$.erros[0].mensagem").value("não se aplica a este tipo de despesa"));
	}

	@Test
	void despesaVariavelComValorMensal() throws Exception {
		apoio.registrar(tokenDaLoja, despesaJson(ENTREGA, "VARIAVEL", "50.00", "2.00"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros", hasSize(1)))
			.andExpect(jsonPath("$.erros[0].campo").value("valorMensal"))
			.andExpect(jsonPath("$.erros[0].mensagem").value("não se aplica a este tipo de despesa"));
	}

	@Test
	void despesaVariavelSemPercentual() throws Exception {
		apoio.registrar(tokenDaLoja, despesaJson(ENTREGA, "VARIAVEL", null, null))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros", hasSize(1)))
			.andExpect(jsonPath("$.erros[0].campo").value("percentual"))
			.andExpect(jsonPath("$.erros[0].mensagem").value("informe o percentual da despesa variável"));
	}

	@Test
	void aluguelRegistradoDuasVezes() throws Exception {
		apoio.registrarComSucesso(tokenDaLoja, fixaJson(ALUGUEL, "1200.00"));

		apoio.registrar(tokenDaLoja, fixaJson("ALUGUEL", "1200.00"))
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Despesa já cadastrada"))
			.andExpect(jsonPath("$.detail").value("Já existe uma despesa com esta descrição."));

		assertThat(contarDespesas()).isEqualTo(1);
	}

	@Test
	void mesmaDescricaoEmMeisDiferentes() throws Exception {
		apoio.registrarComSucesso(tokenDaLoja, fixaJson(ALUGUEL, "1200.00"));
		String tokenDaPapelaria = apoio.tokenDaPapelaria();

		apoio.registrar(tokenDaPapelaria, fixaJson(ALUGUEL, "900.00"))
			.andExpect(status().isCreated());
	}

	private int contarDespesas() {
		return jdbcTemplate.queryForObject("select count(*) from despesa", Integer.class);
	}

}
