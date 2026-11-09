package br.com.valorizamei.despesa.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Valida {@link ValorConformeTipo}. Sem tipo informado não há o que comparar: a ausência do
 * tipo já é apontada pelo {@code @NotNull}.
 */
class ValidadorDeValorConformeTipo implements ConstraintValidator<ValorConformeTipo, DespesaRequest> {

	private static final String VALOR_MENSAL = "valorMensal";

	private static final String PERCENTUAL = "percentual";

	@Override
	public boolean isValid(DespesaRequest despesa, ConstraintValidatorContext contexto) {
		if (despesa == null || despesa.tipo() == null) {
			return true;
		}
		contexto.disableDefaultConstraintViolation();
		boolean fixa = despesa.tipo() == TipoDespesa.FIXA;
		boolean valido = true;
		if (fixa && despesa.valorMensal() == null) {
			valido = apontar(contexto, VALOR_MENSAL, MensagensDeValidacao.VALOR_MENSAL_OBRIGATORIO);
		}
		if (fixa && despesa.percentual() != null) {
			valido = apontar(contexto, PERCENTUAL, MensagensDeValidacao.NAO_SE_APLICA);
		}
		if (!fixa && despesa.percentual() == null) {
			valido = apontar(contexto, PERCENTUAL, MensagensDeValidacao.PERCENTUAL_OBRIGATORIO);
		}
		if (!fixa && despesa.valorMensal() != null) {
			valido = apontar(contexto, VALOR_MENSAL, MensagensDeValidacao.NAO_SE_APLICA);
		}
		return valido;
	}

	/** Registra o erro no campo informado e devolve {@code false}. */
	private static boolean apontar(ConstraintValidatorContext contexto, String campo, String mensagem) {
		contexto.buildConstraintViolationWithTemplate(mensagem).addPropertyNode(campo).addConstraintViolation();
		return false;
	}

}
