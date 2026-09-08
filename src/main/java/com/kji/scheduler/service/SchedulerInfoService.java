package com.kji.scheduler.service;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerMetaData;
import org.springframework.stereotype.Service;

import com.kji.scheduler.dto.SchedulerInfoDto;

@Service
public class SchedulerInfoService {
	
    private final Scheduler scheduler;
    
    public SchedulerInfoService(Scheduler scheduler) {
        this.scheduler = scheduler;
    }
    
    // Quartz Scheduler 운영 정보 조회
    public SchedulerInfoDto getSchedulerInfo() throws SchedulerException {
    	
        // 1. Quartz Scheduler의 메타 정보를 조회한다.
        SchedulerMetaData metaData = scheduler.getMetaData();
        
        // 2. Settings 화면에서 사용할 Scheduler 정보를 DTO에 설정한다.
        SchedulerInfoDto schedulerInfo = new SchedulerInfoDto();
        
        schedulerInfo.setSchedulerName(metaData.getSchedulerName());
        schedulerInfo.setSchedulerInstanceId(metaData.getSchedulerInstanceId());
        schedulerInfo.setSchedulerStatus(getSchedulerStatus());
        schedulerInfo.setQuartzVersion(metaData.getVersion());
        schedulerInfo.setThreadPoolClassName(metaData.getThreadPoolClass().getName());
        schedulerInfo.setThreadPoolSize(metaData.getThreadPoolSize());
        schedulerInfo.setJobStoreClassName(metaData.getJobStoreClass().getName());
        schedulerInfo.setJobStoreSupportsPersistence(metaData.isJobStoreSupportsPersistence());
        schedulerInfo.setJobStoreClustered(metaData.isJobStoreClustered());
        
        return schedulerInfo;
    }
    
    // Quartz Scheduler 현재 상태 조회
    private String getSchedulerStatus() throws SchedulerException {
    	
    	// 1. 종료된 Scheduler 상태를 먼저 확인한다.
        if (scheduler.isShutdown()) {
            return "SHUTDOWN";
        }
        
        // 2. Standby 상태인지 확인한다.
        if (scheduler.isInStandbyMode()) {
            return "STANDBY";
        }
        
        // 3. 시작된 Scheduler인지 확인한다.
        if (scheduler.isStarted()) {
            return "RUNNING";
        }
        
        return "NOT_STARTED";
        
    }
    
}
