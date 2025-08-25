package com.kji.scheduler.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kji.scheduler.entity.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long>{
	Optional<AppUser> findByUsername(String username);
}
