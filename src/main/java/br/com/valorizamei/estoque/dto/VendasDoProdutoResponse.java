package br.com.valorizamei.estoque.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Unidades vendidas e valor vendido (2 casas) de um produto num período, base do giro usado nos
 * alertas (RF09).
 */
public record VendasDoProdutoResponse(UUID produtoId, long quantidade, BigDecimal valorTotal) {

}
