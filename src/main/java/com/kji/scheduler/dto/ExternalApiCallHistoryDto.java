package com.kji.scheduler.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ExternalApiCallHistoryDto {
	
	private Long apiCallLogId;
	private Long externalApiId;
	private String apiName;
	private String fireInstanceId;
	private Integer attemptNo;
	private Integer httpStatus;
	private LocalDateTime startedAt;
	private LocalDateTime finishedAt;
	private Long runMillis;
	private String status;
	private String errorMessage;
	private LocalDateTime createdAt;
	
}