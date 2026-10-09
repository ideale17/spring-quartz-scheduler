package com.kji.scheduler.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * External API 실행별 호출 집계와 실행 결과 정보를 담는다.
 */
@Data
public class ExternalApiCallHistorySummaryDto {
	
	private String executionId;
	private Long externalApiId;
	private String apiName;
	private String fireInstanceId;
	private String status;
	private Integer requestCount;
	private Integer attemptCount;
	private Integer retryCount;
	private Long totalRunMillis;
	private Long apiRunMillis;
	private String errorMessage;
	private LocalDateTime startedAt;
	private LocalDateTime finishedAt;
	
}
