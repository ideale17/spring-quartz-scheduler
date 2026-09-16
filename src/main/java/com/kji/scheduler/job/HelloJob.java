package com.kji.scheduler.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@DisallowConcurrentExecution
public class HelloJob implements Job{

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		
		System.out.println("🟢 HelloJob 시작: " + System.currentTimeMillis() + " / Trigger: " + context.getTrigger().getKey().getName());
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new JobExecutionException("HelloJob 실행 대기 중 오류가 발생했습니다.", e);
		}
		
		System.out.println("🔴 HelloJob 종료: " + System.currentTimeMillis() + " / Trigger: " + context.getTrigger().getKey().getName());
		
	}
	
}
