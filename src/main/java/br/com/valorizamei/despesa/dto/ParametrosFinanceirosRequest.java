package br.com.valorizamei.despesa.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Receita mensal estimada em reais e margem de lucro desejada em pontos percentuais
 * (25.00 = 25%), ambas com no máximo 2 casas decimais.
 */
public record ParametrosFinanceirosRequest(
		@NotNull @Positive
		@Digits(integer = 10, fraction = 2, message = MensagensDeValidacao.DUAS_CASAS)
		BigDecimal receitaMensalEstimada,
		@NotNull @PositiveOrZero @DecimalMax(value = "100", inclusive = false)
		@Digits(integer = 3, fraction = 2, message = MensagensDeValidacao.DUAS_CASAS)
		BigDecimal margemDeLucro) {

}
