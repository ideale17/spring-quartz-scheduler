package com.kji.scheduler.dto;

import lombok.Data;

/**
 * 데이터 적재 대상 컬럼과 변환된 값을 담는다.
 */
@Data
public class AnalysisLoadColumnValueDto {
	
	private String column;
	private Object value;
	
	public AnalysisLoadColumnValueDto(String column, Object value) {
		this.column = column;
		this.value = value;
	}
	
}