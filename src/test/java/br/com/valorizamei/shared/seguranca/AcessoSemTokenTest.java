package br.com.valorizamei.shared.seguranca;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import br.com.valorizamei.TestcontainersConfiguration;

/**
 * Rotas protegidas e públicas para um cliente sem token (specs erros-api, autenticacao e
 * documentacao-api).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AcessoSemTokenTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void endpointProtegidoSemTokenRetorna401EmProblemDetails() throws Exception {
		mockMvc.perform(get("/api/v1/conta"))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.title").value("Não autenticado"))
			.andExpect(jsonPath("$.detail").value("É necessário enviar um token de acesso válido."))
			.andExpect(jsonPath("$.instance").value("/api/v1/conta"))
			.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
	}

	@Test
	void rotaInexistenteSemTokenRetorna401() throws Exception {
		mockMvc.perform(get("/api/v1/recurso-inexistente"))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void healthSemTokenRetorna200() throws Exception {
		mockMvc.perform(get("/actuator/health"))
			.andExpect(status().isOk());
	}

	@Test
	void especificacaoESwaggerUiSemTokenRetornam200() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk());
		mockMvc.perform(get("/swagger-ui/index.html"))
			.andExpect(status().isOk());
	}

}
