package com.example.demo.dto;

import java.util.Map;

import lombok.Data;

@Data
public class ScheduleRequest {
	private String jobClassName;
    private String jobName;
    private String jobGroup = "default";
    private ScheduleType scheduleType;
    private String scheduleExpr;

    private Map<String, Object> params;  // 💡 동적 JobDataMap
}
