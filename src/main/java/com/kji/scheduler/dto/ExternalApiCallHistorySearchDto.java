package com.kji.scheduler.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class ExternalApiCallHistorySearchDto {
	
	private String apiName;
	private String status;
	private LocalDate startDate;
	private LocalDate endDate;
	private LocalDate endDateExclusive;
	
	private int page = 1;
	private int size = 10;
	
	public int getOffset() {
		return (page - 1) * size;
	}
	
}