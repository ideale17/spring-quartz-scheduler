package com.kji.scheduler.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kji.scheduler.dto.AiAnalysisRequestDto;
import com.kji.scheduler.dto.AiAnalysisSearchDto;
import com.kji.scheduler.service.AiAnalysisService;

@RestController
@RequestMapping("/ai-analysis")
public class AiAnalysisController {
	
	private final AiAnalysisService aiAnalysisService;
	
	public AiAnalysisController(AiAnalysisService aiAnalysisService) {
		this.aiAnalysisService = aiAnalysisService;
	}
	
	/**
	 * AI 분석 대상 데이터 미리보기 요청을 처리한다.
	 *
	 * @param searchDto AI 분석 대상 검색조건
	 * @return 분석 대상 데이터 또는 오류 메시지를 담은 HTTP 응답
	 */
	@GetMapping("/preview")
	public ResponseEntity<?> getAnalysisPreview(AiAnalysisSearchDto searchDto) {
		
		try {
			
			// 1. 검색조건에 해당하는 누적 가공 결과를 조회한다.
			return ResponseEntity.ok(aiAnalysisService.getAnalysisPreview(searchDto));
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("AI 분석 대상 데이터 조회 중 오류가 발생했습니다.");
		}
	}
	
	/**
	 * AI 분석 데이터셋 구성 정보 조회 요청을 처리한다.
	 *
	 * @param searchDto AI 분석 대상 검색조건
	 * @return 데이터셋 구성 정보 또는 오류 메시지를 담은 HTTP 응답
	 */
	@GetMapping("/dataset")
	public ResponseEntity<?> getAnalysisDataset(AiAnalysisSearchDto searchDto) {
		
		try {
			
			// 1. 가공 결과를 공통 분석 데이터셋으로 구성한다.
			return ResponseEntity.ok(aiAnalysisService.getAnalysisDataset(searchDto));
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("AI 분석 데이터셋 조회 중 오류가 발생했습니다.");
		}
	}
	
	/**
	 * AI 분석 실행 요청을 처리한다.
	 *
	 * @param request AI 분석 실행 요청
	 * @return AI 분석 결과 또는 오류 메시지를 담은 HTTP 응답
	 */
	@PostMapping("/execute")
	public ResponseEntity<?> executeAnalysis(@RequestBody(required = false) AiAnalysisRequestDto request) {
		
		try {
			
			// 1. AI 분석을 실행하고 결과를 반환한다.
			return ResponseEntity.ok(aiAnalysisService.executeAnalysis(request));
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("AI 분석 실행 중 오류가 발생했습니다.");
		}
	}
	
}