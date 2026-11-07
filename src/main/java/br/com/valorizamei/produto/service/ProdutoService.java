package br.com.valorizamei.produto.service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.valorizamei.produto.dto.ProdutoRequest;
import br.com.valorizamei.produto.dto.ProdutoResponse;
import br.com.valorizamei.produto.factory.ProdutoFactory;
import br.com.valorizamei.produto.model.Produto;
import br.com.valorizamei.produto.repository.ProdutoRepository;
import br.com.valorizamei.shared.erro.ErroDeNegocioException;
import br.com.valorizamei.shared.seguranca.TenantAtual;

/**
 * Cadastro dos produtos do MEI com o custo direto de aquisição (RF01). Toda operação considera
 * somente os produtos do MEI do token: um produto de outro MEI é tratado como inexistente (RNF07).
 */
@Service
public class ProdutoService {

	private static final Sort POR_NOME = Sort.by(Sort.Order.asc("nome").ignoreCase());

	private final ProdutoRepository produtoRepository;

	private final ProdutoFactory produtoFactory;

	private final TenantAtual tenantAtual;

	private final Clock relogio;

	public ProdutoService(ProdutoRepository produtoRepository, ProdutoFactory produtoFactory,
			TenantAtual tenantAtual, Clock relogio) {
		this.produtoRepository = produtoRepository;
		this.produtoFactory = produtoFactory;
		this.tenantAtual = tenantAtual;
		this.relogio = relogio;
	}

	@Transactional
	public ProdutoResponse cadastrar(ProdutoRequest dados) {
		UUID tenantId = tenantAtual.id();
		if (produtoRepository.existsByTenantIdAndNomeIgnoreCase(tenantId, dados.nome())) {
			throw produtoJaCadastrado();
		}
		Produto produto = produtoFactory.paraProduto(dados, tenantId, relogio.instant());
		return produtoFactory.paraResponse(gravar(produto));
	}

	@Transactional(readOnly = true)
	public List<ProdutoResponse> listar() {
		return produtoRepository.findAllByTenantId(tenantAtual.id(), POR_NOME).stream()
			.map(produtoFactory::paraResponse)
			.toList();
	}

	@Transactional(readOnly = true)
	public ProdutoResponse consultar(UUID id) {
		return produtoFactory.paraResponse(buscarDoMei(id));
	}

	@Transactional
	public ProdutoResponse alterar(UUID id, ProdutoRequest dados) {
		Produto produto = buscarDoMei(id);
		if (produtoRepository.existsByTenantIdAndNomeIgnoreCaseAndIdNot(produto.getTenantId(), dados.nome(), id)) {
			throw produtoJaCadastrado();
		}
		produtoFactory.atualizar(produto, dados, relogio.instant());
		return produtoFactory.paraResponse(gravar(produto));
	}

	@Transactional
	public void excluir(UUID id) {
		produtoRepository.delete(buscarDoMei(id));
	}

	private Produto buscarDoMei(UUID id) {
		return produtoRepository.findByIdAndTenantId(id, tenantAtual.id())
			.orElseThrow(ProdutoService::produtoNaoEncontrado);
	}

	private Produto gravar(Produto produto) {
		try {
			return produtoRepository.saveAndFlush(produto);
		}
		catch (DataIntegrityViolationException ex) {
			// Outro produto com o mesmo nome foi gravado entre a verificação e a gravação.
			throw produtoJaCadastrado();
		}
	}

	private static ErroDeNegocioException produtoNaoEncontrado() {
		return new ErroDeNegocioException(HttpStatus.NOT_FOUND, "Produto não encontrado",
				"Não existe produto com este identificador.");
	}

	private static ErroDeNegocioException produtoJaCadastrado() {
		return new ErroDeNegocioException(HttpStatus.CONFLICT, "Produto já cadastrado",
				"Já existe um produto com este nome.");
	}

}
