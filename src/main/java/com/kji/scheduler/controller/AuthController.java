package com.kji.scheduler.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
	
	public record LoginReq(String username, String password) {}
	public record MeRes(String username) {}
	
	@GetMapping("/me")
	public ResponseEntity<?> me(Authentication auth) {
		return ResponseEntity.ok(new MeRes(auth != null ? auth.getName() : null));
	}
	
	@GetMapping("/csrf")
	public ResponseEntity<CsrfToken> csrf(CsrfToken csrfToken) {
		return ResponseEntity.ok(csrfToken);
	}
	
}

