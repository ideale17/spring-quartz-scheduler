package com.kji.scheduler.dto;

import java.time.LocalDateTime;

import lombok.Data;

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
	private LocalDateTime startedAt;
	private LocalDateTime finishedAt;
	
}