package br.com.valorizamei.estoque.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Estoque atual de um produto (entradas menos saídas), nunca negativo. É alterado somente pelas
 * atualizações atômicas do {@code SaldoEstoqueRepository}, na mesma transação da movimentação;
 * por isso a entidade só é lida.
 */
@Entity
@Table(name = "saldo_estoque")
public class SaldoEstoque {

	@Id
	@Column(name = "produto_id")
	private UUID produtoId;

	@Column(name = "tenant_id", nullable = false)
	private UUID tenantId;

	@Column(name = "quantidade", nullable = false)
	private int quantidade;

	protected SaldoEstoque() {
	}

	public UUID getProdutoId() {
		return produtoId;
	}

	public UUID getTenantId() {
		return tenantId;
	}

	public int getQuantidade() {
		return quantidade;
	}

}
