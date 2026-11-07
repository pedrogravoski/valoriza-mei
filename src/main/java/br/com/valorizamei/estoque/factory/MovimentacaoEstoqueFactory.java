package br.com.valorizamei.estoque.factory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Component;

import br.com.valorizamei.estoque.dto.EstoqueProdutoResponse;
import br.com.valorizamei.estoque.dto.MovimentacaoRegistradaResponse;
import br.com.valorizamei.estoque.dto.MovimentacaoRequest;
import br.com.valorizamei.estoque.dto.MovimentacaoResponse;
import br.com.valorizamei.estoque.dto.TipoMovimentacao;
import br.com.valorizamei.estoque.model.MovimentacaoEstoque;
import br.com.valorizamei.estoque.model.SentidoMovimentacao;
import br.com.valorizamei.produto.dto.ProdutoResponse;

@Component
public class MovimentacaoEstoqueFactory {

	public MovimentacaoEstoque paraMovimentacao(MovimentacaoRequest dados, UUID tenantId, BigDecimal valorUnitario,
			LocalDate data, Instant criadoEm) {
		return new MovimentacaoEstoque(tenantId, dados.produtoId(), paraSentido(dados.tipo()), dados.quantidade(),
				valorUnitario.setScale(2, RoundingMode.UNNECESSARY), data, criadoEm);
	}

	public MovimentacaoResponse paraResponse(MovimentacaoEstoque movimentacao, String nomeProduto) {
		return new MovimentacaoResponse(movimentacao.getId(), movimentacao.getProdutoId(), nomeProduto,
				paraTipo(movimentacao.getSentido()), movimentacao.getQuantidade(), movimentacao.getValorUnitario(),
				movimentacao.getValorTotal(), movimentacao.getData());
	}

	public MovimentacaoRegistradaResponse paraRegistrada(MovimentacaoEstoque movimentacao, String nomeProduto,
			int estoqueAtual) {
		return new MovimentacaoRegistradaResponse(movimentacao.getId(), movimentacao.getProdutoId(), nomeProduto,
				paraTipo(movimentacao.getSentido()), movimentacao.getQuantidade(), movimentacao.getValorUnitario(),
				movimentacao.getValorTotal(), movimentacao.getData(), estoqueAtual);
	}

	public EstoqueProdutoResponse paraEstoque(ProdutoResponse produto, int estoqueAtual) {
		return new EstoqueProdutoResponse(produto.id(), produto.nome(), estoqueAtual, produto.estoqueMinimo());
	}

	public SentidoMovimentacao paraSentido(TipoMovimentacao tipo) {
		return SentidoMovimentacao.valueOf(tipo.name());
	}

	private static TipoMovimentacao paraTipo(SentidoMovimentacao sentido) {
		return TipoMovimentacao.valueOf(sentido.name());
	}

}
