package com.kji.scheduler.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.kji.scheduler.dto.SignupRequest;
import com.kji.scheduler.entity.AppRole;
import com.kji.scheduler.entity.AppUser;
import com.kji.scheduler.repository.AppRoleRepository;
import com.kji.scheduler.repository.AppUserRepository;

@Service
public class AppUserService {
	
	private final AppUserRepository appUserRepository;
	private final AppRoleRepository appRoleRepository;
	private final PasswordEncoder passwordEncoder;
	private final boolean signupEnabled;
	
	public AppUserService(AppUserRepository appUserRepository, AppRoleRepository appRoleRepository, PasswordEncoder passwordEncoder,
			@Value("${app.security.signup-enabled:false}") boolean signupEnabled) {
		this.appUserRepository = appUserRepository;
		this.appRoleRepository = appRoleRepository;
		this.passwordEncoder = passwordEncoder;
		this.signupEnabled = signupEnabled;
	}
	
	public void signup(SignupRequest request) {
		
		// 1. 회원가입 허용 여부를 확인한다.
		if (!signupEnabled) {
			throw new IllegalStateException("현재 회원가입이 허용되지 않습니다.");
		}
		
		// 2. 회원가입 요청값을 검증한다.
		if (request.getUsername() == null || request.getUsername().isBlank()) {
			throw new IllegalArgumentException("아이디는 필수입니다.");
		}
		
		if (request.getPassword() == null || request.getPassword().isBlank()) {
			throw new IllegalArgumentException("비밀번호는 필수입니다.");
		}
		
		if (!request.getPassword().equals(request.getPasswordConfirm())) {
			throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
		}
		
		// 3. 아이디 중복 여부를 확인한다.
		if (appUserRepository.findByUsername(request.getUsername()).isPresent()) {
			throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
		}
		
		// 4. 기본 사용자 권한을 조회한다.
		AppRole userRole = appRoleRepository.findByName("ROLE_USER")
				.orElseThrow(() -> new IllegalStateException("기본 사용자 권한을 찾을 수 없습니다."));
		
		// 5. 회원 정보를 생성한다.
		AppUser appUser = new AppUser();
		appUser.setUsername(request.getUsername());
		appUser.setPassword(passwordEncoder.encode(request.getPassword()));
		appUser.setEnabled(true);
		appUser.getRoles().add(userRole);
		
		// 6. 회원 정보를 저장한다.
		appUserRepository.save(appUser);
		
	}
	
	public boolean isSignupEnabled() {
		return signupEnabled;
	}
	
}