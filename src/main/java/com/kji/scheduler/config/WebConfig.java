package com.kji.scheduler.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
	
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/jobs/**") // jobs로 시작하는 모든 요청만 허용
				.allowedOrigins("http://localhost:5173") // 프론트 개발 서버 허용
				.allowedMethods("*"); // GET, POST, PUT, DELETE 등 모두 허용
	}
	
}
