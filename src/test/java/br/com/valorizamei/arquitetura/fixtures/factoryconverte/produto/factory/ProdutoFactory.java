package br.com.valorizamei.arquitetura.fixtures.factoryconverte.produto.factory;

import br.com.valorizamei.arquitetura.fixtures.factoryconverte.produto.dto.ProdutoRequest;
import br.com.valorizamei.arquitetura.fixtures.factoryconverte.produto.dto.ProdutoResponse;
import br.com.valorizamei.arquitetura.fixtures.factoryconverte.produto.model.Produto;

public class ProdutoFactory {

	public Produto criarModel(ProdutoRequest request) {
		return new Produto();
	}

	public ProdutoResponse criarResponse(Produto produto) {
		return new ProdutoResponse("produto");
	}

}
