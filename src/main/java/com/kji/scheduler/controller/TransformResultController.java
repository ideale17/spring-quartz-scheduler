package com.kji.scheduler.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kji.scheduler.dto.TransformResultSearchDto;
import com.kji.scheduler.service.TransformResultService;

@RestController
@RequestMapping("/transform-result")
public class TransformResultController {
	
	private final TransformResultService transformResultService;
	
	public TransformResultController(TransformResultService transformResultService) {
		this.transformResultService = transformResultService;
	}
	
	/**
	 * 가공 결과 목록 조회 요청을 처리한다.
	 *
	 * @param searchDto 가공 결과 검색조건 및 페이징 정보
	 * @return 페이지 정보를 포함한 가공 결과 목록 또는 오류 메시지를 담은 HTTP 응답
	 */
	@GetMapping("/list")
	public ResponseEntity<?> getTransformResultList(TransformResultSearchDto searchDto) {
		
		try {
			
			// 1. 검색조건과 페이징 정보로 가공 결과 목록을 조회한다.
			return ResponseEntity.ok(transformResultService.getTransformResultList(searchDto));
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("가공 결과 목록 조회 실패: " + e.getMessage());
		}
	}
	
	/**
	 * 가공 결과 상세 조회 요청을 처리한다.
	 *
	 * @param transformResultId 조회 대상 가공 결과 식별자
	 * @return 가공 결과 상세 정보 또는 오류 메시지를 담은 HTTP 응답
	 */
	@GetMapping("/detail")
	public ResponseEntity<?> getTransformResultDetail(@RequestParam(name = "transformResultId") Long transformResultId) {
		
		try {
			
			// 1. 가공 결과 식별자로 상세 정보를 조회한다.
			return ResponseEntity.ok(transformResultService.getTransformResultDetail(transformResultId));
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("가공 결과 상세 조회 실패: " + e.getMessage());
		}
	}
	
}