package com.kji.scheduler.dto;

import lombok.Data;

/**
 * 적재 결과 검색조건 및 페이징 정보를 담는다.
 */
@Data
public class AnalysisLoadResultSearchDto {
	
	private Long analysisModelId;
	
	private int page = 1;
	private int size = 50;
	
	public long getOffset() {
		return ((long) page - 1) * size;
	}
	
}