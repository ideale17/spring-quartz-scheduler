package com.kji.scheduler.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.kji.scheduler.service.ExternalApiExecutionService;

@DisallowConcurrentExecution
public class ExternalApiCallJob extends QuartzJobBean {
	
	private static final Logger log = LoggerFactory.getLogger(ExternalApiCallJob.class);
	
	private final ExternalApiExecutionService externalApiExecutionService;

	public ExternalApiCallJob(ExternalApiExecutionService externalApiExecutionService) {
		this.externalApiExecutionService = externalApiExecutionService;
	}
	
	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		
		try {
			
			// 1. JobDataMap에서 External API 식별자를 조회한다.
			Long externalApiId = getExternalApiId(context);
			
			// 2. Quartz 실행 인스턴스 ID를 조회한다.
			String fireInstanceId = context.getFireInstanceId();
			
			// 3. External API 실행 Service에 처리를 위임한다.
			externalApiExecutionService.execute(externalApiId, fireInstanceId);
			
		} catch (Exception e) {
			throw new JobExecutionException("External API 실행 중 오류가 발생했습니다.", e);
		}
	}
	
	// JobDataMap에서 External API 식별자를 조회한다.
	private Long getExternalApiId(JobExecutionContext context) {
		
		// 1. JobDetail에 등록된 파라미터를 조회한다.
		JobDataMap dataMap = context.getMergedJobDataMap();
		
		// 2. externalApiId 값을 조회한다.
		Object externalApiId = dataMap.get("externalApiId");
		
		// 3. externalApiId가 없으면 예외를 발생시킨다.
		if (externalApiId == null) {
			throw new IllegalArgumentException("External API 호출에 필요한 externalApiId가 없습니다.");
		}
		
		try {
			
			// 4. Quartz에 저장된 값을 Long 타입으로 변환한다.
			return Long.valueOf(externalApiId.toString());
			
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("externalApiId 형식이 올바르지 않습니다. externalApiId: " + externalApiId, e);
		}
	}
	
}