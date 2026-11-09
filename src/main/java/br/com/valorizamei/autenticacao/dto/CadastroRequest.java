package br.com.valorizamei.autenticacao.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dados para criar a conta de um MEI. Os espaços nas extremidades do e-mail são removidos
 * antes da validação.
 */
public record CadastroRequest(
		@NotBlank @Size(max = 120) String nomeNegocio,
		@NotBlank @Size(max = 120) String nome,
		@NotBlank @Email @Size(max = 254) String email,
		@NotBlank @Size(min = 8, max = 72) String senha) {

	public CadastroRequest {
		email = email == null ? null : email.strip();
	}

}
