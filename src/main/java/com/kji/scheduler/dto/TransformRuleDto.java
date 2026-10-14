package com.kji.scheduler.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * Raw Data 가공 규칙의 기본 정보를 담는다.
 */
@Data
public class TransformRuleDto {
	
	private Long transformRuleId;
	private String ruleName;
	private String description;
	private String enabled;
	private Integer currentVersion;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	
}