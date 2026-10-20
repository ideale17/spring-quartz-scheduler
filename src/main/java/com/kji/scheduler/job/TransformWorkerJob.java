
package com.kji.scheduler.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.kji.scheduler.service.TransformWorkerService;

/**
 * Quartz 실행 주기마다 대기 중인 Raw Data 가공 작업을 일괄 처리한다.
 */
@DisallowConcurrentExecution
public class TransformWorkerJob extends QuartzJobBean {
	
	private static final Logger log = LoggerFactory.getLogger(TransformWorkerJob.class);
	private static final int BATCH_SIZE = 50;
	
	private final TransformWorkerService transformWorkerService;
	
	public TransformWorkerJob(TransformWorkerService transformWorkerService) {
		this.transformWorkerService = transformWorkerService;
	}
	
	/**
	 * 대기 중인 가공 작업을 지정된 최대 건수만큼 처리한다.
	 *
	 * @param context Quartz 실행 컨텍스트
	 * @throws JobExecutionException Worker 실행 자체에 실패한 경우
	 */
	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		
		try {
			// 1. 대기 중인 가공 작업을 최대 50건 선점하여 처리한다.
			int processedCount = transformWorkerService.processBatch(BATCH_SIZE);
			
			// 2. 이번 Quartz 실행에서 선점한 작업 건수를 기록한다.
			log.info("Raw Data 가공 Worker 실행 완료. fireInstanceId: {}, 선점 건수: {}", context.getFireInstanceId(), processedCount);
			
		} catch (Exception e) {
			// 3. Worker 실행 자체가 실패한 경우 Quartz에 예외를 전달한다.
			throw new JobExecutionException("Raw Data 가공 Worker 실행 중 오류가 발생했습니다.", e);
		}
	}
	
}
