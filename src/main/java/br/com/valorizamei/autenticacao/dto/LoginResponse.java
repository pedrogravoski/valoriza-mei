package br.com.valorizamei.autenticacao.dto;

/**
 * Token de acesso devolvido no login, a ser enviado em {@code Authorization: Bearer <token>}.
 *
 * @param token o JWT assinado
 * @param tipo sempre "Bearer"
 * @param expiraEmSegundos validade do token, contada a partir do login
 */
public record LoginResponse(String token, String tipo, long expiraEmSegundos) {

}
