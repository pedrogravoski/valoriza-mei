package br.com.valorizamei.shared.seguranca;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.servlet.HandlerExceptionResolver;

import br.com.valorizamei.shared.erro.ErroDeNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Valida as requisições CORS como o {@link DefaultCorsProcessor}, mas responde à recusa
 * (origem, método ou cabeçalho não permitidos) com 403 em Problem Details, em português, pelo
 * tratamento de erros do Spring MVC, como as demais respostas de erro da API.
 * <p>
 * O {@code rejectRequest} da classe base só recebe a resposta; por isso ele interrompe a
 * validação com uma exceção interna, e o {@code processRequest}, que tem a requisição, monta o
 * erro.
 */
class ProcessadorDeCors extends DefaultCorsProcessor {

	private final HandlerExceptionResolver tratadorDeErros;

	ProcessadorDeCors(HandlerExceptionResolver tratadorDeErros) {
		this.tratadorDeErros = tratadorDeErros;
	}

	@Override
	public boolean processRequest(CorsConfiguration configuracao, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		try {
			return super.processRequest(configuracao, request, response);
		}
		catch (RequisicaoCorsRecusada ex) {
			ErroDeNegocioException erro = new ErroDeNegocioException(HttpStatus.FORBIDDEN, "Origem não permitida",
					"A origem da requisição não tem permissão para acessar a API.");
			if (tratadorDeErros.resolveException(request, response, null, erro) == null) {
				response.sendError(HttpStatus.FORBIDDEN.value());
			}
			return false;
		}
	}

	@Override
	protected void rejectRequest(ServerHttpResponse response) {
		throw new RequisicaoCorsRecusada();
	}

	/** Sinaliza a recusa ao {@link #processRequest}; nunca sai desta classe. */
	private static final class RequisicaoCorsRecusada extends RuntimeException {

		private static final long serialVersionUID = 1L;

		RequisicaoCorsRecusada() {
			super(null, null, false, false);
		}

	}

}
