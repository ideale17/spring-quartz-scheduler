package com.kji.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobKey;
import org.quartz.Scheduler;

import com.kji.scheduler.dto.RunJobTargetDto;
import com.kji.scheduler.dto.RunJobsRequest;
import com.kji.scheduler.dto.RunJobsResponse;
import com.kji.scheduler.mapper.DynamicJobMapper;
import com.kji.scheduler.repository.JobClassRegistry;

class DynamicJobServiceTest {
	
	private Scheduler scheduler;
	private JobClassRegistry jobClassRegistry;
	private DynamicJobMapper dynamicJobMapper;
	
	private DynamicJobService dynamicJobService;
	
	@BeforeEach
	void setUp() {
		
		scheduler = org.mockito.Mockito.mock(Scheduler.class);
		jobClassRegistry = org.mockito.Mockito.mock(JobClassRegistry.class);
		dynamicJobMapper = org.mockito.Mockito.mock(DynamicJobMapper.class);
		
		dynamicJobService = new DynamicJobService(
				scheduler,
				jobClassRegistry,
				dynamicJobMapper
		);
	}
	
	@Test
	void runJobs_모든Job이존재하면_모두실행요청한다() throws Exception {
		
		// 1. 테스트용 Job 정보를 생성한다.
		RunJobTargetDto job1 = createTarget("job1", "group1");
		RunJobTargetDto job2 = createTarget("job2", "group2");
		
		RunJobsRequest request = new RunJobsRequest();
		request.setJobs(List.of(job1, job2));
		
		// 2. 두 Job 모두 Quartz에 존재하도록 설정한다.
		when(scheduler.checkExists(JobKey.jobKey("job1", "group1")))
				.thenReturn(true);
		
		when(scheduler.checkExists(JobKey.jobKey("job2", "group2")))
				.thenReturn(true);
		
		// 3. 일괄 즉시 실행을 요청한다.
		RunJobsResponse response = dynamicJobService.runJobs(request);
		
		// 4. 전체 실행 결과를 검증한다.
		assertEquals(2, response.getTotalCount());
		assertEquals(2, response.getSuccessCount());
		assertEquals(0, response.getFailCount());
		
		// 5. 실제 Quartz 즉시 실행 요청이 각각 호출됐는지 검증한다.
		verify(scheduler).triggerJob(JobKey.jobKey("job1", "group1"));
		verify(scheduler).triggerJob(JobKey.jobKey("job2", "group2"));
	}
	
	@Test
	void runJobs_일부Job이존재하지않아도_나머지는계속실행한다() throws Exception {
		
		// 1. 테스트용 Job 정보를 생성한다.
		RunJobTargetDto job1 = createTarget("job1", "group1");
		RunJobTargetDto job2 = createTarget("job2", "group2");
		
		RunJobsRequest request = new RunJobsRequest();
		request.setJobs(List.of(job1, job2));
		
		// 2. 첫 번째 Job만 Quartz에 존재하도록 설정한다.
		when(scheduler.checkExists(JobKey.jobKey("job1", "group1")))
				.thenReturn(true);
		
		when(scheduler.checkExists(JobKey.jobKey("job2", "group2")))
				.thenReturn(false);
		
		// 3. 일괄 즉시 실행을 요청한다.
		RunJobsResponse response = dynamicJobService.runJobs(request);
		
		// 4. 부분 성공 결과를 검증한다.
		assertEquals(2, response.getTotalCount());
		assertEquals(1, response.getSuccessCount());
		assertEquals(1, response.getFailCount());
		
		// 5. 존재하는 Job만 실제 실행 요청됐는지 검증한다.
		verify(scheduler).triggerJob(JobKey.jobKey("job1", "group1"));
		verify(scheduler, never()).triggerJob(JobKey.jobKey("job2", "group2"));
	}
	
	@Test
	void runJobs_실행할Job이없으면_예외가발생한다() {
		
		// 1. 빈 Job 목록을 생성한다.
		RunJobsRequest request = new RunJobsRequest();
		request.setJobs(Collections.emptyList());
		
		// 2. 일괄 즉시 실행 요청 시 예외가 발생하는지 검증한다.
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> dynamicJobService.runJobs(request)
		);
		
		// 3. 예외 메시지를 검증한다.
		assertEquals(
				"즉시 실행할 Job을 하나 이상 선택해야 합니다.",
				exception.getMessage()
		);
	}
	
	private RunJobTargetDto createTarget(String jobName, String jobGroup) {
		
		RunJobTargetDto target = new RunJobTargetDto();
		target.setJobName(jobName);
		target.setJobGroup(jobGroup);
		
		return target;
	}
	
}