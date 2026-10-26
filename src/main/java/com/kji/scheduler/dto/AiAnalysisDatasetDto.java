package com.kji.scheduler.dto;

import java.util.List;

import lombok.Data;

/**
 * AI 분석 데이터셋의 구성 및 검증 결과를 담는다.
 */
@Data
public class AiAnalysisDatasetDto {
	
	private long totalSourceCount;
	private int sourceCount;
	private int recordCount;
	private int duplicateCandidateCount;
	private boolean hasMore;
	private List<AiAnalysisFieldDto> fields;
	
}