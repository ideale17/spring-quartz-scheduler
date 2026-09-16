package com.kji.scheduler.dto;

import java.util.List;

import lombok.Data;

@Data
public class RunJobsResponse {
	
	private int totalCount;
	private int successCount;
	private int failCount;
	private List<RunJobResultDto> results;
	
}