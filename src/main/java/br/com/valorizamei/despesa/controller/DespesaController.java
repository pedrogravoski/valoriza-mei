package br.com.valorizamei.despesa.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.valorizamei.despesa.dto.DespesaRequest;
import br.com.valorizamei.despesa.dto.DespesaResponse;
import br.com.valorizamei.despesa.dto.RateioResponse;
import br.com.valorizamei.despesa.dto.TipoDespesa;
import br.com.valorizamei.despesa.service.DespesaService;
import br.com.valorizamei.despesa.service.RateioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping(DespesaController.CAMINHO)
@Tag(name = "Despesas")
public class DespesaController {

	static final String CAMINHO = "/api/v1/despesas";

	private final DespesaService despesaService;

	private final RateioService rateioService;

	public DespesaController(DespesaService despesaService, RateioService rateioService) {
		this.despesaService = despesaService;
		this.rateioService = rateioService;
	}

	@PostMapping
	@Operation(summary = "Registra uma despesa fixa ou variável",
			description = "A despesa FIXA informa só o valorMensal, em reais; a VARIAVEL, só o percentual sobre "
					+ "o preço de venda, em pontos percentuais (3.00 = 3%).")
	public ResponseEntity<DespesaResponse> registrar(@Valid @RequestBody DespesaRequest despesa) {
		DespesaResponse registrada = despesaService.registrar(despesa);
		return ResponseEntity.created(URI.create(CAMINHO + "/" + registrada.id())).body(registrada);
	}

	@GetMapping
	@Operation(summary = "Lista as despesas do MEI pela descrição, com filtro opcional pelo tipo")
	public List<DespesaResponse> listar(
			@Parameter(description = "FIXA ou VARIAVEL; sem o parâmetro, lista todas")
			@RequestParam(required = false) TipoDespesa tipo) {
		return despesaService.listar(tipo);
	}

	@GetMapping("/rateio")
	@Operation(summary = "Calcula o rateio das despesas fixas sobre a receita mensal estimada",
			description = "DF = total das despesas fixas × 100 / receita mensal estimada; DV = soma dos percentuais "
					+ "das despesas variáveis. Sem receita informada, a receita e o DF vêm nulos.")
	public RateioResponse rateio() {
		return rateioService.consultar();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Consulta uma despesa do MEI")
	public DespesaResponse consultar(@PathVariable UUID id) {
		return despesaService.consultar(id);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Altera uma despesa do MEI, inclusive o tipo")
	public DespesaResponse alterar(@PathVariable UUID id, @Valid @RequestBody DespesaRequest despesa) {
		return despesaService.alterar(id, despesa);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Exclui uma despesa do MEI")
	public void excluir(@PathVariable UUID id) {
		despesaService.excluir(id);
	}

}
