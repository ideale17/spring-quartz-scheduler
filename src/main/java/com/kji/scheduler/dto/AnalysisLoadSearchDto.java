package com.kji.scheduler.dto;

import java.time.LocalDate;

import lombok.Data;

/**
 * 데이터 적재 대상 검색조건 및 페이징 정보를 담는다.
 */
@Data
public class AnalysisLoadSearchDto {
	
	private Long analysisModelId;
	private LocalDate startDate;
	private LocalDate endDate;
	private String loadStatus;
	private int page = 1;
	private int size = 10;
	
	public long getOffset() {
		return ((long) page - 1) * size;
	}
	
}
