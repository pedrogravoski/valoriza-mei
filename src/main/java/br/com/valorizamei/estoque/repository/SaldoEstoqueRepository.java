package br.com.valorizamei.estoque.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.valorizamei.estoque.model.SaldoEstoque;

/**
 * Atualiza o estoque atual de forma atômica no banco, sem ler e regravar o valor: duas
 * movimentações simultâneas do mesmo produto são serializadas pela linha do saldo, e a saída só
 * acontece se houver quantidade suficiente. Todas as operações filtram pelo MEI (RNF07).
 */
public interface SaldoEstoqueRepository extends JpaRepository<SaldoEstoque, UUID> {

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(nativeQuery = true, value = """
			insert into saldo_estoque (produto_id, tenant_id, quantidade)
			values (:produtoId, :tenantId, :quantidade)
			on conflict (produto_id) do update set quantidade = saldo_estoque.quantidade + excluded.quantidade
			""")
	int somar(@Param("produtoId") UUID produtoId, @Param("tenantId") UUID tenantId,
			@Param("quantidade") int quantidade);

	/** Devolve 0 quando o estoque é menor que a quantidade: nada é alterado. */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(nativeQuery = true, value = """
			update saldo_estoque set quantidade = quantidade - :quantidade
			where produto_id = :produtoId and tenant_id = :tenantId and quantidade >= :quantidade
			""")
	int subtrair(@Param("produtoId") UUID produtoId, @Param("tenantId") UUID tenantId,
			@Param("quantidade") int quantidade);

	Optional<SaldoEstoque> findByProdutoIdAndTenantId(UUID produtoId, UUID tenantId);

	List<SaldoEstoque> findAllByTenantId(UUID tenantId);

}
