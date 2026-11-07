package br.com.valorizamei.estoque.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

/**
 * Dados de uma entrada (compra) ou saída (venda) de estoque. A saída informa o valor unitário da
 * venda; a entrada não, porque usa o custo direto do produto. Sem data, a movimentação recebe a
 * data atual; a data não pode ser futura.
 */
@ValorUnitarioConformeTipo
public record MovimentacaoRequest(
		@NotNull UUID produtoId,
		@NotNull TipoMovimentacao tipo,
		@NotNull @Min(1) @Max(1_000_000) Integer quantidade,
		@Positive @Digits(integer = 10, fraction = 2, message = MensagensDeValidacao.DUAS_CASAS)
		BigDecimal valorUnitario,
		@PastOrPresent(message = MensagensDeValidacao.DATA_FUTURA) LocalDate data) {

}
