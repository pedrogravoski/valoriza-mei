package br.com.valorizamei.shared.erro;

import java.math.BigDecimal;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Endpoint existente só nos testes, usado para provocar erros de validação, método não
 * suportado e falha inesperada. Por ser {@link TestComponent}, não é encontrado pela
 * varredura de componentes: cada teste que precisa dele o importa explicitamente.
 */
@TestComponent
@RestController
@RequestMapping("/api/v1/teste-erros")
public class ErroTesteController {

	static final String MENSAGEM_TECNICA = "falha de conexão interna";

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ItemTesteRequest criar(@Valid @RequestBody ItemTesteRequest item) {
		return item;
	}

	@PostMapping("/falha")
	void falhar() {
		throw new IllegalStateException(MENSAGEM_TECNICA);
	}

	record ItemTesteRequest(
			@NotBlank String nome,
			@NotNull @PositiveOrZero BigDecimal valor) {
	}

}
