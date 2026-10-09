package com.kji.scheduler.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * External API 실행별 호출 이력과 페이징 정보를 담는다.
 */
@Data
@AllArgsConstructor
public class ExternalApiCallHistoryPageDto {
	
	private List<ExternalApiCallHistorySummaryDto> content;
	private long totalCount;
	private int page;
	private int size;
	
}
