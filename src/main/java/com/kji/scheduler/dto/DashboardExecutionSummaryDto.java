package com.kji.scheduler.dto;

import lombok.Data;

/**
 * 대시보드에서 사용할 Job 실행 결과 집계 정보를 담는다.
 */
@Data
public class DashboardExecutionSummaryDto {
	
    private long totalCount;
    private long successCount;
    private long failedCount;
    
}
