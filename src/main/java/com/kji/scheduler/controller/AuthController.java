package com.kji.scheduler.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kji.scheduler.dto.AuthMeResponseDto;
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
	public record SignupEnabledRes(boolean signupEnabled) {}
	public record ErrorRes(String message) {}
	
	/**
	 * 현재 사용자 이름과 권한 목록 조회 요청을 처리한다.
	 *
	 * @param auth 현재 사용자 인증 정보
	 * @return 현재 사용자 이름과 권한 목록
	 */
	@GetMapping("/me")
	public ResponseEntity<AuthMeResponseDto> me(Authentication auth) {
		
	    // 1. 인증된 사용자의 권한 목록을 조회한다.
	    List<String> roles = auth.getAuthorities().stream()
	            .map(GrantedAuthority::getAuthority)
	            .sorted()
	            .toList();
	    
	    // 2. 응답 정보를 생성한다.
	    AuthMeResponseDto response = new AuthMeResponseDto();
	    response.setUsername(auth.getName());
	    response.setRoles(roles);
	    
	    // 3. 사용자 정보와 권한 목록을 반환한다.
	    return ResponseEntity.ok(response);
	    
	}
		
	/**
	 * 회원가입 요청을 처리한다.
	 *
	 * @param request 회원가입 요청 정보
	 * @return 회원가입 성공 시 본문 없는 HTTP 응답, 실패 시 오류 메시지를 담은 HTTP 응답
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
	 * 회원가입 허용 여부 조회 요청을 처리한다.
	 *
	 * @return 회원가입 허용 여부를 담은 HTTP 응답
	 */
	@GetMapping("/signup-enabled")
	public ResponseEntity<SignupEnabledRes> signupEnabled() {
		return ResponseEntity.ok(new SignupEnabledRes(appUserService.isSignupEnabled()));
	}
	
}

