package br.com.valorizamei.despesa.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Receita mensal estimada (base do rateio das despesas fixas, RF05) e margem de lucro desejada
 * (RF03) de um MEI. Há um único registro por MEI, identificado pelo próprio {@code tenant_id}.
 * Os percentuais são pontos percentuais (25.00 = 25%).
 */
@Entity
@Table(name = "parametros_financeiros")
public class ParametrosFinanceiros {

	@Id
	@Column(name = "tenant_id")
	private UUID tenantId;

	@Column(name = "receita_mensal_estimada", nullable = false, precision = 12, scale = 2)
	private BigDecimal receitaMensalEstimada;

	@Column(name = "margem_lucro", nullable = false, precision = 5, scale = 2)
	private BigDecimal margemDeLucro;

	@Column(name = "atualizado_em", nullable = false)
	private Instant atualizadoEm;

	protected ParametrosFinanceiros() {
	}

	public ParametrosFinanceiros(UUID tenantId, BigDecimal receitaMensalEstimada, BigDecimal margemDeLucro,
			Instant atualizadoEm) {
		this.tenantId = tenantId;
		alterar(receitaMensalEstimada, margemDeLucro, atualizadoEm);
	}

	public void alterar(BigDecimal receitaMensalEstimada, BigDecimal margemDeLucro, Instant atualizadoEm) {
		this.receitaMensalEstimada = receitaMensalEstimada;
		this.margemDeLucro = margemDeLucro;
		this.atualizadoEm = atualizadoEm;
	}

	public UUID getTenantId() {
		return tenantId;
	}

	public BigDecimal getReceitaMensalEstimada() {
		return receitaMensalEstimada;
	}

	public BigDecimal getMargemDeLucro() {
		return margemDeLucro;
	}

	public Instant getAtualizadoEm() {
		return atualizadoEm;
	}

}
