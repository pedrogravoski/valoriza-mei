package br.com.valorizamei.despesa.dto;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * A despesa fixa exige o valor mensal e não aceita percentual; a variável exige o percentual e
 * não aceita valor mensal. Cada erro é apontado no campo correspondente.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidadorDeValorConformeTipo.class)
public @interface ValorConformeTipo {

	String message() default "valor incompatível com o tipo da despesa";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
