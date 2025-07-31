package com.example.demo.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SchedulerInitializer {
	
	//private final Scheduler scheduler;
    //private final DynamicJobService dynamicJobService; // DB에서 Job 정보 조회
    
    @EventListener(ApplicationReadyEvent.class)
    public void initializeScheduler() {
    	//dynamicJobService.getAllScheduledJobs();
    	System.out.println("=====================ApplicationReadyEvent====================");
    	
    }
    
}
