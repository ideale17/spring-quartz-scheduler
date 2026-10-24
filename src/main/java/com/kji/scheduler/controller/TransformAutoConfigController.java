
package com.kji.scheduler.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kji.scheduler.dto.TransformAutoConfigRequestDto;
import com.kji.scheduler.service.TransformAutoConfigService;

@RestController
@RequestMapping("/transform-auto-config")
public class TransformAutoConfigController {
	
	private final TransformAutoConfigService transformAutoConfigService;
	
	public TransformAutoConfigController(TransformAutoConfigService transformAutoConfigService) {
		this.transformAutoConfigService = transformAutoConfigService;
	}
	
	/**
	 * External API에 연결된 자동 가공 규칙 목록 조회 요청을 처리한다.
	 *
	 * @param externalApiId External API 식별자
	 * @return 자동 가공 규칙 연결 목록 또는 오류 메시지를 담은 HTTP 응답
	 */
	@GetMapping("/list")
	public ResponseEntity<?> getTransformAutoConfigList(@RequestParam(name = "externalApiId") Long externalApiId) {
		
		try {
			
			// 1. 자동 가공 규칙 연결 목록을 조회한다.
			return ResponseEntity.ok(transformAutoConfigService.getTransformAutoConfigList(externalApiId));
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("자동 가공 규칙 연결 목록 조회 실패: " + e.getMessage());
		}
	}
	
	/**
	 * External API의 자동 가공 규칙 연결 설정 수정 요청을 처리한다.
	 *
	 * @param externalApiId External API 식별자
	 * @param request 자동 가공 규칙 연결 목록
	 * @return 저장 결과를 담은 HTTP 응답
	 */
	@PutMapping("/update")
	public ResponseEntity<?> updateTransformAutoConfigs(
			@RequestParam(name = "externalApiId") Long externalApiId,
			@RequestBody TransformAutoConfigRequestDto request) {
		
		try {
			
			// 1. 자동 가공 규칙 연결 설정을 저장한다.
			transformAutoConfigService.updateTransformAutoConfigs(externalApiId, request);
			
			// 2. 저장 결과를 반환한다.
			return ResponseEntity.ok("자동 가공 규칙 연결 설정 저장 성공");
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			
		} catch (DataIntegrityViolationException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("자동 가공 규칙 연결 정보가 충돌했습니다. 다시 조회해 주세요.");
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("자동 가공 규칙 연결 설정 저장 실패: " + e.getMessage());
		}
	}
	
}
