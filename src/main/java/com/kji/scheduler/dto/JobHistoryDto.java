package com.kji.scheduler.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * Job 실행 이력과 처리 결과 정보를 담는다.
 */
@Data
public class JobHistoryDto {
	
	private Long logId;
    private String fireInstanceId;
    private String jobName;
    private String jobGroup;
    private String triggerName;
    private String triggerGroup;
    private LocalDateTime scheduledFireTime;
    private LocalDateTime actualFireTime;
    private LocalDateTime finishedAt;
    private Long runMillis;
    private String status;
    private String exceptionMessage;
    private LocalDateTime createdAt;
	
	
}
