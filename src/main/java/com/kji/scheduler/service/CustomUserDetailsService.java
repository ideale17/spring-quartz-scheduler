package com.kji.scheduler.service;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.kji.scheduler.entity.AppUser;
import com.kji.scheduler.repository.AppUserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final AppUserRepository userRepo;

	/**
	 * 사용자 이름으로 사용자와 권한을 조회하여 Spring Security 인증 정보를 생성한다.
	 *
	 * @param username 조회할 사용자 이름
	 * @return 인증에 사용할 사용자 정보
	 * @throws UsernameNotFoundException 사용자가 존재하지 않는 경우
	 */
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		
		// 1. 사용자 정보를 조회한다.
		AppUser user = userRepo.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
		
		// 2. 사용자 권한을 Spring Security 권한으로 변환한다.
		List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
				.map(role -> new SimpleGrantedAuthority(role.getName()))
				.toList();
		
		// 3. 인증에 사용할 사용자 정보를 반환한다.
		return org.springframework.security.core.userdetails.User
				.withUsername(user.getUsername())
				.password(user.getPassword())
				.authorities(authorities)
				.disabled(!user.isEnabled())
				.build();
	}
}
