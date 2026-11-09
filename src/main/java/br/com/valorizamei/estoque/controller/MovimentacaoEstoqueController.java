package br.com.valorizamei.estoque.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.valorizamei.estoque.dto.MovimentacaoRegistradaResponse;
import br.com.valorizamei.estoque.dto.MovimentacaoRequest;
import br.com.valorizamei.estoque.dto.MovimentacaoResponse;
import br.com.valorizamei.estoque.service.EstoqueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping(MovimentacaoEstoqueController.CAMINHO)
@Tag(name = "Estoque")
public class MovimentacaoEstoqueController {

	static final String CAMINHO = "/api/v1/estoque/movimentacoes";

	private final EstoqueService estoqueService;

	public MovimentacaoEstoqueController(EstoqueService estoqueService) {
		this.estoqueService = estoqueService;
	}

	@PostMapping
	@Operation(summary = "Registra uma entrada (compra) ou saída (venda) de estoque",
			description = "A ENTRADA usa o custo direto do produto e gera uma saída no fluxo de caixa; a SAIDA "
					+ "informa o valorUnitario da venda e gera uma entrada no caixa. Sem data, usa a data atual.")
	@ApiResponse(responseCode = "201", description = "Movimentação registrada, com o estoque atual do produto")
	@ApiResponse(responseCode = "404", description = "Produto não encontrado no MEI")
	@ApiResponse(responseCode = "422", description = "Venda maior que o estoque atual do produto")
	public ResponseEntity<MovimentacaoRegistradaResponse> registrar(
			@Valid @RequestBody MovimentacaoRequest movimentacao) {
		MovimentacaoRegistradaResponse registrada = estoqueService.registrar(movimentacao);
		return ResponseEntity.created(URI.create(CAMINHO + "/" + registrada.id())).body(registrada);
	}

	@GetMapping
	@Operation(summary = "Lista as movimentações do MEI, da mais recente para a mais antiga")
	public List<MovimentacaoResponse> listar(
			@Parameter(description = "Filtra pelas movimentações de um produto")
			@RequestParam(required = false) UUID produtoId) {
		return estoqueService.listarMovimentacoes(produtoId);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Consulta uma movimentação de estoque do MEI")
	public MovimentacaoResponse consultar(@PathVariable UUID id) {
		return estoqueService.consultarMovimentacao(id);
	}

}
