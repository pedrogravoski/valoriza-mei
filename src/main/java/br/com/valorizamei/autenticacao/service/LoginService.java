package br.com.valorizamei.autenticacao.service;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.valorizamei.autenticacao.dto.LoginRequest;
import br.com.valorizamei.autenticacao.dto.LoginResponse;
import br.com.valorizamei.autenticacao.model.Usuario;
import br.com.valorizamei.autenticacao.repository.UsuarioRepository;
import br.com.valorizamei.shared.erro.ErroDeNegocioException;
import br.com.valorizamei.shared.seguranca.EmissorDeToken;
import br.com.valorizamei.shared.seguranca.TokenEmitido;

/**
 * Autentica o usuário por e-mail e senha e emite o token de acesso (RF10).
 * <p>
 * E-mail não cadastrado e senha incorreta geram a mesma resposta, e a senha é comparada com
 * um hash mesmo quando o e-mail não existe, para que nem a mensagem nem o tempo de resposta
 * revelem quais e-mails estão cadastrados.
 */
@Service
public class LoginService {

	static final String TIPO_DO_TOKEN = "Bearer";

	private final UsuarioRepository usuarioRepository;

	private final PasswordEncoder codificadorDeSenha;

	private final EmissorDeToken emissorDeToken;

	private final String hashParaEmailInexistente;

	public LoginService(UsuarioRepository usuarioRepository, PasswordEncoder codificadorDeSenha,
			EmissorDeToken emissorDeToken) {
		this.usuarioRepository = usuarioRepository;
		this.codificadorDeSenha = codificadorDeSenha;
		this.emissorDeToken = emissorDeToken;
		this.hashParaEmailInexistente = codificadorDeSenha.encode("senha-de-comparacao-para-email-inexistente");
	}

	@Transactional(readOnly = true)
	public LoginResponse entrar(LoginRequest login) {
		Optional<Usuario> usuario = usuarioRepository.findByEmail(Emails.normalizar(login.email()));
		String hash = usuario.map(Usuario::getSenhaHash).orElse(hashParaEmailInexistente);
		boolean senhaConfere = codificadorDeSenha.matches(login.senha(), hash);
		if (usuario.isEmpty() || !senhaConfere) {
			throw new ErroDeNegocioException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas",
					"E-mail ou senha inválidos.");
		}
		TokenEmitido token = emissorDeToken.emitir(usuario.get().getId(), usuario.get().getTenantId());
		return new LoginResponse(token.valor(), TIPO_DO_TOKEN, token.validadeEmSegundos());
	}

}
