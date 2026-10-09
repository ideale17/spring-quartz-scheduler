package com.kji.scheduler.dto;

import java.util.Map;

import lombok.Data;

/**
 * Quartz Job의 기본 정보와 스케줄 설정 정보를 담는다.
 */
@Data
public class JobInfoDto {
	
    // Job 정보
    private String jobName;
    private String jobGroup;
    private String jobClassName;
    private String description;
    private String isDurable;

    private byte[] jobData;
    private Map<String, Object> params;
    private String state;
    
    // Trigger 정보
    private String triggerName;
    private String triggerGroup;
    private String triggerState;			// WAITING, ACQUIRED 같은 DB 내부 상태
    private String schedulerTriggerState;	// scheduler.getTriggerState() NORMAL, PAUSED, BLOCKED 같은 Quartz API 상태
    private String triggerType;
    private String startTime;
    private String endTime;
    private String nextFireTime;
    private String prevFireTime;
    private Integer misfireInstr;
    private MisfirePolicy misfirePolicy;
    private Integer priority;
    
    // CronTrigger 전용
    private String cronExpression;
    private String timeZoneId;
    
    // SimpleTrigger 전용
    private Integer repeatCount;
    private Long repeatInterval;
    private Integer timesTriggered;
    
}
