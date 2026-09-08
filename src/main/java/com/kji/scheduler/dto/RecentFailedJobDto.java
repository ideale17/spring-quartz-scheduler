package com.kji.scheduler.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class RecentFailedJobDto {
	
    private String jobName;
    private String jobGroup;
    private LocalDateTime actualFireTime;
    private String exceptionMessage;
    
}