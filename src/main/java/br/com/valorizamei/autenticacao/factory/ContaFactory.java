package br.com.valorizamei.autenticacao.factory;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

import br.com.valorizamei.autenticacao.dto.CadastroRequest;
import br.com.valorizamei.autenticacao.dto.ContaResponse;
import br.com.valorizamei.autenticacao.model.Mei;
import br.com.valorizamei.autenticacao.model.Usuario;

@Component
public class ContaFactory {

	public Mei paraMei(CadastroRequest cadastro, Instant criadoEm) {
		return new Mei(cadastro.nomeNegocio().strip(), criadoEm);
	}

	public Usuario paraUsuario(CadastroRequest cadastro, UUID tenantId, String emailNormalizado, String senhaHash,
			Instant criadoEm) {
		return new Usuario(tenantId, cadastro.nome().strip(), emailNormalizado, senhaHash, criadoEm);
	}

	public ContaResponse paraResponse(Mei mei, Usuario usuario) {
		return new ContaResponse(mei.getNomeNegocio(), usuario.getNome(), usuario.getEmail());
	}

}
