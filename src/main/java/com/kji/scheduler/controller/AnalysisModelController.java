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

import com.kji.scheduler.dto.AnalysisModelRequestDto;
import com.kji.scheduler.dto.AnalysisModelSearchDto;
import com.kji.scheduler.service.AnalysisModelService;

@RestController
@RequestMapping("/analysis-model")
public class AnalysisModelController {
	
	private final AnalysisModelService analysisModelService;
	
	public AnalysisModelController(AnalysisModelService analysisModelService) {
		this.analysisModelService = analysisModelService;
	}
	
	/**
	 * 데이터 적재 모델 목록 조회 요청을 처리한다.
	 *
	 * @param searchDto 분석 모델 검색 조건
	 * @return 분석 모델 목록 정보
	 */
	@GetMapping("/list")
	public ResponseEntity<?> getAnalysisModelList(AnalysisModelSearchDto searchDto) {
		
		try {
			return ResponseEntity.ok(analysisModelService.getAnalysisModelList(searchDto));
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("데이터 적재 모델 목록 조회에 실패했습니다.");
		}
	}
	

	/**
	 * 데이터 적재 모델의 현재 버전 상세 조회 요청을 처리한다.
	 *
	 * @param analysisModelId 분석 모델 식별자
	 * @return 현재 버전의 분석 모델 상세 정보
	 */
	@GetMapping("/detail")
	public ResponseEntity<?> getAnalysisModelDetail(@RequestParam(name = "analysisModelId") Long analysisModelId) {
		
		try {
			return ResponseEntity.ok(analysisModelService.getAnalysisModelDetail(analysisModelId));
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("데이터 적재 모델 상세 조회에 실패했습니다.");
		}
	}
	
	/**
	 * 데이터 적재 모델 버전 이력 조회 요청을 처리한다.
	 *
	 * @param analysisModelId 분석 모델 식별자
	 * @return 모델 버전 이력
	 */
	@GetMapping("/versions")
	public ResponseEntity<?> getAnalysisModelVersionList(@RequestParam(name = "analysisModelId") Long analysisModelId) {
		
		try {
			return ResponseEntity.ok(analysisModelService.getAnalysisModelVersionList(analysisModelId));
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("데이터 적재 모델 버전 이력 조회에 실패했습니다.");
		}
	}
	
	/**
	 * 선택한 데이터 적재 모델 버전의 상세 조회 요청을 처리한다.
	 *
	 * @param analysisModelId 분석 모델 식별자
	 * @param analysisModelVersionId 모델 버전 식별자
	 * @return 선택한 버전의 분석 모델 상세 정보
	 */
	@GetMapping("/version/detail")
	public ResponseEntity<?> getAnalysisModelVersionDetail(
			@RequestParam(name = "analysisModelId") Long analysisModelId,
			@RequestParam(name = "analysisModelVersionId") Long analysisModelVersionId) {
		
		try {
			return ResponseEntity.ok(analysisModelService.getAnalysisModelVersionDetail(analysisModelId, analysisModelVersionId));
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("데이터 적재 모델 버전 상세 조회에 실패했습니다.");
		}
	}
	
	/**
	 * 데이터 적재 모델 등록 요청을 처리한다.
	 *
	 * @param request 모델 기본 정보, 버전, 필드 매핑
	 * @return 생성된 분석 모델 식별자
	 */
	@PostMapping("/add")
	public ResponseEntity<?> createAnalysisModel(@RequestBody AnalysisModelRequestDto request) {
		
		try {
			Long analysisModelId = analysisModelService.createAnalysisModel(request);
			
			return ResponseEntity.ok(analysisModelId);
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			
		} catch (org.springframework.dao.DataIntegrityViolationException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("중복된 모델명 또는 적재 대상 테이블이거나 DB 제약조건을 위반했습니다.");
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("데이터 적재 모델 등록에 실패했습니다.");
		}
	}
	

	/**
	 * 데이터 적재 모델 수정 요청을 처리한다.
	 *
	 * 기본 정보 변경 시 기존 버전을 유지하고,
	 * 매핑 설정 변경 시 새로운 모델 버전을 생성한다.
	 *
	 * @param analysisModelId 수정 대상 모델 식별자
	 * @param request 수정할 모델 및 필드 매핑 정보
	 * @return 모델 수정 결과
	 */
	@PutMapping("/update")
	public ResponseEntity<?> updateAnalysisModel(
			@RequestParam(name = "analysisModelId") Long analysisModelId,
			@RequestBody AnalysisModelRequestDto request) {
		
		try {
			// 1. 모델 기본 정보와 필드 매핑 설정을 수정한다.
			analysisModelService.updateAnalysisModel(analysisModelId, request);
			
			// 2. 수정 성공 결과를 반환한다.
			return ResponseEntity.ok("데이터 적재 모델 수정 성공");
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
			
		} catch (org.springframework.dao.DataIntegrityViolationException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("중복된 모델명이거나 버전 충돌이 발생했습니다. 다시 조회해 주세요.");
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("데이터 적재 모델 수정에 실패했습니다.");
		}
	}
	
}