package br.com.valorizamei.estoque.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Movimentação de estoque, com valores em reais (2 casas).
 */
public record MovimentacaoResponse(UUID id, UUID produtoId, String nomeProduto, TipoMovimentacao tipo,
		int quantidade, BigDecimal valorUnitario, BigDecimal valorTotal, LocalDate data) {

}
