package br.com.valorizamei.calendario.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.valorizamei.calendario.model.DataComemorativa;

/**
 * Todas as consultas usadas pelo service filtram pelo MEI ({@code tenantId}); os métodos
 * herdados sem filtro ({@code findById}, {@code findAll}, {@code deleteById}) não devem ser usados
 * (RNF07).
 */
public interface DataComemorativaRepository extends JpaRepository<DataComemorativa, UUID> {

	Optional<DataComemorativa> findByIdAndTenantId(UUID id, UUID tenantId);

	List<DataComemorativa> findAllByTenantId(UUID tenantId);

	boolean existsByTenantIdAndNomeIgnoreCase(UUID tenantId, String nome);

	boolean existsByTenantIdAndNomeIgnoreCaseAndIdNot(UUID tenantId, String nome, UUID id);

}
