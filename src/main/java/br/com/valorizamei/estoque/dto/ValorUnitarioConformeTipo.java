package br.com.valorizamei.estoque.dto;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * A saída (venda) exige o valor unitário; a entrada (compra) não o aceita, porque usa o custo
 * direto do produto. O erro é apontado no campo {@code valorUnitario}.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidadorDeValorUnitarioConformeTipo.class)
public @interface ValorUnitarioConformeTipo {

	String message() default "valor unitário incompatível com o tipo da movimentação";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
