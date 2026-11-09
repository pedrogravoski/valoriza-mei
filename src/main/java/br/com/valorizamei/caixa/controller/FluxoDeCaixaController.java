package br.com.valorizamei.caixa.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.valorizamei.caixa.dto.FluxoDeCaixaResponse;
import br.com.valorizamei.caixa.service.CaixaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/fluxo-de-caixa")
@Tag(name = "Fluxo de caixa")
public class FluxoDeCaixaController {

	private final CaixaService caixaService;

	public FluxoDeCaixaController(CaixaService caixaService) {
		this.caixaService = caixaService;
	}

	@GetMapping
	@Operation(summary = "Consulta o fluxo de caixa do MEI no período",
			description = "Compras de mercadoria (entradas de estoque) aparecem como saída de caixa e vendas, como "
					+ "entrada. Sem período informado, usa o mês atual.")
	public FluxoDeCaixaResponse consultar(
			@Parameter(description = "Data inicial (AAAA-MM-DD), inclusiva; padrão: primeiro dia do mês atual")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
			@Parameter(description = "Data final (AAAA-MM-DD), inclusiva; padrão: último dia do mês atual")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
		return caixaService.consultar(inicio, fim);
	}

}
