package com.kji.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kji.scheduler.dto.AnalysisLoadSourceDto;

@ExtendWith(MockitoExtension.class)
class AnalysisLoadWorkerServiceTest {
	
	@Mock
	private AnalysisLoadStateService stateService;
	
	@Mock
	private AnalysisLoadExecutionService executionService;
	
	@InjectMocks
	private AnalysisLoadWorkerService workerService;
	
	private AnalysisLoadSourceDto firstTask;
	private AnalysisLoadSourceDto secondTask;
	
	@BeforeEach
	void setUp() {
		
		// 1. 테스트에 사용할 적재 작업 정보를 생성한다.
		firstTask = new AnalysisLoadSourceDto();
		firstTask.setAnalysisLoadSourceId(1L);
		
		secondTask = new AnalysisLoadSourceDto();
		secondTask.setAnalysisLoadSourceId(2L);
		
	}
	
	@Test
	void 대기작업이_없으면_실행하지_않는다() {
		
		// 1. 대기 작업이 없는 상황을 설정한다.
		when(stateService.claimPendingLoads(50)).thenReturn(List.of());
		
		// 2. 적재 Worker를 실행한다.
		int processedCount = workerService.processBatch(50);
		
		// 3. 처리 건수가 0인지 확인한다.
		assertEquals(0, processedCount);
		
	}
	
	@Test
	void 선점한_적재작업을_순차적으로_실행한다() {
		
		// 1. 두 건의 적재 작업이 선점되도록 설정한다.
		when(stateService.claimPendingLoads(50)).thenReturn(List.of(firstTask, secondTask));
		
		// 2. 적재 Worker를 실행한다.
		int processedCount = workerService.processBatch(50);
		
		// 3. 선점된 작업이 모두 실행되었는지 확인한다.
		assertEquals(2, processedCount);
		verify(executionService).executeLoad(1L);
		verify(executionService).executeLoad(2L);
		
	}
	
	@Test
	void 첫번째_적재작업이_실패해도_다음_작업을_실행한다() {
		
		// 1. 두 건의 적재 작업이 선점되도록 설정한다.
		when(stateService.claimPendingLoads(50)).thenReturn(List.of(firstTask, secondTask));
		
		// 2. 첫 번째 적재 작업에서 예외가 발생하도록 설정한다.
		doThrow(new IllegalStateException("적재 실패")).when(executionService).executeLoad(1L);
		
		// 3. 적재 Worker를 실행한다.
		int processedCount = workerService.processBatch(50);
		
		// 4. 실패 상태가 기록되는지 확인한다.
		assertEquals(2, processedCount);
		verify(stateService).markFailed(1L, "적재 실패");
		
		// 5. 두 번째 적재 작업이 계속 실행되는지 확인한다.
		verify(executionService).executeLoad(2L);
		
	}
	
}