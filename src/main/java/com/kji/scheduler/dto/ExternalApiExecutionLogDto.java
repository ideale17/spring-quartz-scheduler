package com.kji.scheduler.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * External API 1회 실행 단위의 전체 실행 이력 저장에 필요한 정보를 담는다.
 */
@Data
public class ExternalApiExecutionLogDto {

	private String executionId;
	private Long externalApiId;
	private String fireInstanceId;
	private LocalDateTime startedAt;
	private LocalDateTime finishedAt;
	private Long runMillis;
	private String status;
	private String errorMessage;
	private LocalDateTime createdAt;
	
}
