package br.com.valorizamei.shared.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import br.com.valorizamei.TestcontainersConfiguration;
import br.com.valorizamei.shared.erro.ErroTesteController;

/**
 * O {@link ErroTesteController} é importado para que a especificação tenha ao menos um
 * endpoint de API publicado ({@code /api/v1/teste-erros}).
 */
@Import({ TestcontainersConfiguration.class, ErroTesteController.class })
@SpringBootTest
@AutoConfigureMockMvc
class DocumentacaoApiTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void especificacaoOpenApi31ComTituloEVersao() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.openapi", startsWith("3.1")))
			.andExpect(jsonPath("$.info.title").value("Valoriza MEI API"))
			.andExpect(jsonPath("$.info.version").value("v1"));
	}

	@Test
	void interfaceSwaggerUiDisponivel() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
	}

	@Test
	void atalhoRedirecionaParaSwaggerUi() throws Exception {
		mockMvc.perform(get("/swagger-ui.html"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/swagger-ui/index.html"));
	}

	@Test
	void endpointsDoActuatorNaoSaoDocumentados() throws Exception {
		assertThat(caminhosDocumentados()).noneMatch(caminho -> caminho.startsWith("/actuator"));
	}

	@Test
	void todosOsCaminhosDocumentadosUsamPrefixoVersionado() throws Exception {
		Set<String> caminhos = caminhosDocumentados();

		assertThat(caminhos).isNotEmpty().allMatch(caminho -> caminho.startsWith("/api/v1"));
	}

	@Test
	void esquemaBearerDeclaradoEExigidoPelaConta() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
			.andExpect(jsonPath("$.security[0].bearerAuth").exists())
			.andExpect(jsonPath("$.paths['/api/v1/conta'].get").exists())
			.andExpect(jsonPath("$.paths['/api/v1/conta'].get.security").doesNotExist());
	}

	@Test
	void cadastroELoginNaoExigemTokenNaDocumentacao() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/auth/cadastro'].post.security", hasSize(0)))
			.andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.security", hasSize(0)));
	}

	@Test
	void produtosDocumentadosExigemToken() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/produtos'].get.tags[0]").value("Produtos"))
			.andExpect(jsonPath("$.paths['/api/v1/produtos'].get.security").doesNotExist())
			.andExpect(jsonPath("$.paths['/api/v1/produtos'].post.security").doesNotExist())
			.andExpect(jsonPath("$.paths['/api/v1/produtos/{id}'].get.security").doesNotExist())
			.andExpect(jsonPath("$.paths['/api/v1/produtos/{id}'].put.security").doesNotExist())
			.andExpect(jsonPath("$.paths['/api/v1/produtos/{id}'].delete.security").doesNotExist());
	}

	@Test
	void despesasEParametrosDocumentadosExigemToken() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/despesas'].get.tags[0]").value("Despesas"))
			.andExpect(jsonPath("$.paths['/api/v1/despesas'].post.security").doesNotExist())
			.andExpect(jsonPath("$.paths['/api/v1/despesas/{id}'].put.security").doesNotExist())
			.andExpect(jsonPath("$.paths['/api/v1/despesas/rateio'].get.security").doesNotExist())
			.andExpect(jsonPath("$.paths['/api/v1/parametros-financeiros'].get.tags[0]")
				.value("Parâmetros financeiros"))
			.andExpect(jsonPath("$.paths['/api/v1/parametros-financeiros'].put.security").doesNotExist());
	}

	@Test
	void precificacaoDocumentadaExigeTokenEPreve422() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/precificacao'].get.tags[0]").value("Precificação"))
			.andExpect(jsonPath("$.paths['/api/v1/precificacao'].get.security").doesNotExist())
			.andExpect(jsonPath("$.paths['/api/v1/precificacao'].get.responses['422']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/precificacao/produtos/{id}'].get.security").doesNotExist())
			.andExpect(jsonPath("$.paths['/api/v1/precificacao/produtos/{id}'].get.responses['422']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/precificacao/produtos/{id}'].get.responses['404']").exists());
	}

	private Set<String> caminhosDocumentados() throws Exception {
		String especificacao = mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		Map<String, Object> paths = JsonPath.read(especificacao, "$.paths");
		return paths.keySet();
	}

}
