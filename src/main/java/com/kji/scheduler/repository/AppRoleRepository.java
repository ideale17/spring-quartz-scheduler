package com.kji.scheduler.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kji.scheduler.entity.AppRole;

public interface AppRoleRepository extends JpaRepository<AppRole, Long> {
	
	Optional<AppRole> findByName(String name);
	
}