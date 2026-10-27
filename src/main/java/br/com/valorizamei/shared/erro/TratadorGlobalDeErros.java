package br.com.valorizamei.shared.erro;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Converte todas as falhas da API em respostas Problem Details (RFC 9457) com textos em
 * português, sem expor detalhes internos.
 */
@RestControllerAdvice
public class TratadorGlobalDeErros extends ResponseEntityExceptionHandler {

	private static final Logger LOG = LoggerFactory.getLogger(TratadorGlobalDeErros.class);

	private static final String TITULO_GENERICO = "Erro na requisição";

	/** Textos em português para os erros tratados pela classe base, por status. */
	private static final Map<Integer, Mensagem> MENSAGENS_POR_STATUS = Map.of(
			400, new Mensagem("Requisição inválida", "A requisição não pôde ser processada."),
			404, new Mensagem("Recurso não encontrado", "O recurso solicitado não existe."),
			405, new Mensagem("Método não permitido", "O método HTTP utilizado não é suportado por este recurso."),
			406, new Mensagem("Formato não aceito", "O formato de resposta solicitado não é suportado."),
			415, new Mensagem("Tipo de conteúdo não suportado", "O tipo de conteúdo enviado não é suportado por este recurso."),
			500, new Mensagem("Erro interno", "Ocorreu um erro inesperado. Tente novamente mais tarde."),
			503, new Mensagem("Serviço indisponível", "O serviço está temporariamente indisponível. Tente novamente mais tarde."));

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<ErroDeCampo> erros = ex.getBindingResult().getFieldErrors().stream()
			.map(erro -> new ErroDeCampo(erro.getField(), erro.getDefaultMessage()))
			.toList();

		ProblemDetail problema = ProblemDetail.forStatusAndDetail(status,
				"Um ou mais campos possuem valores inválidos.");
		problema.setTitle("Dados inválidos");
		problema.setProperty("erros", erros);
		return handleExceptionInternal(ex, problema, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, "O corpo da requisição é inválido.");
		problema.setTitle("Requisição inválida");
		return handleExceptionInternal(ex, problema, headers, status, request);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<Object> tratarErroInesperado(Exception ex, WebRequest request) {
		LOG.error("Erro inesperado ao processar {}", caminho(request), ex);
		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		ProblemDetail problema = ProblemDetail.forStatus(status);
		return handleExceptionInternal(ex, problema, new HttpHeaders(), status, request);
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		if (body == null && ex instanceof ErrorResponse respostaDeErro) {
			// A classe base passa o corpo nulo para exceções como 404 e 405 e só o monta depois.
			body = respostaDeErro.updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale());
		}
		if (body instanceof ProblemDetail problema) {
			traduzirSePadrao(problema, statusCode);
			if (problema.getInstance() == null && caminho(request) != null) {
				problema.setInstance(URI.create(caminho(request)));
			}
		}
		return super.handleExceptionInternal(ex, body, headers, statusCode, request);
	}

	/**
	 * Substitui título e detalhe padrão do Spring (em inglês) pelos textos em português.
	 * Problemas que já receberam título próprio neste tratador são mantidos.
	 */
	private static void traduzirSePadrao(ProblemDetail problema, HttpStatusCode statusCode) {
		HttpStatus status = HttpStatus.resolve(statusCode.value());
		String tituloPadrao = status != null ? status.getReasonPhrase() : null;
		if (problema.getTitle() != null && !problema.getTitle().equals(tituloPadrao)) {
			return;
		}
		Mensagem mensagem = MENSAGENS_POR_STATUS.getOrDefault(statusCode.value(),
				new Mensagem(TITULO_GENERICO, "A requisição não pôde ser processada."));
		problema.setTitle(mensagem.titulo());
		problema.setDetail(mensagem.detalhe());
	}

	private static String caminho(WebRequest request) {
		return request instanceof ServletWebRequest servletRequest
				? servletRequest.getRequest().getRequestURI()
				: null;
	}

	private record Mensagem(String titulo, String detalhe) {
	}

}
