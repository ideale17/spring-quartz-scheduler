package com.kji.scheduler.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kji.scheduler.dto.RawDataSearchDto;
import com.kji.scheduler.service.RawDataService;

@RestController
@RequestMapping("/raw-data")
public class RawDataController {
	
	private final RawDataService rawDataService;
	
	public RawDataController(RawDataService rawDataService) {
		this.rawDataService = rawDataService;
	}
	
	/**
	 * Raw Data 목록 조회 요청을 처리한다.
	 *
	 * @param searchDto Raw Data 검색조건 및 페이징 정보
	 * @return 페이지 정보를 포함한 Raw Data 목록 또는 오류 메시지를 담은 HTTP 응답
	 */
	@GetMapping("/list")
	public ResponseEntity<?> getRawDataList(RawDataSearchDto searchDto) {
		
		try {
			
			// 1. 검색조건과 페이징 정보로 Raw Data 목록을 조회한다.
			return ResponseEntity.ok(rawDataService.getRawDataList(searchDto));
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Raw Data 목록 조회 실패: " + e.getMessage());
		}
	}
	
	/**
	 * Raw Data 상세 조회 요청을 처리한다.
	 *
	 * @param rawDataId 조회 대상 Raw Data 식별자
	 * @return 원본 응답을 포함한 Raw Data 상세 정보 또는 오류 메시지를 담은 HTTP 응답
	 */
	@GetMapping("/detail")
	public ResponseEntity<?> getRawDataDetail(@RequestParam(name = "rawDataId") Long rawDataId) {
		
		try {
			
			// 1. Raw Data 식별자로 원본 응답을 포함한 상세 정보를 조회한다.
			return ResponseEntity.ok(rawDataService.getRawDataDetail(rawDataId));
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Raw Data 상세 조회 실패: " + e.getMessage());
		}
	}
	
}