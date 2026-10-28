package com.kji.scheduler.dto;

import java.math.BigDecimal;

import lombok.Data;

/**
 * AI 숫자 필드 통계 계산 결과를 담는다.
 */
@Data
public class AiFieldStatisticsDto {
	
	private String fieldName;
	
	private long totalSourceCount;
	private int sourceCount;
	private int totalRecordCount;
	
	private int validCount;
	private int nullCount;
	private int missingCount;
	private int invalidCount;
	
	private BigDecimal sum;
	private BigDecimal min;
	private BigDecimal max;
	private BigDecimal average;
	
	private boolean complete;
}