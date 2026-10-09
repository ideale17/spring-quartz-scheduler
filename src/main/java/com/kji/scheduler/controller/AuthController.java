package com.kji.scheduler.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kji.scheduler.dto.SignupRequest;
import com.kji.scheduler.service.AppUserService;

@RestController
@RequestMapping("/auth")
public class AuthController {
	
	private final AppUserService appUserService;
	
	public AuthController(AppUserService appUserService) {
		this.appUserService = appUserService;
	}
	
	public record LoginReq(String username, String password) {}
	public record MeRes(String username) {}
	public record SignupEnabledRes(boolean signupEnabled) {}
	public record ErrorRes(String message) {}
	
	/**
	 * 현재 사용자 이름을 조회한다.
	 *
	 * @param auth 현재 사용자 인증 정보
	 * @return 현재 사용자 이름 조회 결과
	 */
	@GetMapping("/me")
	public ResponseEntity<?> me(Authentication auth) {
		return ResponseEntity.ok(new MeRes(auth != null ? auth.getName() : null));
	}
		
	/**
	 * 회원가입을 처리한다.
	 *
	 * @param request 회원가입 요청 정보
	 * @return 회원가입 처리 결과
	 */
	@PostMapping("/signup")
	public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
		
		try {
			
			// 1. 회원가입을 처리한다.
			appUserService.signup(request);
			
			// 2. 회원가입 성공 응답을 반환한다.
			return ResponseEntity.ok().build();
			
		} catch (IllegalArgumentException | IllegalStateException e) {
			return ResponseEntity.badRequest().body(new ErrorRes(e.getMessage()));
		}
		
	}
	
	/**
	 * 회원가입 허용 여부를 조회한다.
	 *
	 * @return 회원가입 허용 여부
	 */
	@GetMapping("/signup-enabled")
	public ResponseEntity<SignupEnabledRes> signupEnabled() {
		return ResponseEntity.ok(new SignupEnabledRes(appUserService.isSignupEnabled()));
	}
	
}

