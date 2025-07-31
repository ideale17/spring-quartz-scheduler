package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.entity.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long>{
	// 기본 CRUD 메서드 자동 생성됨
}
