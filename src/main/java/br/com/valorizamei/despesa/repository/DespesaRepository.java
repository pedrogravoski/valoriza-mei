package br.com.valorizamei.despesa.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.valorizamei.despesa.model.ClassificacaoDespesa;
import br.com.valorizamei.despesa.model.Despesa;

/**
 * Todas as consultas usadas pelo service filtram pelo MEI ({@code tenantId}); os métodos
 * herdados sem filtro ({@code findById}, {@code findAll}, {@code deleteById}) não devem ser usados
 * (RNF07).
 */
public interface DespesaRepository extends JpaRepository<Despesa, UUID> {

	Optional<Despesa> findByIdAndTenantId(UUID id, UUID tenantId);

	List<Despesa> findAllByTenantId(UUID tenantId, Sort ordem);

	List<Despesa> findAllByTenantIdAndClassificacao(UUID tenantId, ClassificacaoDespesa classificacao, Sort ordem);

	boolean existsByTenantIdAndDescricaoIgnoreCase(UUID tenantId, String descricao);

	boolean existsByTenantIdAndDescricaoIgnoreCaseAndIdNot(UUID tenantId, String descricao, UUID id);

}
