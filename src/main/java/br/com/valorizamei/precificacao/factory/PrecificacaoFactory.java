package br.com.valorizamei.precificacao.factory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Component;

import br.com.valorizamei.precificacao.dto.PrecificacaoDeProdutoResponse;
import br.com.valorizamei.precificacao.dto.PrecificacaoResponse;
import br.com.valorizamei.precificacao.dto.PrecoSugeridoResponse;
import br.com.valorizamei.produto.dto.ProdutoResponse;

@Component
public class PrecificacaoFactory {

	/** Os percentuais chegam sem arredondamento e são exibidos com 2 casas, como no rateio. */
	public PrecificacaoResponse paraPrecificacao(BigDecimal percentualDespesasVariaveis,
			BigDecimal percentualDespesasFixas, BigDecimal margemDeLucro, BigDecimal indiceMarkup,
			List<PrecoSugeridoResponse> produtos) {
		return new PrecificacaoResponse(comDuasCasas(percentualDespesasVariaveis), comDuasCasas(percentualDespesasFixas),
				comDuasCasas(margemDeLucro), indiceMarkup, produtos);
	}

	public PrecoSugeridoResponse paraPrecoSugerido(ProdutoResponse produto, BigDecimal precoSugerido) {
		return new PrecoSugeridoResponse(produto.id(), produto.nome(), produto.custoDireto(), precoSugerido);
	}

	public PrecificacaoDeProdutoResponse paraPrecificacaoDeProduto(ProdutoResponse produto, BigDecimal indiceMarkup,
			BigDecimal precoSugerido) {
		return new PrecificacaoDeProdutoResponse(produto.id(), produto.nome(), produto.custoDireto(), indiceMarkup,
				precoSugerido);
	}

	private static BigDecimal comDuasCasas(BigDecimal percentual) {
		return percentual.setScale(2, RoundingMode.HALF_UP);
	}

}
