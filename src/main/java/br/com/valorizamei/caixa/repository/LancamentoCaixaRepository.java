package br.com.valorizamei.caixa.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.valorizamei.caixa.model.LancamentoCaixa;

/**
 * Todas as consultas usadas pelo service filtram pelo MEI ({@code tenantId}) (RNF07).
 */
public interface LancamentoCaixaRepository extends JpaRepository<LancamentoCaixa, UUID> {

	List<LancamentoCaixa> findAllByTenantIdAndDataBetween(UUID tenantId, LocalDate inicio, LocalDate fim, Sort ordem);

}
