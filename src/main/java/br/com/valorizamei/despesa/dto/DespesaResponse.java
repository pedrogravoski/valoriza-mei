package br.com.valorizamei.despesa.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Despesa do MEI, com valores de 2 casas decimais. O campo que não se aplica ao tipo vem nulo.
 */
public record DespesaResponse(UUID id, String descricao, TipoDespesa tipo, BigDecimal valorMensal,
		BigDecimal percentual) {

}
