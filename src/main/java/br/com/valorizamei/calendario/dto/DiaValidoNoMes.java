package br.com.valorizamei.calendario.dto;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * O dia precisa existir no mês em todos os anos: 31/04 é recusado e fevereiro vai até 28, para
 * que a data se repita todo ano. O erro é apontado no campo {@code dia}.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidadorDeDiaValidoNoMes.class)
public @interface DiaValidoNoMes {

	String message() default "o dia não existe em todos os anos neste mês";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
