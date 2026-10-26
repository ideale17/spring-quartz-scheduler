package com.kji.scheduler.dto;

import java.util.List;

import lombok.Data;

/**
 * AI 분석 대상 데이터 미리보기 결과를 담는다.
 */
@Data
public class AiAnalysisPreviewDto {
	
	private int resultCount;
	private boolean hasMore;
	private List<AiAnalysisDataDto> results;
	
}