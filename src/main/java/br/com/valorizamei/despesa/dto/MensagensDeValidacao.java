package br.com.valorizamei.despesa.dto;

/**
 * Mensagens próprias de validação desta feature, no lugar das mensagens padrão da biblioteca.
 */
final class MensagensDeValidacao {

	static final String DUAS_CASAS = "informe o valor com no máximo 2 casas decimais";

	static final String VALOR_MENSAL_OBRIGATORIO = "informe o valor mensal da despesa fixa";

	static final String PERCENTUAL_OBRIGATORIO = "informe o percentual da despesa variável";

	static final String NAO_SE_APLICA = "não se aplica a este tipo de despesa";

	private MensagensDeValidacao() {
	}

}
