package br.com.valorizamei.produto.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Produto vendido por um MEI, com o custo direto de aquisição (RF01) e o estoque mínimo que
 * dispara o alerta de ruptura (RF08). O nome é único entre os produtos do mesmo MEI, sem
 * diferenciar maiúsculas e minúsculas.
 */
@Entity
@Table(name = "produto")
public class Produto {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "tenant_id", nullable = false)
	private UUID tenantId;

	@Column(name = "nome", nullable = false, length = 120)
	private String nome;

	@Column(name = "custo_direto", nullable = false, precision = 12, scale = 2)
	private BigDecimal custoDireto;

	@Column(name = "estoque_minimo", nullable = false)
	private int estoqueMinimo;

	@Column(name = "criado_em", nullable = false)
	private Instant criadoEm;

	@Column(name = "atualizado_em", nullable = false)
	private Instant atualizadoEm;

	protected Produto() {
	}

	public Produto(UUID tenantId, String nome, BigDecimal custoDireto, int estoqueMinimo, Instant criadoEm) {
		this.tenantId = tenantId;
		this.nome = nome;
		this.custoDireto = custoDireto;
		this.estoqueMinimo = estoqueMinimo;
		this.criadoEm = criadoEm;
		this.atualizadoEm = criadoEm;
	}

	public void alterar(String nome, BigDecimal custoDireto, int estoqueMinimo, Instant atualizadoEm) {
		this.nome = nome;
		this.custoDireto = custoDireto;
		this.estoqueMinimo = estoqueMinimo;
		this.atualizadoEm = atualizadoEm;
	}

	public UUID getId() {
		return id;
	}

	public UUID getTenantId() {
		return tenantId;
	}

	public String getNome() {
		return nome;
	}

	public BigDecimal getCustoDireto() {
		return custoDireto;
	}

	public int getEstoqueMinimo() {
		return estoqueMinimo;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}

	public Instant getAtualizadoEm() {
		return atualizadoEm;
	}

}
