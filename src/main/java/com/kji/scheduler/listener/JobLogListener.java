package com.kji.scheduler.listener;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.kji.scheduler.dto.JobLogDto;
import com.kji.scheduler.service.JobLogService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
    	
    	log.debug("Job 실행 시작 처리. fireInstanceId: {}", ctx.getFireInstanceId());
    	
    	// (선택) 로그 상관분석 위해 MDC에 넣어두면 편함
        MDC.put("fireId", ctx.getFireInstanceId());
        
        // 시작 로그
    	jobLogService.insertStart(JobLogDto.fromContextStart(ctx));
    	
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext ctx) {
    	
    	log.debug("Job 실행 거부 처리. fireInstanceId: {}", ctx.getFireInstanceId());
    	
    	jobLogService.markVetoed(ctx.getFireInstanceId());
    	
    	MDC.remove("fireId");
    }

    @Override
    public void jobWasExecuted(JobExecutionContext ctx, JobExecutionException ex) {
    	
    	log.debug("Job 실행 종료 처리. fireInstanceId: {}", ctx.getFireInstanceId());
    	
        long runMillis = ctx.getJobRunTime(); // 여기서만 유효
        
        if (ex == null) {
        	jobLogService.markSuccess(ctx.getFireInstanceId(), runMillis);
        } else {
        	jobLogService.markFailed(
        			ctx.getFireInstanceId(),
        			runMillis,
        			getErrorMessage(ex)
        	);
        }
        
        MDC.remove("fireId");
    }
    
    // Job 실행 예외에서 실제 원인 메시지를 조회한다.
    private String getErrorMessage(JobExecutionException exception) {
    	
    	// 1. 가장 하위 원인 예외를 찾는다.
    	Throwable cause = exception;
    	
    	while (cause.getCause() != null) {
    		cause = cause.getCause();
    	}
    	
    	// 2. 실제 원인 메시지가 있으면 반환한다.
    	if (cause.getMessage() != null && !cause.getMessage().isBlank()) {
    		return cause.getMessage();
    	}
    	
    	// 3. 원인 메시지가 없으면 Quartz 예외 메시지를 반환한다.
    	if (exception.getMessage() != null && !exception.getMessage().isBlank()) {
    		return exception.getMessage();
    	}
    	
    	return exception.toString();
    }
    
}
