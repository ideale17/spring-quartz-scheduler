package com.kji.scheduler.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * Raw Data 가공 규칙의 버전별 설정 정보를 담는다.
 */
@Data
public class TransformRuleVersionDto {
	
	private Long transformRuleVersionId;
	private Long transformRuleId;
	private Integer versionNo;
	private String dataFormat;
	private String repeatPath;
	private LocalDateTime createdAt;
	
}