package com.kji.scheduler.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.kji.scheduler.dto.JobInfoDto;
import com.kji.scheduler.repository.JobClassRegistry;
import com.kji.scheduler.service.DynamicJobService;

@Component
public class SchedulerInitializer {
	
	private final Scheduler scheduler;
    private final JobClassRegistry jobClassRegistry;
    private final DynamicJobService dynamicJobService; // DB에서 Job 정보 조회
    
    public SchedulerInitializer(@Qualifier("scheduler")Scheduler scheduler, @Qualifier("jobClassRegistry")JobClassRegistry jobClassRegistry, DynamicJobService dynamicJobService) {
        this.scheduler = scheduler;
        this.jobClassRegistry = jobClassRegistry;
        this.dynamicJobService = dynamicJobService;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void initializeScheduler() {
    	
    	System.out.println("=====================ApplicationReadyEvent====================");
    	
    	List<JobInfoDto> jobList = new ArrayList<>();
    	
    	// 스케줄러 목록 DB 조회
    	jobList = dynamicJobService.getAllScheduledJobs();
    	
    	for (JobInfoDto jobInfo : jobList) {
    		
            try {
            	
            	// 1. Job 클래스 찾기 (등록되어 있어야 함)
            	Class<? extends Job> jobClass = jobClassRegistry.getJobClass(jobInfo.getJobClassName());
            	
            	// 2. JobData 역직렬화
                JobDataMap jobDataMap = new JobDataMap();  // 기본값 (비어있음)
                
                if (jobInfo.getJobData() != null) {
                	
                    ByteArrayInputStream bais = null;
                    ObjectInputStream ois = null;
                    
                    try {
                        bais = new ByteArrayInputStream(jobInfo.getJobData());
                        ois = new ObjectInputStream(bais);
                        
                        Object deserialized = ois.readObject();
                        
                        if (deserialized instanceof JobDataMap map) {
                            jobDataMap = map;
                        } else {
                            System.err.println("역직렬화된 객체가 JobDataMap이 아님: " + deserialized.getClass());
                        }
                        
                    } catch (IOException | ClassNotFoundException e) {
                        System.err.println("JobData 역직렬화 실패: " + e.getMessage());
                    } finally {
                        try {
                            if (ois != null) ois.close();
                            if (bais != null) bais.close();
                        } catch (IOException e) {
                            // 무시 또는 로깅
                        }
                    }
                }
            	
            	// 3. JobDetail 생성
                JobDetail jobDetail = JobBuilder.newJob(jobClass)
                    .withIdentity(jobInfo.getJobName(), jobInfo.getJobGroup())
                    .usingJobData(jobDataMap)
                    .build();
                
                // 4. Trigger 생성
                Trigger trigger;
                
                if(jobInfo.getTriggerType().equals("SIMPLE")) {
                	
                	//초 -> 밀리초 변경
                	long intervalMillis = jobInfo.getRepeatInterval() * 1000L;
                	
                	trigger = TriggerBuilder.newTrigger()
                            .withIdentity(jobInfo.getJobName() + "Trigger", jobInfo.getJobGroup())
                            .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInMilliseconds(intervalMillis)
                            .repeatForever())
                            .forJob(jobDetail)
                            .build();
                	
                }else if(jobInfo.getTriggerType().equals("CRON")){
                	
                    trigger = TriggerBuilder.newTrigger()
                        .withIdentity(jobInfo.getJobName() + "Trigger", jobInfo.getJobGroup())
                        .withSchedule(CronScheduleBuilder.cronSchedule(jobInfo.getCronExpression()))
                        .forJob(jobDetail)
                        .build();
                    
                }else {
                	throw new IllegalArgumentException("지원하지 않는 스케줄 형식입니다: " + jobInfo.getTriggerType());
                }
                
                if (!scheduler.checkExists(jobDetail.getKey())) {
                    scheduler.scheduleJob(jobDetail, trigger);
                    System.out.println("스케줄 등록 완료: " + jobInfo.getJobName());
                } else {
                    System.out.println("기존 등록된 작업을 유지합니다: " + jobInfo.getJobName());
                }

            } catch (Exception e) {
                System.err.println("스케줄 등록 실패: " + jobInfo.getJobName());
                e.printStackTrace();
            }
        }
    	
    }
    
}
