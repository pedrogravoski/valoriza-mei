package br.com.valorizamei.precificacao.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.valorizamei.precificacao.dto.PrecificacaoDeProdutoResponse;
import br.com.valorizamei.precificacao.dto.PrecificacaoResponse;
import br.com.valorizamei.precificacao.service.PrecificacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/precificacao")
@Tag(name = "Precificação")
public class PrecificacaoController {

	private static final String FORMULAS = "Markup = 100 / [100 - (DV + DF + ML)], em pontos percentuais, exibido "
			+ "com 4 casas; PV = custo direto × markup, arredondado só ao final, para 2 casas.";

	private static final String SEM_MARKUP = "Parâmetros financeiros não informados, ou soma DV + DF + ML "
			+ "maior ou igual a 100%";

	private final PrecificacaoService precificacaoService;

	public PrecificacaoController(PrecificacaoService precificacaoService) {
		this.precificacaoService = precificacaoService;
	}

	@GetMapping
	@Operation(summary = "Calcula o índice de markup e o preço de venda sugerido dos produtos do MEI",
			description = FORMULAS)
	@ApiResponse(responseCode = "200", description = "Percentuais, markup e preços sugeridos")
	@ApiResponse(responseCode = "422", description = SEM_MARKUP)
	public PrecificacaoResponse consultar() {
		return precificacaoService.consultar();
	}

	@GetMapping("/produtos/{id}")
	@Operation(summary = "Calcula o preço de venda sugerido de um produto do MEI", description = FORMULAS)
	@ApiResponse(responseCode = "200", description = "Markup e preço sugerido do produto")
	@ApiResponse(responseCode = "404", description = "Produto não encontrado no MEI")
	@ApiResponse(responseCode = "422", description = SEM_MARKUP)
	public PrecificacaoDeProdutoResponse consultarProduto(@PathVariable UUID id) {
		return precificacaoService.consultarProduto(id);
	}

}
