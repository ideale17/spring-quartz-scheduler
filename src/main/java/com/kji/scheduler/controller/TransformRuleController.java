
package com.kji.scheduler.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kji.scheduler.dto.TransformPreviewRequestDto;
import com.kji.scheduler.dto.TransformRuleRequestDto;
import com.kji.scheduler.service.TransformEngineService;
import com.kji.scheduler.service.TransformRuleService;

@RestController
@RequestMapping("/transform-rule")
public class TransformRuleController {
	
	private final TransformRuleService transformRuleService;
	private final TransformEngineService transformEngineService;

	public TransformRuleController(TransformRuleService transformRuleService, TransformEngineService transformEngineService) {
		this.transformRuleService = transformRuleService;
		this.transformEngineService = transformEngineService;
	}
	
	/**
	 * 가공 규칙 목록 조회 요청을 처리한다.
	 *
	 * @return 가공 규칙 목록 또는 오류 메시지를 담은 HTTP 응답
	 */
	@GetMapping("/list")
	public ResponseEntity<?> getTransformRuleList() {
		
		try {
			
			return ResponseEntity.ok(transformRuleService.getTransformRuleList());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("가공 규칙 목록 조회 실패: " + e.getMessage());
		}
	}
	
	/**
	 * 가공 규칙 상세 정보 조회 요청을 처리한다.
	 *
	 * @param transformRuleId 가공 규칙 식별자
	 * @return 현재 버전의 가공 규칙 상세 정보 또는 오류 메시지를 담은 HTTP 응답
	 */
	@GetMapping("/detail")
	public ResponseEntity<?> getTransformRuleDetail(@RequestParam(name = "transformRuleId") Long transformRuleId) {
		
		try {
			
			return ResponseEntity.ok(transformRuleService.getTransformRuleDetail(transformRuleId));
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("가공 규칙 상세 조회 실패: " + e.getMessage());
		}
	}
	
	/**
	 * 가공 규칙 버전 목록 조회 요청을 처리한다.
	 *
	 * @param transformRuleId 가공 규칙 식별자
	 * @return 가공 규칙 버전 목록 또는 오류 메시지를 담은 HTTP 응답
	 */
	@GetMapping("/versions")
	public ResponseEntity<?> getTransformRuleVersionList(@RequestParam(name = "transformRuleId") Long transformRuleId) {
		
		try {
			
			return ResponseEntity.ok(transformRuleService.getTransformRuleVersionList(transformRuleId));
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("가공 규칙 버전 목록 조회 실패: " + e.getMessage());
		}
	}
	
	/**
	 * 특정 가공 규칙 버전의 상세 조회 요청을 처리한다.
	 *
	 * @param transformRuleId 가공 규칙 식별자
	 * @param transformRuleVersionId 가공 규칙 버전 식별자
	 * @return 해당 버전의 가공 규칙 상세 정보 또는 오류 메시지를 담은 HTTP 응답
	 */
	@GetMapping("/version/detail")
	public ResponseEntity<?> getTransformRuleVersionDetail(
			@RequestParam(name = "transformRuleId") Long transformRuleId,
			@RequestParam(name = "transformRuleVersionId") Long transformRuleVersionId) {
		
		try {
			
			return ResponseEntity.ok(transformRuleService.getTransformRuleVersionDetail(transformRuleId, transformRuleVersionId));
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("가공 규칙 버전 상세 조회 실패: " + e.getMessage());
		}
	}
	
	/**
	 * 가공 규칙 등록 요청을 처리한다.
	 *
	 * @param request 가공 규칙 등록 정보
	 * @return 생성된 가공 규칙 식별자 또는 오류 메시지를 담은 HTTP 응답
	 */
	@PostMapping("/add")
	public ResponseEntity<?> createTransformRule(@RequestBody TransformRuleRequestDto request) {
		
		try {
			
			// 1. 가공 규칙과 최초 버전 및 추출 필드를 등록한다.
			Long transformRuleId = transformRuleService.createTransformRule(request);
			
			// 2. 생성된 가공 규칙 식별자를 반환한다.
			return ResponseEntity.ok(transformRuleId);
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			
		} catch (org.springframework.dao.DataIntegrityViolationException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("중복된 가공 규칙명이거나 데이터 제약조건에 위배됩니다.");
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("가공 규칙 등록 실패: " + e.getMessage());
		}
	}
	
	/**
	 * 가공 규칙 수정 요청을 처리한다.
	 *
	 * @param transformRuleId 수정 대상 가공 규칙 식별자
	 * @param request 가공 규칙 수정 정보
	 * @return 가공 규칙 수정 결과를 담은 HTTP 응답
	 */
	@PutMapping("/update")
	public ResponseEntity<?> updateTransformRule(
			@RequestParam(name = "transformRuleId") Long transformRuleId,
			@RequestBody TransformRuleRequestDto request) {
		
		try {
			
			// 1. 가공 규칙 기본 정보와 가공 설정을 수정한다.
			transformRuleService.updateTransformRule(transformRuleId, request);
			
			// 2. 수정 성공 결과를 반환한다.
			return ResponseEntity.ok("가공 규칙 수정 성공");
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
			
		} catch (org.springframework.dao.DataIntegrityViolationException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("중복된 가공 규칙명이거나 버전 충돌이 발생했습니다. 다시 조회해 주세요.");
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("가공 규칙 수정 실패: " + e.getMessage());
		}
	}
	
	/**
	 * JSON 가공 규칙 미리보기 요청을 처리한다.
	 *
	 * @param request 가공 대상 데이터와 가공 규칙 설정
	 * @return 가공된 JSON 데이터 또는 오류 메시지를 담은 HTTP 응답
	 */
	@PostMapping("/preview")
	public ResponseEntity<?> previewTransformRule(@RequestBody TransformPreviewRequestDto request) {
		
		try {
			
			// 1. 미리보기 요청 정보를 확인한다.
			if (request == null) {
				throw new IllegalArgumentException("가공 미리보기 요청 정보가 없습니다.");
			}
			
			// 2. 전달받은 가공 규칙을 적용한다.
			return ResponseEntity.ok(transformEngineService.transformJson(
					request.getRawData(), request.getVersion(), request.getFields()));
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("가공 미리보기 실패: " + e.getMessage());
		}
		
	}
	
}
