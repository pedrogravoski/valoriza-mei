package br.com.valorizamei.shared.erro;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * Erro previsto por uma regra de negócio, devolvido ao cliente em Problem Details com o
 * status, o título e o detalhe informados (em português). Exemplos: e-mail já cadastrado (409)
 * e credenciais inválidas (401).
 */
public class ErroDeNegocioException extends ErrorResponseException {

	public ErroDeNegocioException(HttpStatusCode status, String titulo, String detalhe) {
		super(status, problema(status, titulo, detalhe), null);
	}

	private static ProblemDetail problema(HttpStatusCode status, String titulo, String detalhe) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
		problema.setTitle(titulo);
		return problema;
	}

}
