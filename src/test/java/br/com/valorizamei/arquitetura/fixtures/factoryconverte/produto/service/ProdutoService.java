package br.com.valorizamei.arquitetura.fixtures.factoryconverte.produto.service;

import br.com.valorizamei.arquitetura.fixtures.factoryconverte.produto.dto.ProdutoRequest;
import br.com.valorizamei.arquitetura.fixtures.factoryconverte.produto.dto.ProdutoResponse;
import br.com.valorizamei.arquitetura.fixtures.factoryconverte.produto.factory.ProdutoFactory;

public class ProdutoService {

	private final ProdutoFactory factory;

	public ProdutoService(ProdutoFactory factory) {
		this.factory = factory;
	}

	public ProdutoResponse cadastrar(ProdutoRequest request) {
		return factory.criarResponse(factory.criarModel(request));
	}

}
