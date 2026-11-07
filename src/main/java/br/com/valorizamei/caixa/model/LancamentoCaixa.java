package br.com.valorizamei.caixa.model;

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
 * Lançamento no fluxo de caixa de um MEI (RF07), gerado por uma movimentação de estoque. Os
 * lançamentos não são alterados nem excluídos.
 */
@Entity
@Table(name = "lancamento_caixa")
public class LancamentoCaixa {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "tenant_id", nullable = false)
	private UUID tenantId;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 7)
	private SentidoLancamento sentido;

	@Column(name = "descricao", nullable = false, length = 200)
	private String descricao;

	@Column(name = "valor", nullable = false, precision = 14, scale = 2)
	private BigDecimal valor;

	@Column(name = "data", nullable = false)
	private LocalDate data;

	@Column(name = "movimentacao_estoque_id", unique = true)
	private UUID movimentacaoEstoqueId;

	@Column(name = "criado_em", nullable = false)
	private Instant criadoEm;

	protected LancamentoCaixa() {
	}

	public LancamentoCaixa(UUID tenantId, SentidoLancamento sentido, String descricao, BigDecimal valor,
			LocalDate data, UUID movimentacaoEstoqueId, Instant criadoEm) {
		this.tenantId = tenantId;
		this.sentido = sentido;
		this.descricao = descricao;
		this.valor = valor;
		this.data = data;
		this.movimentacaoEstoqueId = movimentacaoEstoqueId;
		this.criadoEm = criadoEm;
	}

	public UUID getId() {
		return id;
	}

	public UUID getTenantId() {
		return tenantId;
	}

	public SentidoLancamento getSentido() {
		return sentido;
	}

	public String getDescricao() {
		return descricao;
	}

	public BigDecimal getValor() {
		return valor;
	}

	public LocalDate getData() {
		return data;
	}

	public UUID getMovimentacaoEstoqueId() {
		return movimentacaoEstoqueId;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}

}
