package com.kji.scheduler.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class RawDataSearchDto {
	
	private String apiName;
	private LocalDate startDate;
	private LocalDate endDate;
	
	private int page = 1;
	private int size = 10;
	
	public int getOffset() {
		return (page - 1) * size;
	}
	
}