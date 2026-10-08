package com.kji.scheduler.security;

import java.util.function.Supplier;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
	
	private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
	private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();
	
	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
		
		// 1. 기본 XOR 방식으로 CSRF 토큰을 요청 속성에 설정한다.
		this.xor.handle(request, response, csrfToken);
		
		// 2. 토큰을 실제로 조회하여 XSRF-TOKEN 쿠키가 생성되도록 한다.
		csrfToken.get();
	}
	
	@Override
	public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
		
		// 1. SPA에서 X-XSRF-TOKEN 헤더로 전달된 토큰이 있는지 확인한다.
		String headerValue = request.getHeader(csrfToken.getHeaderName());
		
		// 2. 헤더 토큰은 일반 방식으로, 그 외 요청은 XOR 방식으로 검증한다.
		return (StringUtils.hasText(headerValue) ? this.plain : this.xor)
				.resolveCsrfTokenValue(request, csrfToken);
	}
	
}