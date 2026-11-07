package br.com.valorizamei.autenticacao.service;

import java.util.Locale;

/**
 * O e-mail identifica o usuário sem diferenciar maiúsculas de minúsculas nem considerar
 * espaços nas extremidades; ele é gravado e consultado sempre nesta forma.
 */
final class Emails {

	private Emails() {
	}

	static String normalizar(String email) {
		return email.strip().toLowerCase(Locale.ROOT);
	}

}
