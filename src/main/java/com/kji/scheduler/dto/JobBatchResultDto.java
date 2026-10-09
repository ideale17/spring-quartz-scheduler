package com.kji.scheduler.dto;

import lombok.Data;

/**
 * 일괄 처리 대상 Job 한 건의 처리 결과를 담는다.
 */
@Data
public class JobBatchResultDto {
	
	private String jobName;
	private String jobGroup;
	private boolean success;
	private String message;
	
}