package br.com.valorizamei.produto.factory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

import br.com.valorizamei.produto.dto.ProdutoRequest;
import br.com.valorizamei.produto.dto.ProdutoResponse;
import br.com.valorizamei.produto.model.Produto;

@Component
public class ProdutoFactory {

	private static final int CASAS_DO_CUSTO = 2;

	public Produto paraProduto(ProdutoRequest dados, UUID tenantId, Instant criadoEm) {
		return new Produto(tenantId, dados.nome(), custo(dados), dados.estoqueMinimo(), criadoEm);
	}

	public void atualizar(Produto produto, ProdutoRequest dados, Instant atualizadoEm) {
		produto.alterar(dados.nome(), custo(dados), dados.estoqueMinimo(), atualizadoEm);
	}

	public ProdutoResponse paraResponse(Produto produto) {
		return new ProdutoResponse(produto.getId(), produto.getNome(), produto.getCustoDireto(),
				produto.getEstoqueMinimo());
	}

	/** A validação garante no máximo 2 casas; aqui só se completa a escala (180 vira 180.00). */
	private static BigDecimal custo(ProdutoRequest dados) {
		return dados.custoDireto().setScale(CASAS_DO_CUSTO, RoundingMode.UNNECESSARY);
	}

}
