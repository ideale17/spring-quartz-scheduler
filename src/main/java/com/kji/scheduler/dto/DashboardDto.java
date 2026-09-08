package com.kji.scheduler.dto;

import java.util.List;

import lombok.Data;

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