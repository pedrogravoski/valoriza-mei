package br.com.valorizamei.estoque.dto;

import java.util.UUID;

/**
 * Estoque atual (entradas menos saídas) e estoque mínimo de um produto do MEI.
 */
public record EstoqueProdutoResponse(UUID produtoId, String nome, int estoqueAtual, int estoqueMinimo) {

}
