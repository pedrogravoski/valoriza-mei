package br.com.valorizamei.calendario.controller;

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

import br.com.valorizamei.calendario.dto.DataComemorativaRequest;
import br.com.valorizamei.calendario.dto.DataComemorativaResponse;
import br.com.valorizamei.calendario.service.CalendarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping(DataComemorativaController.CAMINHO)
@Tag(name = "Datas comemorativas")
public class DataComemorativaController {

	static final String CAMINHO = "/api/v1/datas-comemorativas";

	private final CalendarioService calendarioService;

	public DataComemorativaController(CalendarioService calendarioService) {
		this.calendarioService = calendarioService;
	}

	@GetMapping
	@Operation(summary = "Lista as datas comemorativas fixas e as personalizadas do MEI",
			description = "Ordenadas pela próxima ocorrência. As fixas (Dia das Mães, Dia dos Pais, Dia das "
					+ "Crianças, Black Friday e Natal) têm id nulo e não podem ser alteradas.")
	public List<DataComemorativaResponse> listar() {
		return calendarioService.calendario();
	}

	@PostMapping
	@Operation(summary = "Cadastra uma data comemorativa própria do MEI, repetida todo ano",
			description = "Fevereiro aceita até o dia 28, para que a data exista todo ano.")
	public ResponseEntity<DataComemorativaResponse> cadastrar(@Valid @RequestBody DataComemorativaRequest data) {
		DataComemorativaResponse cadastrada = calendarioService.cadastrar(data);
		return ResponseEntity.created(URI.create(CAMINHO + "/" + cadastrada.id())).body(cadastrada);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Consulta uma data comemorativa personalizada do MEI")
	public DataComemorativaResponse consultar(@PathVariable UUID id) {
		return calendarioService.consultar(id);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Altera o nome, o dia e o mês de uma data comemorativa personalizada")
	public DataComemorativaResponse alterar(@PathVariable UUID id, @Valid @RequestBody DataComemorativaRequest data) {
		return calendarioService.alterar(id, data);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Exclui uma data comemorativa personalizada do MEI")
	public void excluir(@PathVariable UUID id) {
		calendarioService.excluir(id);
	}

}
