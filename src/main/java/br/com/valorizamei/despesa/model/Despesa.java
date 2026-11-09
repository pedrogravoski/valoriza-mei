package br.com.valorizamei.despesa.model;

import java.math.BigDecimal;
import java.time.Instant;
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
 * Despesa de um MEI (RF02). Conforme a classificação, só um dos valores é preenchido: o valor
 * mensal em reais (fixa) ou o percentual sobre o preço de venda, em pontos percentuais
 * (variável). A descrição é única entre as despesas do mesmo MEI, sem diferenciar maiúsculas e
 * minúsculas.
 */
@Entity
@Table(name = "despesa")
public class Despesa {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "tenant_id", nullable = false)
	private UUID tenantId;

	@Column(name = "descricao", nullable = false, length = 120)
	private String descricao;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 10)
	private ClassificacaoDespesa classificacao;

	@Column(name = "valor_mensal", precision = 12, scale = 2)
	private BigDecimal valorMensal;

	@Column(name = "percentual", precision = 5, scale = 2)
	private BigDecimal percentual;

	@Column(name = "criado_em", nullable = false)
	private Instant criadoEm;

	@Column(name = "atualizado_em", nullable = false)
	private Instant atualizadoEm;

	protected Despesa() {
	}

	public Despesa(UUID tenantId, String descricao, ClassificacaoDespesa classificacao, BigDecimal valorMensal,
			BigDecimal percentual, Instant criadoEm) {
		this.tenantId = tenantId;
		this.criadoEm = criadoEm;
		alterar(descricao, classificacao, valorMensal, percentual, criadoEm);
	}

	public void alterar(String descricao, ClassificacaoDespesa classificacao, BigDecimal valorMensal,
			BigDecimal percentual, Instant atualizadoEm) {
		this.descricao = descricao;
		this.classificacao = classificacao;
		this.valorMensal = valorMensal;
		this.percentual = percentual;
		this.atualizadoEm = atualizadoEm;
	}

	public UUID getId() {
		return id;
	}

	public UUID getTenantId() {
		return tenantId;
	}

	public String getDescricao() {
		return descricao;
	}

	public ClassificacaoDespesa getClassificacao() {
		return classificacao;
	}

	public BigDecimal getValorMensal() {
		return valorMensal;
	}

	public BigDecimal getPercentual() {
		return percentual;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}

	public Instant getAtualizadoEm() {
		return atualizadoEm;
	}

}
