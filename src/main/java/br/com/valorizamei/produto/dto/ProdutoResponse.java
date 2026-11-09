package br.com.valorizamei.produto.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Produto do MEI, com o custo direto sempre com 2 casas decimais.
 */
public record ProdutoResponse(UUID id, String nome, BigDecimal custoDireto, int estoqueMinimo) {

}
