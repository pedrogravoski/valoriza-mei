package br.com.valorizamei.autenticacao.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.valorizamei.autenticacao.dto.ContaResponse;
import br.com.valorizamei.autenticacao.service.ContaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/conta")
@Tag(name = "Conta")
public class ContaController {

	private final ContaService contaService;

	public ContaController(ContaService contaService) {
		this.contaService = contaService;
	}

	@GetMapping
	@Operation(summary = "Consulta a conta do MEI autenticado")
	public ContaResponse consultar() {
		return contaService.consultar();
	}

}
