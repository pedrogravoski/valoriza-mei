package br.com.valorizamei;

import org.springframework.boot.SpringApplication;

/**
 * Sobe a aplicação localmente com o PostgreSQL do Testcontainers ({@code mvnw.cmd spring-boot:test-run}).
 * O Docker Compose fica desativado: o banco vem do contêiner do Testcontainers, que só precisa do
 * Docker acessível (inclusive o Docker do WSL exposto em {@code localhost:2375}).
 */
public class TestValorizaMeiApplication {

	public static void main(String[] args) {
		System.setProperty("spring.docker.compose.enabled", "false");
		SpringApplication.from(ValorizaMeiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
