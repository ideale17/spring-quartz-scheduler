package com.kji.scheduler.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * Raw Data의 가공 작업 이력 정보를 담는다.
 */
@Data
public class TransformTaskHistoryDto {
	
	private Long transformTaskId;
	private Long rawDataId;
	private Long transformRuleId;
	private String ruleName;
	private Integer versionNo;
	private String status;
	private Integer attemptCount;
	private String errorMessage;
	private Long transformResultId;
	private LocalDateTime createdAt;
	private LocalDateTime startedAt;
	private LocalDateTime finishedAt;
	
}