package com.kji.scheduler.service;

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
	
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser u = userRepo.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        var authorities = u.getRoles().stream()
            .map(r -> new SimpleGrantedAuthority(r.getName()))
            .toList();

        return org.springframework.security.core.userdetails.User
            .withUsername(u.getUsername())
            .password(u.getPassword())
            .authorities(authorities)
            .accountExpired(false).accountLocked(false).credentialsExpired(false)
            .disabled(!u.isEnabled())
            .build();
    }
}
