package com.kji.scheduler.dto;

import java.time.LocalDate;

import lombok.Data;

/**
 * AI 분석 실행 요청 정보를 담는다.
 */
@Data
public class AiAnalysisRequestDto {
	
	private Long externalApiId;
	private Long transformRuleId;
	private Integer versionNo;
	
	private LocalDate startDate;
	private LocalDate endDate;
	
	private String prompt;
	
}