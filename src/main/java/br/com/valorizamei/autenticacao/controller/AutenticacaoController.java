package br.com.valorizamei.autenticacao.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.valorizamei.autenticacao.dto.CadastroRequest;
import br.com.valorizamei.autenticacao.dto.ContaResponse;
import br.com.valorizamei.autenticacao.dto.LoginRequest;
import br.com.valorizamei.autenticacao.dto.LoginResponse;
import br.com.valorizamei.autenticacao.service.CadastroService;
import br.com.valorizamei.autenticacao.service.LoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Endpoints públicos: criar a conta e obter o token de acesso.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação")
public class AutenticacaoController {

	private final CadastroService cadastroService;

	private final LoginService loginService;

	public AutenticacaoController(CadastroService cadastroService, LoginService loginService) {
		this.cadastroService = cadastroService;
		this.loginService = loginService;
	}

	@PostMapping("/cadastro")
	@ResponseStatus(HttpStatus.CREATED)
	@SecurityRequirements
	@Operation(summary = "Cria a conta do MEI e o seu usuário")
	public ContaResponse cadastrar(@Valid @RequestBody CadastroRequest cadastro) {
		return cadastroService.cadastrar(cadastro);
	}

	@PostMapping("/login")
	@SecurityRequirements
	@Operation(summary = "Autentica por e-mail e senha e devolve o token de acesso")
	public LoginResponse entrar(@Valid @RequestBody LoginRequest login) {
		return loginService.entrar(login);
	}

}
