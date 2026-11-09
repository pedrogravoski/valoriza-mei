package br.com.valorizamei.shared.seguranca;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import br.com.valorizamei.TestcontainersConfiguration;

/**
 * Origens configuradas por propriedade (spec cors, cenário "Duas origens configuradas"; RF11).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "valorizamei.cors.origens-permitidas="
		+ CorsTest.FRONTEND + "," + CorsComVariasOrigensTest.PRODUCAO)
class CorsComVariasOrigensTest {

	static final String PRODUCAO = "https://app.valorizamei.com.br";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void duasOrigensConfiguradas() throws Exception {
		preChecagem(CorsTest.FRONTEND)
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, CorsTest.FRONTEND));
		preChecagem(PRODUCAO)
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, PRODUCAO));
		preChecagem("https://outro.com.br")
			.andExpect(status().isForbidden());
	}

	private ResultActions preChecagem(String origem) throws Exception {
		return mockMvc.perform(options("/api/v1/produtos").header(HttpHeaders.ORIGIN, origem)
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"));
	}

}
