package com.kji.scheduler.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * External API 호출 시도별 이력 저장에 필요한 정보를 담는다.
 */
@Data
public class ExternalApiCallLogDto {
	
	private Long apiCallLogId;
	private Long externalApiId;
	private String executionId;
	private String fireInstanceId;
	private Integer requestSequence = 1;
	private Integer attemptNo;
	private Integer httpStatus;
	private LocalDateTime startedAt;
	private LocalDateTime finishedAt;
	private Long runMillis;
	private String status;
	private String errorMessage;
	private LocalDateTime createdAt;
	
}
