package br.com.valorizamei.autenticacao.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.valorizamei.autenticacao.dto.CadastroRequest;
import br.com.valorizamei.autenticacao.dto.ContaResponse;
import br.com.valorizamei.autenticacao.factory.ContaFactory;
import br.com.valorizamei.autenticacao.model.Mei;
import br.com.valorizamei.autenticacao.model.Usuario;
import br.com.valorizamei.autenticacao.repository.MeiRepository;
import br.com.valorizamei.autenticacao.repository.UsuarioRepository;
import br.com.valorizamei.shared.erro.ErroDeNegocioException;

/**
 * Cria a conta de um MEI: o MEI (tenant) e o seu usuário, na mesma transação (RF10). A senha
 * é gravada somente como hash BCrypt (RNF01).
 */
@Service
public class CadastroService {

	private final MeiRepository meiRepository;

	private final UsuarioRepository usuarioRepository;

	private final ContaFactory contaFactory;

	private final PasswordEncoder codificadorDeSenha;

	private final Clock relogio;

	public CadastroService(MeiRepository meiRepository, UsuarioRepository usuarioRepository,
			ContaFactory contaFactory, PasswordEncoder codificadorDeSenha, Clock relogio) {
		this.meiRepository = meiRepository;
		this.usuarioRepository = usuarioRepository;
		this.contaFactory = contaFactory;
		this.codificadorDeSenha = codificadorDeSenha;
		this.relogio = relogio;
	}

	@Transactional
	public ContaResponse cadastrar(CadastroRequest cadastro) {
		String email = Emails.normalizar(cadastro.email());
		if (usuarioRepository.existsByEmail(email)) {
			throw emailJaCadastrado();
		}
		Instant agora = relogio.instant();
		Mei mei = meiRepository.save(contaFactory.paraMei(cadastro, agora));
		Usuario usuario = contaFactory.paraUsuario(cadastro, mei.getId(), email,
				codificadorDeSenha.encode(cadastro.senha()), agora);
		try {
			usuarioRepository.saveAndFlush(usuario);
		}
		catch (DataIntegrityViolationException ex) {
			// Outro cadastro com o mesmo e-mail foi gravado entre a verificação e a gravação.
			throw emailJaCadastrado();
		}
		return contaFactory.paraResponse(mei, usuario);
	}

	private static ErroDeNegocioException emailJaCadastrado() {
		return new ErroDeNegocioException(HttpStatus.CONFLICT, "E-mail já cadastrado",
				"Já existe uma conta com este e-mail.");
	}

}
