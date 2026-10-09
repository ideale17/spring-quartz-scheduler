package com.kji.scheduler.dto;

import java.util.Map;

import lombok.Data;

/**
 * Quartz Job 등록에 필요한 클래스, 스케줄 설정과 실행 파라미터를 전달한다.
 */
@Data
public class CreateJobRequest {
	private String jobClassName;
    private String jobName;
    private String jobGroup = "default";
    private ScheduleType scheduleType;
    private String scheduleExpr;
    private MisfirePolicy misfirePolicy = MisfirePolicy.SMART_POLICY;
    
    private Map<String, Object> params;  // 동적 JobDataMap
}
