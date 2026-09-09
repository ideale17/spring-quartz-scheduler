package com.kji.scheduler.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kji.scheduler.service.ExternalApiExecutionService;
import com.kji.scheduler.service.ExternalApiService;

@RestController
@RequestMapping("/externalApi")
public class ExternalApiController {
	
	private final ExternalApiService externalApiService;
	private final ExternalApiExecutionService externalApiExecutionService;
	
	public ExternalApiController(ExternalApiService externalApiService,
			ExternalApiExecutionService externalApiExecutionService) {
		this.externalApiService = externalApiService;
		this.externalApiExecutionService = externalApiExecutionService;
	}
	
	// External API 목록 조회
	@GetMapping("/list")
	public ResponseEntity<?> getExternalApiList() {
		
		try {
			return ResponseEntity.ok(externalApiService.getExternalApiList());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("External API 목록 조회 실패: " + e.getMessage());
		}
		
	}
	
	// External API 단건 조회
	@GetMapping("/detail")
	public ResponseEntity<?> getExternalApi(@RequestParam(name = "externalApiId") Long externalApiId) {
		
		try {
			return ResponseEntity.ok(externalApiService.getExternalApi(externalApiId));
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("External API 조회 실패: " + e.getMessage());
		}
		
	}
	
	// External API 파라미터 목록 조회
	@GetMapping("/params")
	public ResponseEntity<?> getExternalApiParamList(@RequestParam(name = "externalApiId") Long externalApiId) {
		
		try {
			return ResponseEntity.ok(externalApiService.getExternalApiParamList(externalApiId));
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("External API 파라미터 조회 실패: " + e.getMessage());
		}
		
	}
	
	// External API 즉시 실행
	@PostMapping("/execute")
	public ResponseEntity<?> executeExternalApi(@RequestParam(name = "externalApiId") Long externalApiId) {
		
		try {
			
			// 1. 등록된 External API를 즉시 실행한다.
			externalApiExecutionService.execute(externalApiId);
			
			// 2. 실행 요청 성공 결과를 반환한다.
			return ResponseEntity.ok("External API 호출 성공");
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("External API 호출 실패: " + e.getMessage());
		}
	}
	
}