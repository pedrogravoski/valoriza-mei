package br.com.valorizamei.autenticacao.dto;

/**
 * Dados da conta de um MEI, sem senha nem hash.
 */
public record ContaResponse(String nomeNegocio, String nome, String email) {

}
