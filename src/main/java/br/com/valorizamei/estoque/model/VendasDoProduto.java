package br.com.valorizamei.estoque.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Total das saídas (vendas) de um produto num período: unidades vendidas e valor vendido.
 * Resultado da consulta agregada do {@code MovimentacaoEstoqueRepository}; não é uma entidade.
 */
public record VendasDoProduto(UUID produtoId, long quantidade, BigDecimal valorTotal) {

}
