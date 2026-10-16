package com.kji.scheduler.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * Raw Data 가공 작업 정보를 담는다.
 */
@Data
public class TransformTaskDto {
	
	private Long transformTaskId;
	private Long rawDataId;
	private Long transformRuleVersionId;
	private String requestType;
	private String status;
	private Integer attemptCount;
	private LocalDateTime startedAt;
	private LocalDateTime finishedAt;
	private String errorMessage;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	
}