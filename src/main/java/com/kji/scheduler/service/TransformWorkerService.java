package com.kji.scheduler.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.kji.scheduler.dto.TransformTaskDto;

@Service
public class TransformWorkerService {
	
	private static final Logger log = LoggerFactory.getLogger(TransformWorkerService.class);
	
	private final TransformTaskStateService stateService;
	private final TransformTaskExecutionService executionService;
	
	public TransformWorkerService(TransformTaskStateService stateService,
			TransformTaskExecutionService executionService) {
		this.stateService = stateService;
		this.executionService = executionService;
	}
	
	/**
	 * 대기 중인 가공 작업을 최대 지정 건수만큼 선점하여 순차 처리한다.
	 * 작업별 실행 트랜잭션을 분리해 일부 작업이 실패해도 나머지는 계속 처리한다.
	 *
	 * @param batchSize 최대 처리 건수
	 * @return 선점한 작업 건수
	 */
	public int processBatch(int batchSize) {
		
	    // 1. 대기 중인 작업을 일괄 선점한다.
	    List<TransformTaskDto> tasks = stateService.claimPendingTasks(batchSize);
	    
	    // 2. 선점한 작업별로 독립된 실행 트랜잭션에서 가공한다.
	    for (TransformTaskDto task : tasks) {
	        Long transformTaskId = task.getTransformTaskId();
	        
	        try {
	            executionService.executeTask(transformTaskId);
	        } catch (Exception e) {
	            log.error("Raw Data 가공 실패. transformTaskId: {}", transformTaskId, e);
	            
	            try {
	                stateService.markFailed(transformTaskId, e.getMessage());
	            } catch (Exception statusException) {
	                log.error("가공 실패 상태 기록 실패. transformTaskId: {}", transformTaskId, statusException);
	            }
	        }
	    }
	    
	    // 3. 선점한 작업 건수를 반환한다.
	    return tasks.size();
	}
	
}