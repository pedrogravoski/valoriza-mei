package br.com.valorizamei.calendario.dto;

import java.time.Month;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Valida {@link DiaValidoNoMes} com o tamanho do mês em ano não bissexto. Dia ou mês ausente ou
 * fora do intervalo já é apontado pelas anotações dos próprios campos.
 */
class ValidadorDeDiaValidoNoMes implements ConstraintValidator<DiaValidoNoMes, DataComemorativaRequest> {

	@Override
	public boolean isValid(DataComemorativaRequest data, ConstraintValidatorContext contexto) {
		if (data == null || data.dia() == null || data.mes() == null || data.mes() < 1 || data.mes() > 12) {
			return true;
		}
		if (data.dia() <= Month.of(data.mes()).length(false)) {
			return true;
		}
		contexto.disableDefaultConstraintViolation();
		contexto.buildConstraintViolationWithTemplate(contexto.getDefaultConstraintMessageTemplate())
			.addPropertyNode("dia")
			.addConstraintViolation();
		return false;
	}

}
