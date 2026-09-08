package com.kji.scheduler.dto;

import lombok.Data;

@Data
public class SchedulerInfoDto {
	
    private String schedulerName;
    private String schedulerInstanceId;
    private String schedulerStatus;
    private String quartzVersion;
    
    private String threadPoolClassName;
    private int threadPoolSize;
    
    private String jobStoreClassName;
    private boolean jobStoreSupportsPersistence;
    private boolean jobStoreClustered;
    
}