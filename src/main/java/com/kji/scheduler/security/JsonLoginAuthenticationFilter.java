package com.kji.scheduler.security;

import java.io.IOException;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JsonLoginAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
	
	private final ObjectMapper objectMapper;
	
	public JsonLoginAuthenticationFilter(AuthenticationManager authenticationManager, ObjectMapper objectMapper) {
		setAuthenticationManager(authenticationManager);
		this.objectMapper = objectMapper;
		
		setFilterProcessesUrl("/auth/login");
		setSecurityContextRepository(new HttpSessionSecurityContextRepository());
	}
	
	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
		
		try {
			
			LoginRequest loginRequest = objectMapper.readValue(request.getInputStream(), LoginRequest.class);
			
			UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password());
			
			return getAuthenticationManager().authenticate(authenticationToken);
			
		} catch (IOException e) {
			throw new AuthenticationServiceException("로그인 요청을 읽을 수 없습니다.", e);
		}
	}
	
	private record LoginRequest(String username, String password) {}
	
}