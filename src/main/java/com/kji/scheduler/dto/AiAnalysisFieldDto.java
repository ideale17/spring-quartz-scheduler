package com.kji.scheduler.dto;

import lombok.Data;

/**
 * AI 분석 데이터셋의 필드 정보를 담는다.
 */
@Data
public class AiAnalysisFieldDto {
	
	private String fieldName;
	private String dataType;
	private int presentCount;
	private int nullCount;
	private int missingCount;
	
}