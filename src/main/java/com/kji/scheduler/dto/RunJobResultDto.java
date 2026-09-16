package com.kji.scheduler.dto;

import lombok.Data;

@Data
public class RunJobResultDto {
	
	private String jobName;
	private String jobGroup;
	private boolean success;
	private String message;
	
}