package com.kji.scheduler.dto;

import java.time.LocalDate;

import lombok.Data;

/**
 * 가공 결과 목록 검색조건 및 페이징 정보를 담는다.
 */
@Data
public class TransformResultSearchDto {
	
	private Long externalApiId;
	private Long transformRuleId;
	private LocalDate startDate;
	private LocalDate endDate;
	private int page = 1;
	private int size = 10;
	
	public long getOffset() {
		return ((long) page - 1) * size;
	}
	
}