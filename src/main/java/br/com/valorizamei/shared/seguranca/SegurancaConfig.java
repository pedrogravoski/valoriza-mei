package br.com.valorizamei.shared.seguranca;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Segurança da API: stateless, autenticada por token JWT (HS256) enviado no cabeçalho
 * {@code Authorization: Bearer}, emitido e validado pela própria aplicação.
 * <p>
 * Só o cadastro, o login, a documentação da API e o health são públicos; todo o resto exige
 * um token válido de um MEI existente.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PropriedadesJwt.class)
public class SegurancaConfig {

	static final String[] ROTAS_DE_AUTENTICACAO = { "/api/v1/auth/cadastro", "/api/v1/auth/login" };

	static final String[] ROTAS_DE_DOCUMENTACAO = { "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui/**",
			"/swagger-ui.html" };

	@Bean
	SecurityFilterChain cadeiaDeSeguranca(HttpSecurity http, JwtDecoder decodificador,
			VerificadorDeTenant verificadorDeTenant,
			@Qualifier("handlerExceptionResolver") HandlerExceptionResolver tratadorDeErros) {
		RespostaDeAcessoRecusado acessoRecusado = new RespostaDeAcessoRecusado(tratadorDeErros);
		return http
			.csrf(csrf -> csrf.disable())
			.sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(rotas -> rotas
				.requestMatchers(HttpMethod.POST, ROTAS_DE_AUTENTICACAO).permitAll()
				.requestMatchers(ROTAS_DE_DOCUMENTACAO).permitAll()
				.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
				.requestMatchers("/error").permitAll()
				.anyRequest().authenticated())
			.oauth2ResourceServer(servidor -> servidor
				.jwt(jwt -> jwt
					.decoder(decodificador)
					.jwtAuthenticationConverter(new ConversorDeTokenAutenticado(verificadorDeTenant)))
				.authenticationEntryPoint(acessoRecusado)
				.accessDeniedHandler(acessoRecusado))
			.exceptionHandling(excecoes -> excecoes
				.authenticationEntryPoint(acessoRecusado)
				.accessDeniedHandler(acessoRecusado))
			.build();
	}

	@Bean
	JwtEncoder codificadorDeToken(PropriedadesJwt propriedades) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(chave(propriedades)));
	}

	@Bean
	JwtDecoder decodificadorDeToken(PropriedadesJwt propriedades, Clock relogio) {
		NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(chave(propriedades))
			.macAlgorithm(MacAlgorithm.HS256)
			.build();
		JwtTimestampValidator validadeDoToken = new JwtTimestampValidator(Duration.ZERO);
		validadeDoToken.setClock(relogio);
		decodificador.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
				validadeDoToken, new JwtIssuerValidator(ClaimsDoToken.EMISSOR)));
		return decodificador;
	}

	@Bean
	PasswordEncoder codificadorDeSenha() {
		return new BCryptPasswordEncoder();
	}

	private static SecretKey chave(PropriedadesJwt propriedades) {
		return new SecretKeySpec(propriedades.segredo().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

}
