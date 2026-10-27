package br.com.valorizamei.shared.erro;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import br.com.valorizamei.TestcontainersConfiguration;

@Import({ TestcontainersConfiguration.class, ErroTesteController.class })
@SpringBootTest
@AutoConfigureMockMvc
class TratadorGlobalDeErrosTest {

	private static final String ENDPOINT = "/api/v1/teste-erros";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void rotaInexistenteRetorna404EmProblemDetails() throws Exception {
		mockMvc.perform(get("/api/v1/recurso-inexistente"))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.title").value("Recurso não encontrado"))
			.andExpect(jsonPath("$.detail").value("O recurso solicitado não existe."))
			.andExpect(jsonPath("$.instance").value("/api/v1/recurso-inexistente"));
	}

	@Test
	void camposObrigatoriosAusentesListamUmErroPorCampo() throws Exception {
		mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.title").value("Dados inválidos"))
			.andExpect(jsonPath("$.erros", hasSize(2)))
			.andExpect(jsonPath("$.erros[*].campo", containsInAnyOrder("nome", "valor")))
			.andExpect(jsonPath("$.erros[0].mensagem", notNullValue()))
			.andExpect(jsonPath("$.erros[1].mensagem", notNullValue()));
	}

	@Test
	void mensagensDeValidacaoSaoEmPortugues() throws Exception {
		mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("{\"valor\": 10}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros[0].campo").value("nome"))
			.andExpect(jsonPath("$.erros[0].mensagem").value("não deve estar em branco"));
	}

	@Test
	void valorNegativoEmCampoQueExigeZeroOuMaisRetorna400() throws Exception {
		mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\": \"Furadeira\", \"valor\": -1}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros", hasSize(1)))
			.andExpect(jsonPath("$.erros[0].campo").value("valor"));
	}

	@Test
	void jsonInvalidoRetorna400ComCorpoInvalido() throws Exception {
		mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("{\"nome\": "))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("O corpo da requisição é inválido."));
	}

	@Test
	void metodoNaoAceitoRetorna405EmProblemDetails() throws Exception {
		mockMvc.perform(delete(ENDPOINT))
			.andExpect(status().isMethodNotAllowed())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(405))
			.andExpect(jsonPath("$.title").value("Método não permitido"));
	}

	@Test
	void falhaInesperadaRetorna500SemDetalhesInternos() throws Exception {
		mockMvc.perform(post(ENDPOINT + "/falha"))
			.andExpect(status().isInternalServerError())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(500))
			.andExpect(jsonPath("$.detail").value("Ocorreu um erro inesperado. Tente novamente mais tarde."))
			.andExpect(content().string(not(containsString(ErroTesteController.MENSAGEM_TECNICA))))
			.andExpect(content().string(not(containsString("IllegalStateException"))))
			.andExpect(content().string(not(containsString("at br.com.valorizamei"))));
	}

	@Test
	void requisicaoValidaNaoGeraErro() throws Exception {
		mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\": \"Furadeira\", \"valor\": 180.00}"))
			.andExpect(status().isCreated());
	}

}
