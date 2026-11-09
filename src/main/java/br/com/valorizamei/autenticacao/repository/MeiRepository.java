package br.com.valorizamei.autenticacao.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.valorizamei.autenticacao.model.Mei;

public interface MeiRepository extends JpaRepository<Mei, UUID> {

}
