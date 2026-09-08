package com.kji.scheduler.dto;

import lombok.Data;

@Data
public class DashboardExecutionSummaryDto {
	
    private long totalCount;
    private long successCount;
    private long failedCount;
    
}