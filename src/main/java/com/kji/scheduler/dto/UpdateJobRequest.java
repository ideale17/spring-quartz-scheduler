package com.kji.scheduler.dto;

import java.util.Map;

import lombok.Data;

/**
 * Quartz Job의 스케줄 수정 요청 정보를 담는다.
 */
@Data
public class UpdateJobRequest {
	
    private String jobName;
    private String jobGroup = "default";
    private ScheduleType scheduleType;
    private String scheduleExpr;
    private MisfirePolicy misfirePolicy = MisfirePolicy.SMART_POLICY;
    
    private Map<String, Object> params;  // 동적 JobDataMap
}
