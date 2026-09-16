package com.kji.scheduler.dto;

import java.util.List;

import lombok.Data;

@Data
public class RunJobsRequest {
	
	private List<RunJobTargetDto> jobs;
	
}