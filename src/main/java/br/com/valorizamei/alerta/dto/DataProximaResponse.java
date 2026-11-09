package br.com.valorizamei.alerta.dto;

import java.time.LocalDate;

/**
 * Data comemorativa que ocorre nos próximos 30 dias (RF09).
 */
public record DataProximaResponse(String nome, LocalDate data, long diasRestantes, boolean personalizada) {

}
