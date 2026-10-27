package br.com.valorizamei.shared.erro;

/**
 * Item da propriedade {@code erros} de uma resposta de validação: o campo inválido e a
 * descrição do problema.
 */
public record ErroDeCampo(String campo, String mensagem) {
}
