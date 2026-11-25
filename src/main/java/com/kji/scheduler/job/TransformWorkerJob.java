
package com.kji.scheduler.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
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
	private static final String BATCH_SIZE_PARAM = "BATCH_SIZE";
	private static final int DEFAULT_BATCH_SIZE = 50;
	private static final String INVALID_BATCH_SIZE_MESSAGE = "BATCH_SIZE는 1~100 사이의 정수여야 합니다.";
	
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
		
		// 1. 실행 파라미터를 검증한 후 배치 크기를 확정한다.
		int batchSize;
		
		try {
			batchSize = resolveBatchSize(context.getMergedJobDataMap());
		} catch (IllegalArgumentException e) {
			throw new JobExecutionException(e.getMessage(), e);
		}
		
		try {
			// 2. 대기 중인 가공 작업을 지정한 건수만큼 선점하여 처리한다.
			int claimedCount = transformWorkerService.processBatch(batchSize);
			
			// 3. 설정한 배치 크기와 실제 선점 건수를 기록한다.
			log.info("Raw Data 가공 Worker 실행 완료. fireInstanceId: {}, 배치 크기: {}, 선점 건수: {}",
					context.getFireInstanceId(), batchSize, claimedCount);
			
		} catch (Exception e) {
			// 4. Worker 실행 오류를 Quartz에 전달한다.
			throw new JobExecutionException("Raw Data 가공 Worker 실행 중 오류가 발생했습니다.", e);
		}
	}
	
	// Job 저장과 실행에 동일한 배치 크기 규칙을 적용한다.
	public static int resolveBatchSize(JobDataMap dataMap) {
		
		if (!dataMap.containsKey(BATCH_SIZE_PARAM)) return DEFAULT_BATCH_SIZE;
		
		int batchSize;
		
		try {
			batchSize = Integer.parseInt(String.valueOf(dataMap.get(BATCH_SIZE_PARAM)).trim());
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(INVALID_BATCH_SIZE_MESSAGE);
		}
		
		if (batchSize < 1 || batchSize > 100) {
			throw new IllegalArgumentException(INVALID_BATCH_SIZE_MESSAGE);
		}
		
		return batchSize;
	}
	
}
