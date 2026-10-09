package com.kji.scheduler.dto;

import lombok.Data;

/**
 * Quartz Job의 실행 대상 정보를 담는다.
 */
@Data
public class JobTargetDto {
	
	private String jobName;
	private String jobGroup;
	
}