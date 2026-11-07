package br.com.valorizamei.despesa.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Dados para registrar ou alterar uma despesa. A fixa informa só o valor mensal em reais; a
 * variável, só o percentual sobre o preço de venda em pontos percentuais (3.00 = 3%). Os espaços
 * nas extremidades da descrição são removidos antes da validação.
 */
@ValorConformeTipo
public record DespesaRequest(
		@NotBlank @Size(max = 120) String descricao,
		@NotNull TipoDespesa tipo,
		@Positive @Digits(integer = 10, fraction = 2, message = MensagensDeValidacao.DUAS_CASAS)
		BigDecimal valorMensal,
		@Positive @DecimalMax(value = "100", inclusive = false)
		@Digits(integer = 3, fraction = 2, message = MensagensDeValidacao.DUAS_CASAS)
		BigDecimal percentual) {

	public DespesaRequest {
		descricao = descricao == null ? null : descricao.strip();
	}

}
