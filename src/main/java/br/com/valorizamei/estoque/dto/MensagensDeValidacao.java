package br.com.valorizamei.estoque.dto;

/**
 * Mensagens próprias de validação desta feature, no lugar das mensagens padrão da biblioteca.
 */
final class MensagensDeValidacao {

	static final String DUAS_CASAS = "informe o valor com no máximo 2 casas decimais";

	static final String DATA_FUTURA = "a data não pode ser futura";

	static final String VALOR_DA_VENDA_OBRIGATORIO = "informe o valor unitário da venda";

	static final String ENTRADA_USA_O_CUSTO = "não se aplica à entrada, que usa o custo direto do produto";

	private MensagensDeValidacao() {
	}

}
