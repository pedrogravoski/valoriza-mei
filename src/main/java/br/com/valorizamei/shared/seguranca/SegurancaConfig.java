package br.com.valorizamei.shared.seguranca;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
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
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Segurança da API: stateless, autenticada por token JWT (HS256) enviado no cabeçalho
 * {@code Authorization: Bearer}, emitido e validado pela própria aplicação.
 * <p>
 * Só o cadastro, o login, a documentação da API e o health são públicos; todo o resto exige
 * um token válido de um MEI existente. O frontend em outra origem acessa {@code /api/**} pelas
 * origens de {@link PropriedadesCors}.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ PropriedadesJwt.class, PropriedadesCors.class })
public class SegurancaConfig {

	static final String[] ROTAS_DE_AUTENTICACAO = { "/api/v1/auth/cadastro", "/api/v1/auth/login" };

	static final String[] ROTAS_DE_DOCUMENTACAO = { "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui/**",
			"/swagger-ui.html" };

	/** Validade, em segundos, da pré-checagem CORS no navegador. */
	private static final long VALIDADE_DA_PRE_CHECAGEM = 3600;

	@Bean
	SecurityFilterChain cadeiaDeSeguranca(HttpSecurity http, JwtDecoder decodificador,
			VerificadorDeTenant verificadorDeTenant, PropriedadesCors propriedadesCors,
			@Qualifier("handlerExceptionResolver") HandlerExceptionResolver tratadorDeErros) {
		RespostaDeAcessoRecusado acessoRecusado = new RespostaDeAcessoRecusado(tratadorDeErros);
		return http
			// Antes da autenticação: a pré-checagem não exige token e as respostas 401 e 403
			// já saem com os cabeçalhos CORS, para o frontend conseguir lê-las.
			.addFilterBefore(filtroCors(propriedadesCors, tratadorDeErros), BearerTokenAuthenticationFilter.class)
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

	/**
	 * CORS para o frontend em {@code /api/**}: origens exatas configuradas, sem cookies (o token vai
	 * no cabeçalho {@code Authorization}) e com o {@code Location} dos cadastros visível. Não é
	 * registrado como bean, para nenhuma configuração automática aplicar um segundo filtro.
	 */
	private static CorsFilter filtroCors(PropriedadesCors propriedades, HandlerExceptionResolver tratadorDeErros) {
		CorsConfiguration configuracao = new CorsConfiguration();
		configuracao.setAllowedOrigins(propriedades.origensPermitidas());
		configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuracao.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
		configuracao.setExposedHeaders(List.of(HttpHeaders.LOCATION));
		configuracao.setAllowCredentials(false);
		configuracao.setMaxAge(VALIDADE_DA_PRE_CHECAGEM);
		UrlBasedCorsConfigurationSource origem = new UrlBasedCorsConfigurationSource();
		origem.registerCorsConfiguration("/api/**", configuracao);
		CorsFilter filtro = new CorsFilter(origem);
		filtro.setCorsProcessor(new ProcessadorDeCors(tratadorDeErros));
		return filtro;
	}

	private static SecretKey chave(PropriedadesJwt propriedades) {
		return new SecretKeySpec(propriedades.segredo().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

}
