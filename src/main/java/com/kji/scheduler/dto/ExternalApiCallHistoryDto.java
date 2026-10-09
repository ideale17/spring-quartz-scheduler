package com.kji.scheduler.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * External API 호출 시도별 이력과 처리 결과 정보를 담는다.
 */
@Data
public class ExternalApiCallHistoryDto {
	
	private Long apiCallLogId;
	private Long externalApiId;
	private String apiName;
	private String fireInstanceId;
	private Integer requestSequence;
	private Integer attemptNo;
	private Integer httpStatus;
	private LocalDateTime startedAt;
	private LocalDateTime finishedAt;
	private Long runMillis;
	private String status;
	private String errorMessage;
	private LocalDateTime createdAt;
	
}
