package br.com.valorizamei.calendario.model;

import java.time.Instant;
import java.time.MonthDay;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Data comemorativa cadastrada por um MEI (RF09), repetida todo ano no dia e mês informados.
 * O nome é único entre as datas do mesmo MEI, sem diferenciar maiúsculas e minúsculas.
 */
@Entity
@Table(name = "data_comemorativa")
public class DataComemorativa {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "tenant_id", nullable = false)
	private UUID tenantId;

	@Column(name = "nome", nullable = false, length = 80)
	private String nome;

	@Column(name = "dia", nullable = false)
	private short dia;

	@Column(name = "mes", nullable = false)
	private short mes;

	@Column(name = "criado_em", nullable = false)
	private Instant criadoEm;

	@Column(name = "atualizado_em", nullable = false)
	private Instant atualizadoEm;

	protected DataComemorativa() {
	}

	public DataComemorativa(UUID tenantId, String nome, int dia, int mes, Instant criadoEm) {
		this.tenantId = tenantId;
		this.criadoEm = criadoEm;
		alterar(nome, dia, mes, criadoEm);
	}

	public void alterar(String nome, int dia, int mes, Instant atualizadoEm) {
		this.nome = nome;
		this.dia = (short) dia;
		this.mes = (short) mes;
		this.atualizadoEm = atualizadoEm;
	}

	public MonthDay getDiaDoAno() {
		return MonthDay.of(mes, dia);
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

	public int getDia() {
		return dia;
	}

	public int getMes() {
		return mes;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}

	public Instant getAtualizadoEm() {
		return atualizadoEm;
	}

}
