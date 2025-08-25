package com.kji.scheduler.config;

import java.util.List;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
	
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		
		// 추가: SPA 헤더 토큰 인식용 핸들러
		CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
		
		// (옵션) 요청 속성 이름 지정. 기본 동작으로도 충분하지만 명시해두면 안전.
		requestHandler.setCsrfRequestAttributeName("_csrf");
		
		http
			.cors(c -> {}) // 아래 CorsConfigurationSource 사용
			.csrf(csrf -> csrf
					.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
					.csrfTokenRequestHandler(requestHandler)  // 추가: 헤더(X-XSRF-TOKEN) 인식
					.ignoringRequestMatchers("/auth/login", "/auth/logout")
					)
			.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
			.authorizeHttpRequests(auth -> auth
					.requestMatchers("/auth/login", "/auth/logout", "/actuator/health", "/h2-console/**").permitAll()
					.requestMatchers(HttpMethod.GET, "/jobs/**").permitAll() // 조회만 오픈
					.anyRequest().authenticated()
					)
			.headers(h -> h.frameOptions(f -> f.sameOrigin())) // H2 console용
			.formLogin(f -> f.disable())   // SPA이므로 폼로그인 비활성화
			.httpBasic(b -> b.disable());  // 기본 인증 끔
		
		return http.build();
		
	  }
	
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		
		CorsConfiguration cfg = new CorsConfiguration();
		cfg.setAllowedOrigins(List.of("http://localhost:5173"));
		cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
		cfg.setAllowedHeaders(List.of("Content-Type","X-XSRF-TOKEN","Authorization"));
		cfg.setAllowCredentials(true); // 세션 쿠키 전송 허용
		
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", cfg);
		//source.registerCorsConfiguration("/jobs/**", cfg);
		return source;
		
	}
	
	// 데모용 InMemory; 이후 DB 연동으로 교체
//	@Bean
//	public UserDetailsService userDetailsService(PasswordEncoder encoder) {
//		UserDetails user = User.withUsername("admin")
//				.password(encoder.encode("***REMOVED***"))
//				.roles("ADMIN")
//				.build();
//		return new InMemoryUserDetailsManager(user);
//	}

	@Bean
	public PasswordEncoder passwordEncoder() { 
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}
	
//	@Bean
//	ApplicationRunner initPwd(PasswordEncoder encoder) {
//	    return args -> {
//	        System.out.println("ADMIN PW = " + encoder.encode("***REMOVED***"));
//	    };
//	}
	
}
