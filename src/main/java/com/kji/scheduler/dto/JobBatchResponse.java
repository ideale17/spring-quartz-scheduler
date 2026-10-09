package com.kji.scheduler.dto;

import java.util.List;

import lombok.Data;

/**
 * Job 일괄 처리 결과와 성공·실패 집계 정보를 담는다.
 */
@Data
public class JobBatchResponse {
	
	private int totalCount;
	private int successCount;
	private int failCount;
	private List<JobBatchResultDto> results;
	
}