package br.com.valorizamei.caixa.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Lançamento do fluxo de caixa, com o valor em reais (2 casas).
 */
public record LancamentoResponse(UUID id, LocalDate data, TipoLancamento tipo, String descricao, BigDecimal valor) {

}
