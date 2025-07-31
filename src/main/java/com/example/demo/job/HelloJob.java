package com.example.demo.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class HelloJob implements Job{

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		// TODO Auto-generated method stub
		//System.out.println("👋 HelloJob 실행됨! 현재 시간: " + System.currentTimeMillis());
		
		System.out.println("🕒 HelloJob 실행: " + System.currentTimeMillis()
        + " / Trigger: " + context.getTrigger().getKey().getName());
		
	}
	
}
