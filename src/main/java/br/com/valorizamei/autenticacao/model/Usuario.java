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
 * Usuário que acessa a conta de um MEI. O e-mail é único em todo o sistema e gravado
 * normalizado (sem espaços nas extremidades e em minúsculas); a senha só existe como hash BCrypt.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "tenant_id", nullable = false)
	private UUID tenantId;

	@Column(name = "nome", nullable = false, length = 120)
	private String nome;

	@Column(name = "email", nullable = false, length = 254, unique = true)
	private String email;

	@Column(name = "senha_hash", nullable = false, length = 100)
	private String senhaHash;

	@Column(name = "criado_em", nullable = false)
	private Instant criadoEm;

	protected Usuario() {
	}

	public Usuario(UUID tenantId, String nome, String email, String senhaHash, Instant criadoEm) {
		this.tenantId = tenantId;
		this.nome = nome;
		this.email = email;
		this.senhaHash = senhaHash;
		this.criadoEm = criadoEm;
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

	public String getEmail() {
		return email;
	}

	public String getSenhaHash() {
		return senhaHash;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}

}
