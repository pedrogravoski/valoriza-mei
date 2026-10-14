package br.com.valorizamei.arquitetura.fixtures.injecaoatributo.produto.service;

import org.springframework.beans.factory.annotation.Autowired;

import br.com.valorizamei.arquitetura.fixtures.injecaoatributo.produto.repository.ProdutoRepository;

public class ProdutoService {

	@Autowired
	private ProdutoRepository repository;

}
