package com.kji.scheduler.dto;

import java.util.List;

import lombok.Data;

/**
 * Job 일괄 처리 요청에 필요한 대상 Job 목록을 담는다.
 */
@Data
public class JobBatchRequest {
	
	private List<JobTargetDto> jobs;
	
}
