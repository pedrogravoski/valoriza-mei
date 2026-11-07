package br.com.valorizamei.despesa.dto;

import java.math.BigDecimal;

/**
 * Parâmetros financeiros do MEI; os dois valores vêm nulos enquanto o MEI não os informar.
 */
public record ParametrosFinanceirosResponse(BigDecimal receitaMensalEstimada, BigDecimal margemDeLucro) {

}
