package com.kji.scheduler.controller;

import java.util.List;

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

import com.kji.scheduler.dto.ExternalApiAuthDto;
import com.kji.scheduler.dto.ExternalApiBasicDto;
import com.kji.scheduler.dto.ExternalApiCallHistorySearchDto;
import com.kji.scheduler.dto.ExternalApiPagingDto;
import com.kji.scheduler.dto.ExternalApiParamDto;
import com.kji.scheduler.dto.ExternalApiRequestDto;
import com.kji.scheduler.service.ExternalApiCallLogService;
import com.kji.scheduler.service.ExternalApiExecutionLogService;
import com.kji.scheduler.service.ExternalApiExecutionService;
import com.kji.scheduler.service.ExternalApiService;

@RestController
@RequestMapping("/external-api")
public class ExternalApiController {
	
	private final ExternalApiService externalApiService;
	private final ExternalApiExecutionService externalApiExecutionService;
	private final ExternalApiCallLogService externalApiCallLogService;
	private final ExternalApiExecutionLogService externalApiExecutionLogService;
	
	public ExternalApiController(
			ExternalApiService externalApiService,
			ExternalApiExecutionService externalApiExecutionService,
			ExternalApiCallLogService externalApiCallLogService,
			ExternalApiExecutionLogService externalApiExecutionLogService) {

		this.externalApiService = externalApiService;
		this.externalApiExecutionService = externalApiExecutionService;
		this.externalApiCallLogService = externalApiCallLogService;
		this.externalApiExecutionLogService = externalApiExecutionLogService;
	}
	
	/**
	 * External API 목록을 조회한다.
	 *
	 * @return External API 목록 조회 결과
	 */
	@GetMapping("/list")
	public ResponseEntity<?> getExternalApiList() {
		
		try {
			
			return ResponseEntity.ok(externalApiService.getExternalApiList());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("External API 목록 조회 실패: " + e.getMessage());
		}
		
	}
	
	/**
	 * External API 상세 정보를 조회한다.
	 *
	 * @param externalApiId External API 식별자
	 * @return External API 상세 정보 조회 결과
	 */
	@GetMapping("/detail")
	public ResponseEntity<?> getExternalApi(@RequestParam(name = "externalApiId") Long externalApiId) {
		
		try {
			
			return ResponseEntity.ok(externalApiService.getExternalApiDetail(externalApiId));
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("External API 조회 실패: " + e.getMessage());
		}
		
	}
	
	/**
	 * External API 페이징 설정을 조회한다.
	 *
	 * @param externalApiId External API 식별자
	 * @return External API 페이징 설정 조회 결과
	 */
	@GetMapping("/paging")
	public ResponseEntity<?> getExternalApiPaging(@RequestParam(name = "externalApiId") Long externalApiId) {
		
		try {
			
			return ResponseEntity.ok(externalApiService.getExternalApiPaging(externalApiId));
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("External API 페이징 설정 조회 실패: " + e.getMessage());
		}
		
	}
	
	/**
	 * External API 파라미터 목록을 조회한다.
	 *
	 * @param externalApiId External API 식별자
	 * @return External API 파라미터 목록 조회 결과
	 */
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
	
	/**
	 * External API를 등록한다.
	 *
	 * @param request External API 등록 요청 정보
	 * @return 생성된 External API 식별자 또는 등록 실패 결과
	 */
	@PostMapping("/add")
	public ResponseEntity<?> createExternalApi(@RequestBody ExternalApiRequestDto request) {
		
		try {
			
			// 1. External API 기본 정보와 파라미터를 등록한다.
			Long externalApiId = externalApiService.createExternalApi(request);
			
			// 2. 생성된 External API 식별자를 반환한다.
			return ResponseEntity.ok(externalApiId);
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("External API 등록 실패: " + e.getMessage());
		}
	}
	
	/**
	 * External API 기본 정보를 수정한다.
	 *
	 * @param externalApiId External API 식별자
	 * @param basic External API 기본 정보
	 * @return External API 기본 정보 수정 처리 결과
	 */
	@PutMapping("/basic")
	public ResponseEntity<?> updateExternalApiBasic(
			@RequestParam(name = "externalApiId") Long externalApiId,
			@RequestBody ExternalApiBasicDto basic) {
		
		try {
			
			// 1. External API 기본 정보를 수정한다.
			externalApiService.updateExternalApiBasic(externalApiId, basic);
			
			// 2. 수정 성공 결과를 반환한다.
			return ResponseEntity.ok("External API 기본 정보 수정 성공");
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("External API 기본 정보 수정 실패: " + e.getMessage());
		}
	}
	
	/**
	 * External API 인증 정보를 수정한다.
	 *
	 * @param externalApiId External API 식별자
	 * @param auth External API 인증 정보
	 * @return External API 인증 정보 수정 처리 결과
	 */
	@PutMapping("/auth")
	public ResponseEntity<?> updateExternalApiAuth(
			@RequestParam(name = "externalApiId") Long externalApiId,
			@RequestBody ExternalApiAuthDto auth) {
		
		try {
			
			// 1. External API 인증 정보를 수정한다.
			externalApiService.updateExternalApiAuth(externalApiId, auth);
			
			// 2. 수정 성공 결과를 반환한다.
			return ResponseEntity.ok("External API 인증 정보 수정 성공");
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("External API 인증 정보 수정 실패: " + e.getMessage());
		}
		
	}
	
	/**
	 * External API 페이징 설정을 저장한다.
	 *
	 * @param externalApiId External API 식별자
	 * @param paging External API 페이징 설정
	 * @return External API 페이징 설정 저장 처리 결과
	 */
	@PutMapping("/paging")
	public ResponseEntity<?> saveExternalApiPaging(
			@RequestParam(name = "externalApiId") Long externalApiId,
			@RequestBody ExternalApiPagingDto paging) {
		
		try {
			
			// 1. External API 페이징 설정을 저장한다.
			externalApiService.saveExternalApiPaging(externalApiId, paging);
			
			// 2. 저장 성공 결과를 반환한다.
			return ResponseEntity.ok("External API 페이징 설정 저장 성공");
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("External API 페이징 설정 저장 실패: " + e.getMessage());
		}
	}
	
	/**
	 * External API 파라미터를 수정한다.
	 *
	 * @param externalApiId External API 식별자
	 * @param params External API 파라미터 목록
	 * @return External API 파라미터 수정 처리 결과
	 */
	@PutMapping("/params")
	public ResponseEntity<?> updateExternalApiParams(@RequestParam(name = "externalApiId") Long externalApiId, @RequestBody List<ExternalApiParamDto> params) {
		
		try {
			
			// 1. External API 파라미터를 수정한다.
			externalApiService.updateExternalApiParams(externalApiId, params);
			
			// 2. 수정 성공 결과를 반환한다.
			return ResponseEntity.ok("External API 파라미터 수정 성공");
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("External API 파라미터 수정 실패: " + e.getMessage());
		}
	}
	
	/**
	 * External API를 삭제한다.
	 *
	 * @param externalApiId External API 식별자
	 * @return External API 삭제 처리 결과
	 */
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
	
	/**
	 * External API 페이징 설정을 삭제한다.
	 *
	 * @param externalApiId External API 식별자
	 * @return External API 페이징 설정 삭제 처리 결과
	 */
	@DeleteMapping("/paging")
	public ResponseEntity<?> deleteExternalApiPaging(@RequestParam(name = "externalApiId") Long externalApiId) {
		
		try {
			
			// 1. External API 페이징 설정을 삭제한다.
			externalApiService.deleteExternalApiPaging(externalApiId);
			
			// 2. 삭제 성공 결과를 반환한다.
			return ResponseEntity.ok("External API 페이징 설정 삭제 성공");
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
			
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("External API 페이징 설정 삭제 실패: " + e.getMessage());
		}
		
	}
	
	/**
	 * 등록된 External API를 즉시 실행한다.
	 *
	 * @param externalApiId External API 식별자
	 * @return External API 호출 처리 결과
	 */
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
	
	/**
	 * External API 실행 이력을 조회한다.
	 *
	 * @param searchDto External API 실행 이력 검색 조건
	 * @return External API 실행 이력 조회 결과
	 */
	@GetMapping("/call-history")
	public ResponseEntity<?> getExternalApiCallHistory(ExternalApiCallHistorySearchDto searchDto) {
		
		try {
			
			// 1. 검색조건과 페이징 정보로 External API 실행 이력을 조회한다.
			return ResponseEntity.ok(externalApiExecutionLogService.getExecutionHistory(searchDto));
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("External API 실행 이력 조회 실패: " + e.getMessage());
		}
	}
	
	/**
	 * External API 실행별 호출 시도 이력을 조회한다.
	 *
	 * @param executionId External API 실행 식별자
	 * @return External API 호출 시도 이력 조회 결과
	 */
	@GetMapping("/call-history/detail")
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
