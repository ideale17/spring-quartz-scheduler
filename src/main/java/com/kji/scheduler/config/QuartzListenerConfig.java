package com.kji.scheduler.config;

import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.kji.scheduler.listener.JobLogListener;

@Configuration
public class QuartzListenerConfig {
	
	@Bean
    SchedulerFactoryBeanCustomizer quartzCustomizer(JobLogListener jobLogListener) {
        return factory -> factory.setGlobalJobListeners(jobLogListener);
    }
	
}
