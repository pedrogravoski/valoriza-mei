package br.com.valorizamei;

import static br.com.valorizamei.ApoioDeAutenticacao.bearer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class HealthCheckTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void healthRespondeUpSemDetalhes() throws Exception {
		mockMvc.perform(get("/actuator/health"))
			.andExpect(status().isOk())
			.andExpect(content().json("{\"status\":\"UP\"}", JsonCompareMode.STRICT));
	}

	@Test
	void endpointDeGerenciamentoNaoExpostoRetorna404() throws Exception {
		String token = new ApoioDeAutenticacao(mockMvc).tokenDeNovaConta();

		mockMvc.perform(get("/actuator/env").header(HttpHeaders.AUTHORIZATION, bearer(token)))
			.andExpect(status().isNotFound());
	}

	@Test
	void endpointDeGerenciamentoSemTokenRetorna401() throws Exception {
		mockMvc.perform(get("/actuator/env"))
			.andExpect(status().isUnauthorized());
	}

}
