package com.kji.scheduler.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kji.scheduler.dto.AnalysisLoadRequestDto;
import com.kji.scheduler.dto.AnalysisLoadSearchDto;
import com.kji.scheduler.service.AnalysisLoadService;

@RestController
@RequestMapping("/analysis-load")
public class AnalysisLoadController {
	
	private final AnalysisLoadService analysisLoadService;
	
	public AnalysisLoadController(AnalysisLoadService analysisLoadService) {
		this.analysisLoadService = analysisLoadService;
	}
	
	/**
	 * 적재 모델 정보, 가공 결과 및 적재 상태를 조회한다.
	 */
	@GetMapping("/list")
	public ResponseEntity<?> getAnalysisLoadList(AnalysisLoadSearchDto searchDto) {
		
		try {
			// 1. 적재 대상 목록을 조회한다.
			return ResponseEntity.ok(analysisLoadService.getAnalysisLoadList(searchDto));
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("데이터 적재 대상 목록 조회에 실패했습니다.");
		}
	}
	
	/**
	 * 가공 완료 결과를 데이터 적재 대기 작업으로 등록한다.
	 */
	@PostMapping("/add")
	public ResponseEntity<?> createAnalysisLoadSources(@RequestBody AnalysisLoadRequestDto request) {
		
		try {
			// 1. 적재 대기 작업을 등록한다.
			int count = analysisLoadService.createAnalysisLoadSources(request);
			
			// 2. 등록 건수를 반환한다.
			return ResponseEntity.ok(count);
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			
		} catch (DataIntegrityViolationException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 등록된 Raw Data이거나 데이터 적재 제약조건을 위반했습니다.");
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("데이터 적재 작업 등록에 실패했습니다.");
		}
	}
	
}