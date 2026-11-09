package br.com.valorizamei.caixa.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Fluxo de caixa do MEI no período (datas inclusivas), com os lançamentos em ordem cronológica.
 * O saldo é o total de entradas menos o total de saídas e pode ser negativo.
 */
public record FluxoDeCaixaResponse(LocalDate inicio, LocalDate fim, BigDecimal totalEntradas, BigDecimal totalSaidas,
		BigDecimal saldo, List<LancamentoResponse> lancamentos) {

}
