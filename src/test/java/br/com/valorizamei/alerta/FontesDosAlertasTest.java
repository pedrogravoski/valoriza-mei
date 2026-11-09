package br.com.valorizamei.alerta;

import static br.com.valorizamei.produto.ApoioDeProdutos.CAIXA;
import static br.com.valorizamei.produto.ApoioDeProdutos.EMAIL_LOJA;
import static br.com.valorizamei.produto.ApoioDeProdutos.FURADEIRA;
import static br.com.valorizamei.produto.ApoioDeProdutos.JOGO_DE_CHAVES;
import static br.com.valorizamei.produto.ApoioDeProdutos.TRENA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import br.com.valorizamei.ApoioDeTenant;
import br.com.valorizamei.BancoDeTeste;
import br.com.valorizamei.RelogioFixoConfiguration;
import br.com.valorizamei.TestcontainersConfiguration;
import br.com.valorizamei.despesa.ApoioDeDespesas;
import br.com.valorizamei.estoque.ApoioDeEstoque;
import br.com.valorizamei.estoque.dto.VendasDoProdutoResponse;
import br.com.valorizamei.estoque.service.EstoqueService;
import br.com.valorizamei.precificacao.service.PrecificacaoService;

/**
 * Dados que os alertas leem de outras features: vendas por produto no período (estoque) e
 * precificação sem recusa (precificação).
 */
@Import({ TestcontainersConfiguration.class, RelogioFixoConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
class FontesDosAlertasTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EstoqueService estoqueService;

	@Autowired
	private PrecificacaoService precificacaoService;

	private ApoioDeEstoque estoque;

	private ApoioDeDespesas despesas;

	private String tokenDaLoja;

	private Map<String, String> produtos;

	@BeforeEach
	void montarEstudoDeCaso() throws Exception {
		BancoDeTeste.limpar(jdbcTemplate);
		estoque = new ApoioDeEstoque(mockMvc);
		despesas = new ApoioDeDespesas(mockMvc);
		tokenDaLoja = estoque.produtos.tokenDaLoja();
		produtos = estoque.estoqueDoEstudoDeCaso(tokenDaLoja);
	}

	@AfterEach
	void sairDoMei() {
		ApoioDeTenant.limpar();
	}

	@Test
	void vendasDosUltimosTrintaDiasDoEstudoDeCaso() {
		comoLoja();
		Map<UUID, VendasDoProdutoResponse> vendas = estoqueService
			.vendasPorProduto(LocalDate.of(2026, 6, 16), LocalDate.of(2026, 7, 15)).stream()
			.collect(Collectors.toMap(VendasDoProdutoResponse::produtoId, venda -> venda));

		assertThat(vendas).hasSize(5).doesNotContainKey(id(CAIXA));
		assertThat(vendas.get(id(FURADEIRA)).quantidade()).isEqualTo(6);
		assertThat(vendas.get(id(FURADEIRA)).valorTotal()).isEqualTo(new BigDecimal("1800.00"));
		assertThat(vendas.get(id(TRENA)).quantidade()).isEqualTo(10);
		assertThat(vendas.get(id(JOGO_DE_CHAVES)).valorTotal()).isEqualTo(new BigDecimal("375.03"));
	}

	@Test
	void vendasForaDoPeriodoNaoEntram() {
		comoLoja();
		assertThat(estoqueService.vendasPorProduto(LocalDate.of(2026, 7, 11), LocalDate.of(2026, 7, 15))).isEmpty();
	}

	@Test
	void precificacaoIndisponivelSemParametros() {
		comoLoja();
		assertThat(precificacaoService.consultarSeDisponivel()).isEmpty();
	}

	@Test
	void precificacaoIndisponivelComMarkupInviavel() throws Exception {
		despesas.registrarEstudoDeCaso(tokenDaLoja);
		despesas.gravarParametros(tokenDaLoja, ApoioDeDespesas.parametrosJson("14000.00", "85.00"))
			.andExpect(status().isOk());
		comoLoja();

		assertThat(precificacaoService.consultarSeDisponivel()).isEmpty();
	}

	@Test
	void precificacaoDisponivelComOEstudoDeCaso() throws Exception {
		despesas.registrarEstudoDeCaso(tokenDaLoja);
		despesas.gravarParametrosComSucesso(tokenDaLoja, "14000.00", "25.00");
		comoLoja();

		assertThat(precificacaoService.consultarSeDisponivel()).hasValueSatisfying(precificacao -> {
			assertThat(precificacao.indiceMarkup()).isEqualTo(new BigDecimal("1.6667"));
			assertThat(precificacao.produtos()).hasSize(6);
		});
	}

	/** Autentica a thread como a loja; depois de requisições do MockMvc, que limpam o contexto. */
	private void comoLoja() {
		ApoioDeTenant.autenticarComo(jdbcTemplate, EMAIL_LOJA);
	}

	private UUID id(String nome) {
		return UUID.fromString(produtos.get(nome));
	}

}
