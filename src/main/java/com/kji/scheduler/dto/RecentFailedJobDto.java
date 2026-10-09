package com.kji.scheduler.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 대시보드에 표시할 최근 실패 Job의 실행 정보를 담는다.
 */
@Data
public class RecentFailedJobDto {
	
    private String jobName;
    private String jobGroup;
    private LocalDateTime actualFireTime;
    private String exceptionMessage;
    
}