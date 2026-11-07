package br.com.valorizamei.despesa.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.valorizamei.despesa.dto.ParametrosFinanceirosRequest;
import br.com.valorizamei.despesa.dto.ParametrosFinanceirosResponse;
import br.com.valorizamei.despesa.service.ParametrosFinanceirosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/parametros-financeiros")
@Tag(name = "Parâmetros financeiros")
public class ParametrosFinanceirosController {

	private final ParametrosFinanceirosService parametrosService;

	public ParametrosFinanceirosController(ParametrosFinanceirosService parametrosService) {
		this.parametrosService = parametrosService;
	}

	@GetMapping
	@Operation(summary = "Consulta a receita mensal estimada e a margem de lucro do MEI",
			description = "Os dois valores vêm nulos enquanto não forem informados. A margem é em pontos "
					+ "percentuais (25.00 = 25%).")
	public ParametrosFinanceirosResponse consultar() {
		return parametrosService.consultar();
	}

	@PutMapping
	@Operation(summary = "Informa a receita mensal estimada e a margem de lucro do MEI",
			description = "Cria os parâmetros na primeira vez e os substitui nas seguintes. A margem é em pontos "
					+ "percentuais (25.00 = 25%).")
	public ParametrosFinanceirosResponse gravar(@Valid @RequestBody ParametrosFinanceirosRequest parametros) {
		return parametrosService.gravar(parametros);
	}

}
