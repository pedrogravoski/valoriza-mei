package br.com.valorizamei.autenticacao.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.valorizamei.autenticacao.model.Usuario;

/**
 * As consultas por e-mail recebem o e-mail já normalizado (sem espaços e em minúsculas).
 */
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

	Optional<Usuario> findByEmail(String email);

	boolean existsByEmail(String email);

	Optional<Usuario> findByTenantId(UUID tenantId);

}
