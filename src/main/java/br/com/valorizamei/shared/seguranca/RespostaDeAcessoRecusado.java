package br.com.valorizamei.shared.seguranca;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Responde às recusas do filtro de segurança (401 sem token válido, 403 sem permissão)
 * repassando a exceção ao tratamento de erros do Spring MVC. Assim, essas respostas saem no
 * mesmo formato Problem Details, em português, que o {@code TratadorGlobalDeErros} usa para
 * o restante da API.
 */
class RespostaDeAcessoRecusado implements AuthenticationEntryPoint, AccessDeniedHandler {

	private final HandlerExceptionResolver tratadorDeErros;

	RespostaDeAcessoRecusado(HandlerExceptionResolver tratadorDeErros) {
		this.tratadorDeErros = tratadorDeErros;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException ex) throws IOException {
		repassar(request, response, ex, HttpStatus.UNAUTHORIZED);
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException ex) throws IOException {
		repassar(request, response, ex, HttpStatus.FORBIDDEN);
	}

	private void repassar(HttpServletRequest request, HttpServletResponse response, Exception ex,
			HttpStatus statusSeNaoTratado) throws IOException {
		if (tratadorDeErros.resolveException(request, response, null, ex) == null) {
			response.sendError(statusSeNaoTratado.value());
		}
	}

}
