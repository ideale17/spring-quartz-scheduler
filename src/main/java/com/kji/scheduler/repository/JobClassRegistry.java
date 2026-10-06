package com.kji.scheduler.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.quartz.Job;
import org.springframework.stereotype.Component;

import com.kji.scheduler.job.ExternalApiCallJob;


@Component
public class JobClassRegistry {
	
	// Job 이름 → 클래스 매핑
    private final Map<String, Class<? extends Job>> jobClassMap = new HashMap<>();

    public JobClassRegistry() {
        // 안전하게 등록할 Job만 수동 등록 (화이트리스트 역할)
        jobClassMap.put("com.kji.scheduler.job.ExternalApiCallJob", ExternalApiCallJob.class);
        
        // 필요 시 더 추가
    }

    // 키를 기반으로 Job 클래스 반환
    public Class<? extends Job> getJobClass(String jobType) {
        Class<? extends Job> clazz = jobClassMap.get(jobType);
        if (clazz == null) {
            throw new IllegalArgumentException("등록되지 않은 job type: " + jobType);
        }
        return clazz;
    }

    // 등록된 Job 목록 확인용 (예: UI 출력용)
    public Set<String> getAvailableJobTypes() {
        return jobClassMap.keySet();
    }

    // 필요한 경우 동적으로 추가할 수 있는 register 메서드도 제공 가능
    public void register(String key, Class<? extends Job> clazz) {
        jobClassMap.put(key, clazz);
    }
    
}
