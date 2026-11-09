package br.com.valorizamei.produto.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.valorizamei.produto.model.Produto;

/**
 * Todas as consultas usadas pelo service filtram pelo MEI ({@code tenantId}); os métodos
 * herdados sem filtro ({@code findById}, {@code findAll}, {@code deleteById}) não devem ser usados
 * (RNF07).
 */
public interface ProdutoRepository extends JpaRepository<Produto, UUID> {

	Optional<Produto> findByIdAndTenantId(UUID id, UUID tenantId);

	List<Produto> findAllByTenantId(UUID tenantId, Sort ordem);

	boolean existsByTenantIdAndNomeIgnoreCase(UUID tenantId, String nome);

	boolean existsByTenantIdAndNomeIgnoreCaseAndIdNot(UUID tenantId, String nome, UUID id);

}
