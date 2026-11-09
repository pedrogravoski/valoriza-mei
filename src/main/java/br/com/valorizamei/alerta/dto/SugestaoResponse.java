package br.com.valorizamei.alerta.dto;

import java.util.UUID;

/**
 * Sugestão para um produto, com o motivo em português (RF09).
 */
public record SugestaoResponse(UUID produtoId, String nome, TipoSugestao tipo, String motivo) {

}
