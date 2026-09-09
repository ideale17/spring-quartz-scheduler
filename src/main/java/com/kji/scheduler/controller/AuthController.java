package com.kji.scheduler.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/auth")
public class AuthController {
	
	private final AuthenticationManager authenticationManager;
	
	private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
	
	public AuthController(AuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}

	public record LoginReq(String username, String password) {}
	public record MeRes(String username) {}
	
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody LoginReq req, HttpServletRequest request, HttpServletResponse response) {
		
		try {
			Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(req.username(), req.password())
			);
			
			// 컨텍스트 생성 후 인증자 설정
		    SecurityContext context = SecurityContextHolder.createEmptyContext();
		    context.setAuthentication(authentication);
		    SecurityContextHolder.setContext(context);

		    // 컨텍스트를 세션에 명시적으로 저장 (다음 요청에서 인식)
		    securityContextRepository.saveContext(context, request, response);
		    
			return ResponseEntity.ok(new MeRes(authentication.getName()));
		} catch (AuthenticationException e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
		}
		
	}
	
	@PostMapping("/logout")
	public ResponseEntity<?> logout(HttpServletRequest request) {
		var s = request.getSession(false);
		if (s != null) s.invalidate();
		SecurityContextHolder.clearContext();
		return ResponseEntity.ok().build();
	}
	
	@GetMapping("/me")
	public ResponseEntity<?> me(Authentication auth) {
		return ResponseEntity.ok(new MeRes(auth != null ? auth.getName() : null));
	}
	
	@GetMapping("/csrf")
	public ResponseEntity<CsrfToken> csrf(CsrfToken csrfToken) {
		return ResponseEntity.ok(csrfToken);
	}
	
}

