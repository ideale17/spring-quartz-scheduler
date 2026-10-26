package com.kji.scheduler.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * AI 분석 대상 가공 결과 정보를 담는다.
 */
@Data
public class AiAnalysisDataDto {
	
	private Long transformResultId;
	private Long transformTaskId;
	private Long rawDataId;
	
	private Long externalApiId;
	private String apiName;
	
	private Long transformRuleId;
	private String ruleName;
	private Integer versionNo;
	private String dataFormat;
	
	private String resultData;
	private LocalDateTime createdAt;
	
}