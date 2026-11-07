package br.com.valorizamei.despesa.dto;

import java.math.BigDecimal;

/**
 * Rateio das despesas do MEI (RF05), com 2 casas decimais e percentuais em pontos percentuais.
 * Sem receita mensal estimada informada, a receita e o percentual das despesas fixas (DF) vêm
 * nulos.
 *
 * @param totalDespesasFixas soma dos valores mensais das despesas fixas
 * @param receitaMensalEstimada receita informada nos parâmetros financeiros
 * @param percentualDespesasFixas DF = total das despesas fixas × 100 / receita mensal estimada
 * @param percentualDespesasVariaveis DV = soma dos percentuais das despesas variáveis
 */
public record RateioResponse(BigDecimal totalDespesasFixas, BigDecimal receitaMensalEstimada,
		BigDecimal percentualDespesasFixas, BigDecimal percentualDespesasVariaveis) {

}
