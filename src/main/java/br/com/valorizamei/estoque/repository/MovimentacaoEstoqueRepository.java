package br.com.valorizamei.estoque.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.valorizamei.estoque.model.MovimentacaoEstoque;
import br.com.valorizamei.estoque.model.SentidoMovimentacao;
import br.com.valorizamei.estoque.model.VendasDoProduto;

/**
 * Todas as consultas usadas pelo service filtram pelo MEI ({@code tenantId}) (RNF07).
 */
public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, UUID> {

	Optional<MovimentacaoEstoque> findByIdAndTenantId(UUID id, UUID tenantId);

	List<MovimentacaoEstoque> findAllByTenantId(UUID tenantId, Sort ordem);

	List<MovimentacaoEstoque> findAllByTenantIdAndProdutoId(UUID tenantId, UUID produtoId, Sort ordem);

	@Query("""
			select coalesce(sum(m.quantidade), 0) from MovimentacaoEstoque m
			where m.tenantId = :tenantId and m.produtoId = :produtoId and m.sentido = :sentido
			and m.data between :de and :ate
			""")
	long somarQuantidade(@Param("tenantId") UUID tenantId, @Param("produtoId") UUID produtoId,
			@Param("sentido") SentidoMovimentacao sentido, @Param("de") LocalDate de, @Param("ate") LocalDate ate);

	@Query("""
			select new br.com.valorizamei.estoque.model.VendasDoProduto(m.produtoId, sum(m.quantidade), sum(m.valorTotal))
			from MovimentacaoEstoque m
			where m.tenantId = :tenantId and m.sentido = :sentido and m.data between :de and :ate
			group by m.produtoId
			""")
	List<VendasDoProduto> somarPorProduto(@Param("tenantId") UUID tenantId,
			@Param("sentido") SentidoMovimentacao sentido, @Param("de") LocalDate de, @Param("ate") LocalDate ate);

}
