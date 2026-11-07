package br.com.valorizamei.precificacao.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Preço de venda sugerido de um produto (RF04), com o índice de markup usado (4 casas).
 */
public record PrecificacaoDeProdutoResponse(UUID id, String nome, BigDecimal custoDireto, BigDecimal indiceMarkup,
		BigDecimal precoSugerido) {

}
