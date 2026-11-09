package br.com.valorizamei.shared.seguranca;

/**
 * Token de acesso recém-emitido.
 *
 * @param valor o JWT assinado
 * @param validadeEmSegundos tempo até a expiração, contado a partir da emissão
 */
public record TokenEmitido(String valor, long validadeEmSegundos) {

}
