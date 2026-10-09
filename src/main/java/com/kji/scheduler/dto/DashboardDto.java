package com.kji.scheduler.dto;

import java.util.List;

import lombok.Data;

/**
 * 대시보드에 표시할 Job 상태 및 실행 현황 정보를 담는다.
 */
@Data
public class DashboardDto {
	
    private int totalJobCount;
    private int normalJobCount;
    private int pausedJobCount;
    
    private long todayExecutionCount;
    private long todaySuccessCount;
    private long todayFailedCount;
    
    private List<RecentFailedJobDto> recentFailedJobs;
    
}
