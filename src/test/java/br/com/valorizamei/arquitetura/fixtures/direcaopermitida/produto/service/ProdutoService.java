package br.com.valorizamei.arquitetura.fixtures.direcaopermitida.produto.service;

public class ProdutoService {

	private final br.com.valorizamei.arquitetura.fixtures.direcaopermitida.produto.repository.ProdutoRepository dep0;

	private final br.com.valorizamei.arquitetura.fixtures.direcaopermitida.produto.model.Produto dep1;

	private final br.com.valorizamei.arquitetura.fixtures.direcaopermitida.produto.factory.ProdutoFactory dep2;

	public ProdutoService(br.com.valorizamei.arquitetura.fixtures.direcaopermitida.produto.repository.ProdutoRepository dep0, br.com.valorizamei.arquitetura.fixtures.direcaopermitida.produto.model.Produto dep1, br.com.valorizamei.arquitetura.fixtures.direcaopermitida.produto.factory.ProdutoFactory dep2) {
		this.dep0 = dep0;
		this.dep1 = dep1;
		this.dep2 = dep2;
	}

}
