package br.com.valorizamei.precificacao.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Precificação do MEI (RF03, RF04): os percentuais usados no markup, com 2 casas e em pontos
 * percentuais, o índice de markup com 4 casas e o preço sugerido de cada produto, em ordem
 * alfabética.
 */
public record PrecificacaoResponse(BigDecimal percentualDespesasVariaveis, BigDecimal percentualDespesasFixas,
		BigDecimal margemDeLucro, BigDecimal indiceMarkup, List<PrecoSugeridoResponse> produtos) {

}
