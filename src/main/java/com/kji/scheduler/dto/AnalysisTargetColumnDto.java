package com.kji.scheduler.dto;

import lombok.Data;

/**
 * 데이터 적재 대상 업무 테이블의 컬럼 정보를 담는다.
 */
@Data
public class AnalysisTargetColumnDto {
	
	private String columnName;
	private String dataType;
	private Integer ordinalPosition;
	private String description;
	private String nullable;
	private Boolean required;
	private Boolean systemManaged;
	
}