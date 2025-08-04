package com.kji.scheduler.dto;

import java.util.Map;

import lombok.Data;

@Data
public class JobInfoDto {
	
    // Job 정보
    private String jobName;
    private String jobGroup;
    private String jobClassName;
    private String description;
    private String isDurable;       // Oracle에서 CHAR 또는 VARCHAR2 타입이면 String으로 받는 게 안전
    //private Map<String, Object> jobData;         // BLOB 또는 CLOB일 경우 String (필요시 변환)
    private byte[] jobData;
    private String state;
    
    // Trigger 정보
    private String triggerName;
    private String triggerGroup;
    private String triggerState;
    private String triggerType;
    private String startTime;
    private String endTime;
    private String nextFireTime;
    private String prevFireTime;
    private Integer misfireInstr;
    private Integer priority;
    
    // CronTrigger 전용
    private String cronExpression;
    private String timeZoneId;
    
    // SimpleTrigger 전용
    private Integer repeatCount;
    private Long repeatInterval;
    private Integer timesTriggered;
    
}
