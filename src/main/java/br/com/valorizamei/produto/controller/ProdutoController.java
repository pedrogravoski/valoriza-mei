package br.com.valorizamei.produto.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.valorizamei.produto.dto.ProdutoRequest;
import br.com.valorizamei.produto.dto.ProdutoResponse;
import br.com.valorizamei.produto.service.ProdutoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping(ProdutoController.CAMINHO)
@Tag(name = "Produtos")
public class ProdutoController {

	static final String CAMINHO = "/api/v1/produtos";

	private final ProdutoService produtoService;

	public ProdutoController(ProdutoService produtoService) {
		this.produtoService = produtoService;
	}

	@PostMapping
	@Operation(summary = "Cadastra um produto com o custo direto de aquisição e o estoque mínimo")
	public ResponseEntity<ProdutoResponse> cadastrar(@Valid @RequestBody ProdutoRequest produto) {
		ProdutoResponse cadastrado = produtoService.cadastrar(produto);
		return ResponseEntity.created(URI.create(CAMINHO + "/" + cadastrado.id())).body(cadastrado);
	}

	@GetMapping
	@Operation(summary = "Lista os produtos do MEI em ordem alfabética")
	public List<ProdutoResponse> listar() {
		return produtoService.listar();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Consulta um produto do MEI")
	public ProdutoResponse consultar(@PathVariable UUID id) {
		return produtoService.consultar(id);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Altera o nome, o custo direto e o estoque mínimo de um produto")
	public ProdutoResponse alterar(@PathVariable UUID id, @Valid @RequestBody ProdutoRequest produto) {
		return produtoService.alterar(id, produto);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Exclui um produto do MEI")
	public void excluir(@PathVariable UUID id) {
		produtoService.excluir(id);
	}

}
