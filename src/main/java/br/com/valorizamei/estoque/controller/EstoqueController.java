package br.com.valorizamei.estoque.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.valorizamei.estoque.dto.EstoqueProdutoResponse;
import br.com.valorizamei.estoque.service.EstoqueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/estoque")
@Tag(name = "Estoque")
public class EstoqueController {

	private final EstoqueService estoqueService;

	public EstoqueController(EstoqueService estoqueService) {
		this.estoqueService = estoqueService;
	}

	@GetMapping
	@Operation(summary = "Lista o estoque atual e o estoque mínimo de todos os produtos do MEI",
			description = "Produtos sem movimentações aparecem com estoque atual 0.")
	public List<EstoqueProdutoResponse> consultar() {
		return estoqueService.estoqueAtual();
	}

}
