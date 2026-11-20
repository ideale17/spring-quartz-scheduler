package com.kji.scheduler.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.kji.scheduler.dto.AnalysisLoadSourceDto;

@Service
public class AnalysisLoadWorkerService {
	
	private static final Logger log = LoggerFactory.getLogger(AnalysisLoadWorkerService.class);
	
	private final AnalysisLoadStateService stateService;
	private final AnalysisLoadExecutionService executionService;
	
	public AnalysisLoadWorkerService(
			AnalysisLoadStateService stateService,
			AnalysisLoadExecutionService executionService) {
		this.stateService = stateService;
		this.executionService = executionService;
	}
	
	/**
	 * 대기 중인 적재 작업을 최대 지정 건수만큼 선점하여 순차 처리한다.
	 * 작업별 실행 트랜잭션을 분리해 일부 작업이 실패해도 나머지는 계속 처리한다.
	 *
	 * @param batchSize 최대 처리 건수
	 * @return 선점한 적재 작업 건수
	 */
	public int processBatch(int batchSize) {
		
		// 1. 대기 중인 적재 작업을 일괄 선점한다.
		List<AnalysisLoadSourceDto> tasks = stateService.claimPendingLoads(batchSize);
		
		// 2. 선점한 작업별로 독립된 실행 트랜잭션에서 적재한다.
		for (AnalysisLoadSourceDto task : tasks) {
			Long analysisLoadSourceId = task.getAnalysisLoadSourceId();
			
			try {
				executionService.executeLoad(analysisLoadSourceId);
				
			} catch (Exception e) {
				log.error("데이터 적재 실패. analysisLoadSourceId: {}", analysisLoadSourceId, e);
				
				// 3. 적재 실패 상태를 별도 트랜잭션에서 기록한다.
				try {
					stateService.markFailed(analysisLoadSourceId, e.getMessage());
					
				} catch (Exception statusException) {
					log.error("적재 실패 상태 기록 실패. analysisLoadSourceId: {}",
							analysisLoadSourceId, statusException);
				}
			}
		}
		
		// 4. 선점한 작업 건수를 반환한다.
		return tasks.size();
		
	}
	
}