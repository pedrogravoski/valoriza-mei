package br.com.valorizamei.precificacao.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Preço de venda sugerido de um produto na lista da precificação.
 */
public record PrecoSugeridoResponse(UUID id, String nome, BigDecimal custoDireto, BigDecimal precoSugerido) {

}
