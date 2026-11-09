package br.com.valorizamei.estoque.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entrada (compra) ou saída (venda) de estoque de um produto do MEI (RF06). Na entrada, o valor
 * unitário é o custo direto do produto; na saída, o valor de venda informado. As movimentações
 * não são alteradas nem excluídas.
 */
@Entity
@Table(name = "movimentacao_estoque")
public class MovimentacaoEstoque {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "tenant_id", nullable = false)
	private UUID tenantId;

	@Column(name = "produto_id", nullable = false)
	private UUID produtoId;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 7)
	private SentidoMovimentacao sentido;

	@Column(name = "quantidade", nullable = false)
	private int quantidade;

	@Column(name = "valor_unitario", nullable = false, precision = 12, scale = 2)
	private BigDecimal valorUnitario;

	@Column(name = "valor_total", nullable = false, precision = 14, scale = 2)
	private BigDecimal valorTotal;

	@Column(name = "data", nullable = false)
	private LocalDate data;

	@Column(name = "criado_em", nullable = false)
	private Instant criadoEm;

	protected MovimentacaoEstoque() {
	}

	public MovimentacaoEstoque(UUID tenantId, UUID produtoId, SentidoMovimentacao sentido, int quantidade,
			BigDecimal valorUnitario, LocalDate data, Instant criadoEm) {
		this.tenantId = tenantId;
		this.produtoId = produtoId;
		this.sentido = sentido;
		this.quantidade = quantidade;
		this.valorUnitario = valorUnitario;
		this.valorTotal = valorUnitario.multiply(BigDecimal.valueOf(quantidade));
		this.data = data;
		this.criadoEm = criadoEm;
	}

	public UUID getId() {
		return id;
	}

	public UUID getTenantId() {
		return tenantId;
	}

	public UUID getProdutoId() {
		return produtoId;
	}

	public SentidoMovimentacao getSentido() {
		return sentido;
	}

	public int getQuantidade() {
		return quantidade;
	}

	public BigDecimal getValorUnitario() {
		return valorUnitario;
	}

	public BigDecimal getValorTotal() {
		return valorTotal;
	}

	public LocalDate getData() {
		return data;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}

}
