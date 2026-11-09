package br.com.valorizamei.alerta.dto;

/**
 * Sugestão para um produto diante de uma data comemorativa próxima (RF09), com base no giro dos
 * últimos 30 dias.
 */
public enum TipoSugestao {

	/** Vendas maiores que o estoque atual. */
	REFORCAR_ESTOQUE,

	/** Nenhuma venda, com estoque maior que zero. */
	PROMOCAO,

	/** Preço médio de venda abaixo do preço sugerido pela precificação. */
	REAJUSTE_PRECO

}
