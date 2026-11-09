package br.com.valorizamei.calendario.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Data comemorativa própria do MEI, repetida todo ano no dia e mês informados. Os espaços nas
 * extremidades do nome são removidos antes da validação.
 */
@DiaValidoNoMes
public record DataComemorativaRequest(
		@NotBlank @Size(max = 80) String nome,
		@NotNull @Min(1) @Max(31) Integer dia,
		@NotNull @Min(1) @Max(12) Integer mes) {

	public DataComemorativaRequest {
		nome = nome == null ? null : nome.strip();
	}

}
