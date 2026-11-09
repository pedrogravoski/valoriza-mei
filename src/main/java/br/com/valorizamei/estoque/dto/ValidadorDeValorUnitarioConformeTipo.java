package br.com.valorizamei.estoque.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Valida {@link ValorUnitarioConformeTipo}. Sem tipo informado não há o que comparar: a ausência
 * do tipo já é apontada pelo {@code @NotNull}.
 */
class ValidadorDeValorUnitarioConformeTipo
		implements ConstraintValidator<ValorUnitarioConformeTipo, MovimentacaoRequest> {

	private static final String VALOR_UNITARIO = "valorUnitario";

	@Override
	public boolean isValid(MovimentacaoRequest movimentacao, ConstraintValidatorContext contexto) {
		if (movimentacao == null || movimentacao.tipo() == null) {
			return true;
		}
		boolean saida = movimentacao.tipo() == TipoMovimentacao.SAIDA;
		boolean informado = movimentacao.valorUnitario() != null;
		if (saida == informado) {
			return true;
		}
		contexto.disableDefaultConstraintViolation();
		String mensagem = saida ? MensagensDeValidacao.VALOR_DA_VENDA_OBRIGATORIO
				: MensagensDeValidacao.ENTRADA_USA_O_CUSTO;
		contexto.buildConstraintViolationWithTemplate(mensagem).addPropertyNode(VALOR_UNITARIO)
			.addConstraintViolation();
		return false;
	}

}
