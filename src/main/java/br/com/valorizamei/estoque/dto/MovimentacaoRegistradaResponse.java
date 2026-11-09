package br.com.valorizamei.estoque.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Movimentação recém-registrada, com o estoque atual do produto depois dela.
 */
public record MovimentacaoRegistradaResponse(UUID id, UUID produtoId, String nomeProduto, TipoMovimentacao tipo,
		int quantidade, BigDecimal valorUnitario, BigDecimal valorTotal, LocalDate data, int estoqueAtual) {

}
