package br.com.valorizamei.autenticacao.service;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.valorizamei.autenticacao.dto.ContaResponse;
import br.com.valorizamei.autenticacao.factory.ContaFactory;
import br.com.valorizamei.autenticacao.model.Mei;
import br.com.valorizamei.autenticacao.model.Usuario;
import br.com.valorizamei.autenticacao.repository.MeiRepository;
import br.com.valorizamei.autenticacao.repository.UsuarioRepository;
import br.com.valorizamei.shared.erro.ErroDeNegocioException;
import br.com.valorizamei.shared.seguranca.TenantAtual;
import br.com.valorizamei.shared.seguranca.VerificadorDeTenant;

/**
 * Conta do MEI autenticado, identificado exclusivamente pelo token (RF10, RNF07). Também
 * confirma, para o filtro de segurança, que o MEI de um token existe.
 */
@Service
public class ContaService implements VerificadorDeTenant {

	private final MeiRepository meiRepository;

	private final UsuarioRepository usuarioRepository;

	private final ContaFactory contaFactory;

	private final TenantAtual tenantAtual;

	public ContaService(MeiRepository meiRepository, UsuarioRepository usuarioRepository,
			ContaFactory contaFactory, TenantAtual tenantAtual) {
		this.meiRepository = meiRepository;
		this.usuarioRepository = usuarioRepository;
		this.contaFactory = contaFactory;
		this.tenantAtual = tenantAtual;
	}

	@Transactional(readOnly = true)
	public ContaResponse consultar() {
		UUID tenantId = tenantAtual.id();
		Mei mei = meiRepository.findById(tenantId).orElseThrow(ContaService::contaInexistente);
		Usuario usuario = usuarioRepository.findByTenantId(tenantId).orElseThrow(ContaService::contaInexistente);
		return contaFactory.paraResponse(mei, usuario);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existe(UUID tenantId) {
		return meiRepository.existsById(tenantId);
	}

	private static ErroDeNegocioException contaInexistente() {
		return new ErroDeNegocioException(HttpStatus.UNAUTHORIZED, "Não autenticado",
				"É necessário enviar um token de acesso válido.");
	}

}
