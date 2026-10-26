package com.kji.scheduler.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * AI 분석 대상 데이터 검색조건을 담는다.
 */
@Data
public class AiAnalysisSearchDto {
	
	private Long externalApiId;
	private Long transformRuleId;
	private Integer versionNo;
	
	private LocalDate startDate;
	private LocalDate endDate;
	
	private LocalDateTime startAt;
	private LocalDateTime endAt;
	private int limit;
	
}