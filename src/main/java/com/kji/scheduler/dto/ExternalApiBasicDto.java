package com.kji.scheduler.dto;

import lombok.Data;

/**
 * External API 기본 정보 수정 DTO.
 *
 * @author kji
 * @since 2026. 9. 30.
 */
@Data
public class ExternalApiBasicDto {
	
	private String apiName;				// External API 이름
	private String apiUrl;				// External API 호출 URL
	private String httpMethod;			// HTTP 요청 방식(GET, POST, PUT, PATCH, DELETE)
	private String enabled;				// External API 사용 여부(Y/N)
	
	private String retryEnabled;		// API 호출 실패 시 재시도 사용 여부(Y/N)
	private Integer maxRetryCount;		// 최초 호출 실패 후 추가 재시도 최대 횟수
	private Integer retryIntervalSec;	// 재시도 호출 간 대기 시간(초)
	
	private String description;			// External API 설명
	
}