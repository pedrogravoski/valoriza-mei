package br.com.valorizamei.calendario.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Data comemorativa com a próxima ocorrência a partir da data atual (hoje, se a data cai hoje).
 * As datas fixas têm {@code id} nulo e {@code personalizada} falso.
 */
public record DataComemorativaResponse(UUID id, String nome, int dia, int mes, LocalDate proximaOcorrencia,
		long diasRestantes, boolean personalizada) {

}
