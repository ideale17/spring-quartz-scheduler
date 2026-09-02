package com.kji.scheduler.dto;

import java.util.Map;

import lombok.Data;

@Data
public class UpdateJobRequest {
	
    private String jobName;
    private String jobGroup = "default";
    private ScheduleType scheduleType;
    private String scheduleExpr;

    private Map<String, Object> params;  // 동적 JobDataMap
}
