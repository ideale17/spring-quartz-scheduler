package com.kji.scheduler.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kji.scheduler.security.JsonLoginAuthenticationFilter;
import com.kji.scheduler.security.SpaCsrfTokenRequestHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
	
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager, ObjectMapper objectMapper) throws Exception {
		
		JsonLoginAuthenticationFilter loginFilter = new JsonLoginAuthenticationFilter(authenticationManager, objectMapper);
		
		loginFilter.setAuthenticationSuccessHandler((request, response, authentication) -> {
			response.setStatus(HttpStatus.OK.value());
		});
		
		loginFilter.setAuthenticationFailureHandler((request, response, exception) -> {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
		});
		
		http
			.cors(c -> {}) // 아래 CorsConfigurationSource 사용
			.csrf(csrf -> csrf
					.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
					.csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
					.ignoringRequestMatchers("/auth/login", "/auth/logout", "/auth/signup")
					)
			.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
			.authorizeHttpRequests(auth -> auth
					// 1. 로그인 및 회원가입 관련 공개 API는 인증 없이 접근을 허용한다.
					.requestMatchers(
							"/auth/login",
							"/auth/logout",
							"/auth/signup",
							"/auth/signup-enabled").permitAll()
					
					// 2. Job 등록, 수정, 삭제 및 실행 API는 관리자만 접근을 허용한다.
					.requestMatchers(HttpMethod.POST, "/jobs/**").hasRole("ADMIN")
					.requestMatchers(HttpMethod.PUT, "/jobs/**").hasRole("ADMIN")
					.requestMatchers(HttpMethod.DELETE, "/jobs/**").hasRole("ADMIN")
					
					// 3. External API 등록, 수정, 삭제 및 실행은 관리자만 허용한다.
					.requestMatchers(HttpMethod.POST, "/external-api/**").hasRole("ADMIN")
					.requestMatchers(HttpMethod.PUT, "/external-api/**").hasRole("ADMIN")
					.requestMatchers(HttpMethod.DELETE, "/external-api/**").hasRole("ADMIN")
					
					// 4. 가공 규칙 등록 및 수정은 관리자만 허용한다.
					.requestMatchers(HttpMethod.POST, "/transform-rule/add").hasRole("ADMIN")
					.requestMatchers(HttpMethod.PUT, "/transform-rule/update").hasRole("ADMIN")
					
					// 5. 가공 대상 등록은 관리자만 허용한다.
					.requestMatchers(HttpMethod.POST, "/transform-task/add").hasRole("ADMIN")
					
					// 6. AI 분석 실행은 관리자만 허용한다.
					.requestMatchers(HttpMethod.POST, "/ai-analysis/execute").hasRole("ADMIN")
					
					// 7. 나머지 API는 기존과 동일하게 인증된 사용자에게 허용한다.
					.anyRequest().authenticated()
					)
			.formLogin(f -> f.disable())   // SPA이므로 폼로그인 비활성화
			.httpBasic(b -> b.disable())  // 기본 인증 끔
			.exceptionHandling(ex -> ex
			        // 인증되지 않은 사용자는 401을 반환한다.
			        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
			        )
			.logout(logout -> logout
					.logoutUrl("/auth/logout")
					.invalidateHttpSession(true)
					.clearAuthentication(true)
					.deleteCookies("JSESSIONID")
					.logoutSuccessHandler((request, response, authentication) ->
							response.setStatus(HttpStatus.OK.value())))
			.addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class);
		
		return http.build();
		
	}
	
	@Bean
	CorsConfigurationSource corsConfigurationSource(@Value("${app.security.cors-allowed-origins:}") String allowedOrigins) {
		
		CorsConfiguration cfg = new CorsConfiguration();
		
		List<String> origins = Arrays.stream(allowedOrigins.split(","))
				.map(String::trim)
				.filter(origin -> !origin.isBlank())
				.toList();
		
		cfg.setAllowedOrigins(origins);
		cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
		cfg.setAllowedHeaders(List.of("Content-Type","X-XSRF-TOKEN","Authorization"));
		cfg.setAllowCredentials(true); // 세션 쿠키 전송 허용
		
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", cfg);
		return source;
		
	}

	@Bean
	PasswordEncoder passwordEncoder() { 
		return new BCryptPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}
		
}
