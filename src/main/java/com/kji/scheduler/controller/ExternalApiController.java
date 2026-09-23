package com.kji.scheduler.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kji.scheduler.dto.ExternalApiCallHistorySearchDto;
import com.kji.scheduler.dto.ExternalApiRequestDto;
import com.kji.scheduler.service.ExternalApiCallLogService;
import com.kji.scheduler.service.ExternalApiExecutionService;
import com.kji.scheduler.service.ExternalApiService;

@RestController
@RequestMapping("/externalApi")
public class ExternalApiController {
	
	private final ExternalApiService externalApiService;
	private final ExternalApiExecutionService externalApiExecutionService;
	private final ExternalApiCallLogService externalApiCallLogService;
	
	public ExternalApiController(
			ExternalApiService externalApiService,
			ExternalApiExecutionService externalApiExecutionService,
			ExternalApiCallLogService externalApiCallLogService) {

		this.externalApiService = externalApiService;
		this.externalApiExecutionService = externalApiExecutionService;
		this.externalApiCallLogService = externalApiCallLogService;
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
	
	// External API 등록
	@PostMapping("/add")
	public ResponseEntity<?> createExternalApi(@RequestBody ExternalApiRequestDto request) {
		
		try {
			
			// 1. External API 기본 정보와 파라미터를 등록한다.
			Long externalApiId = externalApiService.createExternalApiWithParams(request);
			
			// 2. 생성된 External API 식별자를 반환한다.
			return ResponseEntity.ok(externalApiId);
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("External API 등록 실패: " + e.getMessage());
		}
	}
	
	// External API 수정
	@PutMapping("/update")
	public ResponseEntity<?> updateExternalApi(@RequestParam(name = "externalApiId") Long externalApiId, @RequestBody ExternalApiRequestDto request) {
		
		try {
			
			// 1. External API 기본정보와 파라미터를 수정한다.
			externalApiService.updateExternalApiWithParams(externalApiId, request);
			
			// 2. 수정 성공 결과를 반환한다.
			return ResponseEntity.ok("External API 수정 성공");
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("External API 수정 실패: " + e.getMessage());
		}
		
	}
	
	// External API 삭제
	@DeleteMapping("/delete")
	public ResponseEntity<?> deleteExternalApi(@RequestParam(name = "externalApiId") Long externalApiId) {
		
		try {
			// 1. External API와 파라미터를 삭제한다.
			externalApiService.deleteExternalApi(externalApiId);
			
			// 2. 삭제 성공 결과를 반환한다.
			return ResponseEntity.ok("External API 삭제 성공");
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
			
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("External API 삭제 실패: " + e.getMessage());
		}
	}
	
	// External API 즉시 실행
	@PostMapping("/execute")
	public ResponseEntity<?> executeExternalApi(@RequestParam(name = "externalApiId") Long externalApiId) {
		
		try {
			
			// 1. 등록된 External API를 즉시 실행한다.
			externalApiExecutionService.execute(externalApiId, null);
			
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
	
	// External API 호출 이력 조회
	@GetMapping("/callHistory")
	public ResponseEntity<?> getExternalApiCallHistory(ExternalApiCallHistorySearchDto searchDto) {
		
		try {
			
			// 1. 검색조건과 페이징 정보로 External API 호출 이력을 조회한다.
			return ResponseEntity.ok(externalApiCallLogService.getCallHistory(searchDto));
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("External API 호출 이력 조회 실패: " + e.getMessage());
		}
	}
	
	// External API 실행별 호출 시도 이력 조회
	@GetMapping("/callHistory/detail")
	public ResponseEntity<?> getExternalApiCallHistoryDetail(@RequestParam(name = "executionId") String executionId) {
		
		try {
			
			// 1. 실행 식별자로 External API 호출 시도 이력을 조회한다.
			return ResponseEntity.ok(externalApiCallLogService.getCallHistoryDetail(executionId));
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("External API 호출 상세 이력 조회 실패: " + e.getMessage());
		}
	}
	
}