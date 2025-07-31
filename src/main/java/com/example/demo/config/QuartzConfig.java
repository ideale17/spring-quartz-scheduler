package com.example.demo.config;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

@Configuration
public class QuartzConfig {
	
	@Bean
    public Scheduler scheduler(SchedulerFactoryBean factory) throws SchedulerException {
        Scheduler scheduler = factory.getScheduler();
        scheduler.start(); // 명시적으로 시작
        return scheduler;
    }
	
//	@Bean
//    public JobDetail helloJobDetail() {
//        return JobBuilder.newJob(HelloJob.class)
//                .withIdentity("helloJob")
//                .storeDurably()
//                .build();
//    }
	
	// SimpleTrigger - 5초마다 실행
//	@Bean
//    public Trigger helloJobTrigger() {
//        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
//                .withIntervalInSeconds(5)
//                .repeatForever();
//
//        return TriggerBuilder.newTrigger()
//                .forJob(helloJobDetail())
//                .withIdentity("helloJobTrigger")
//                .withSchedule(scheduleBuilder)
//                .build();
//    }
	
	// CronTrigger - 매 10초마다 실행
//    @Bean
//    public Trigger helloCronTrigger() {
//        return TriggerBuilder.newTrigger()
//                .forJob(helloJobDetail())
//                .withIdentity("helloCronTrigger")
//                .withSchedule(CronScheduleBuilder.cronSchedule("0/10 * * * * ?"))
//                .build();
//    }
	
}
