package br.com.valorizamei.autenticacao.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Microempreendedor individual. Cada MEI é um tenant: o seu {@code id} é o {@code tenant_id}
 * gravado nos registros que pertencem a ele.
 */
@Entity
@Table(name = "mei")
public class Mei {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "nome_negocio", nullable = false, length = 120)
	private String nomeNegocio;

	@Column(name = "criado_em", nullable = false)
	private Instant criadoEm;

	protected Mei() {
	}

	public Mei(String nomeNegocio, Instant criadoEm) {
		this.nomeNegocio = nomeNegocio;
		this.criadoEm = criadoEm;
	}

	public UUID getId() {
		return id;
	}

	public String getNomeNegocio() {
		return nomeNegocio;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}

}
