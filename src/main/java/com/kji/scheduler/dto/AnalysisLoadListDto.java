package com.kji.scheduler.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 데이터 적재 대상 목록의 한 건을 담는다.
 */
@Data
public class AnalysisLoadListDto {
	
	private Long transformResultId;
	private Long transformTaskId;
	private Long rawDataId;
	private Long externalApiId;
	private String apiName;
	private Long transformRuleId;
	private String ruleName;
	private Integer versionNo;
	private String dataFormat;
	private LocalDateTime createdAt;
	private String analysisLoadStatus;
	
}