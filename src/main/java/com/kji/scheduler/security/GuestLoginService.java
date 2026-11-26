package com.kji.scheduler.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class GuestLoginService {
	
	public static final String GUEST_AUTHORITY = "ROLE_GUEST";
	
	private final boolean enabled;
	private final HttpSessionSecurityContextRepository securityContextRepository;
	private final SessionAuthenticationStrategy sessionAuthenticationStrategy = new ChangeSessionIdAuthenticationStrategy();
	private final SessionAuthenticationStrategy csrfAuthenticationStrategy;
	
	public GuestLoginService(@Value("${app.security.guest-login-enabled:false}") boolean enabled,
			HttpSessionSecurityContextRepository securityContextRepository, CookieCsrfTokenRepository csrfTokenRepository) {
		this.enabled = enabled;
		this.securityContextRepository = securityContextRepository;
		this.csrfAuthenticationStrategy = new CsrfAuthenticationStrategy(csrfTokenRepository);
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public void login(HttpServletRequest request, HttpServletResponse response) {
		
		// 1. 서버가 조회 전용 게스트 인증을 생성한다.
		Authentication guest = UsernamePasswordAuthenticationToken.authenticated(
				"guest", null, List.of(new SimpleGrantedAuthority(GUEST_AUTHORITY)));
		
		// 2. 기존 세션 식별자와 CSRF 토큰을 갱신한다.
		sessionAuthenticationStrategy.onAuthentication(guest, request, response);
		csrfAuthenticationStrategy.onAuthentication(guest, request, response);
		
		// 3. 다음 요청에서 복원할 수 있도록 인증 정보를 세션에 저장한다.
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(guest);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, request, response);
		
	}
}