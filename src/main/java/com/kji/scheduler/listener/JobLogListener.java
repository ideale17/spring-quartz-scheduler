package com.kji.scheduler.listener;

import java.util.Optional;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.kji.scheduler.dto.JobLogDto;
import com.kji.scheduler.service.JobLogService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JobLogListener implements JobListener {
	
	private final JobLogService jobLogService;

    @Override
    public String getName() {
    	return "execution-log-job-listener";
	}

    @Override
    public void jobToBeExecuted(JobExecutionContext ctx) {
    	
    	System.out.println("===================jobToBeExecuted=======================");
    	
    	// (선택) 로그 상관분석 위해 MDC에 넣어두면 편함
        MDC.put("fireId", ctx.getFireInstanceId());
        
        // 시작 로그
    	jobLogService.insertStart(JobLogDto.fromContextStart(ctx));
    	
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext ctx) {
    	System.out.println("===================jobExecutionVetoed=======================");
    	jobLogService.markVetoed(ctx.getFireInstanceId());
    	MDC.remove("fireId");
    }

    @Override
    public void jobWasExecuted(JobExecutionContext ctx, JobExecutionException ex) {
    	System.out.println("===================jobWasExecuted=======================");
        long runMillis = ctx.getJobRunTime(); // 여기서만 유효
        if (ex == null) {
        	jobLogService.markSuccess(ctx.getFireInstanceId(), runMillis);
        } else {
        	jobLogService.markFailed(ctx.getFireInstanceId(), runMillis,
                    Optional.ofNullable(ex.getMessage()).orElse(ex.toString()));
        }
        MDC.remove("fireId");
    }
    
}
