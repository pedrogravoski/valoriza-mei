package br.com.valorizamei.despesa.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.valorizamei.despesa.model.ParametrosFinanceiros;

/**
 * A chave é o próprio {@code tenant_id}: o service só consulta e grava com o id do MEI do
 * token (RNF07).
 */
public interface ParametrosFinanceirosRepository extends JpaRepository<ParametrosFinanceiros, UUID> {

}
