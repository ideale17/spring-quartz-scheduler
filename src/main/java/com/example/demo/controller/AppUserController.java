package com.example.demo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entity.AppUser;
import com.example.demo.repository.AppUserRepository;

@RestController
@RequestMapping("/appUsers")
public class AppUserController {
	
	private final AppUserRepository appUserRepository;
	
	public AppUserController(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }
	
	@GetMapping
    public List<AppUser> all() {
        return appUserRepository.findAll();
    }

    @PostMapping
    public AppUser create(@RequestBody AppUser appUser) {
        return appUserRepository.save(appUser);
    }

    @GetMapping("/{id}")
    public AppUser getOne(@PathVariable("id") Long id) {
        return appUserRepository.findById(id).orElse(null);
    }
    
}
