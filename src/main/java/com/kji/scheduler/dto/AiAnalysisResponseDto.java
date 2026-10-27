package com.kji.scheduler.dto;

import lombok.Data;

/**
 * AI 분석 실행 결과를 담는다.
 */
@Data
public class AiAnalysisResponseDto {
	
	private String answer;
	private String model;
	private long totalSourceCount;
	private int sourceCount;
	private int recordCount;
	
}