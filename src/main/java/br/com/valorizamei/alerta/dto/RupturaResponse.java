package br.com.valorizamei.alerta.dto;

import java.util.UUID;

/**
 * Produto em ruptura (RF08): estoque atual menor ou igual ao estoque mínimo.
 */
public record RupturaResponse(UUID produtoId, String nome, int estoqueAtual, int estoqueMinimo) {

}
