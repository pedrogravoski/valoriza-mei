package br.com.valorizamei.produto.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Dados para cadastrar ou alterar um produto. O custo direto é em reais, com no máximo 2 casas
 * decimais (um valor com mais casas é recusado, não arredondado); os espaços nas extremidades do
 * nome são removidos antes da validação.
 */
public record ProdutoRequest(
		@NotBlank @Size(max = 120) String nome,
		@NotNull @Positive
		@Digits(integer = 10, fraction = 2, message = "informe o valor com no máximo 2 casas decimais")
		BigDecimal custoDireto,
		@NotNull @PositiveOrZero Integer estoqueMinimo) {

	public ProdutoRequest {
		nome = nome == null ? null : nome.strip();
	}

}
