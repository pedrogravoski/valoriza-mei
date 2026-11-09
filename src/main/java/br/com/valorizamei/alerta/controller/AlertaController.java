package br.com.valorizamei.alerta.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.valorizamei.alerta.dto.AlertasResponse;
import br.com.valorizamei.alerta.service.AlertaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/alertas")
@Tag(name = "Alertas")
public class AlertaController {

	private final AlertaService alertaService;

	public AlertaController(AlertaService alertaService) {
		this.alertaService = alertaService;
	}

	@GetMapping
	@Operation(summary = "Consulta os alertas de ruptura e de datas comemorativas do MEI",
			description = "Ruptura: estoque atual menor ou igual ao mínimo (mínimo maior que zero). Datas "
					+ "comemorativas dos próximos 30 dias. Com alguma data próxima, sugestões pelo giro dos últimos "
					+ "30 dias: REFORCAR_ESTOQUE (vendas maiores que o estoque), PROMOCAO (sem vendas e com "
					+ "estoque) e REAJUSTE_PRECO (preço médio de venda abaixo do sugerido).")
	public AlertasResponse consultar() {
		return alertaService.consultar();
	}

}
