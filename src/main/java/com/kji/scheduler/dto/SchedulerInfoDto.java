package com.kji.scheduler.dto;

import lombok.Data;

/**
 * Quartz 스케줄러의 상태 및 운영 정보를 담는다.
 */
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