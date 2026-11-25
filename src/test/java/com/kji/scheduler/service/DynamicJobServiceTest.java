package com.kji.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

import com.kji.scheduler.dto.CreateJobRequest;
import com.kji.scheduler.dto.JobTargetDto;
import com.kji.scheduler.dto.JobBatchRequest;
import com.kji.scheduler.dto.JobBatchResponse;
import com.kji.scheduler.dto.ScheduleType;
import com.kji.scheduler.dto.UpdateJobRequest;
import com.kji.scheduler.job.TransformWorkerJob;
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
	void rejectsInvalidTransformBatchSizeBeforeScheduling() {
		CreateJobRequest request = new CreateJobRequest();
		request.setJobClassName(TransformWorkerJob.class.getName());
		request.setJobName("transform");
		request.setScheduleType(ScheduleType.SIMPLE);
		request.setScheduleExpr("60");
		request.setParams(Map.of("BATCH_SIZE", "101"));
		doReturn(TransformWorkerJob.class).when(jobClassRegistry).getJobClass(request.getJobClassName());

		assertThrows(IllegalArgumentException.class, () -> dynamicJobService.addJob(request));

		verifyNoInteractions(scheduler);
	}

	@Test
	void rejectsInvalidTransformBatchSizeBeforeChangingJobOrTrigger() throws Exception {
		UpdateJobRequest request = prepareTransformScheduleUpdate(Map.of("BATCH_SIZE", "0"));

		assertThrows(IllegalArgumentException.class, () -> dynamicJobService.updateSchedule(request));

		verify(scheduler, never()).addJob(any(JobDetail.class), anyBoolean(), anyBoolean());
		verify(scheduler, never()).rescheduleJob(any(TriggerKey.class), any(Trigger.class));
	}

	@Test
	void removesBatchSizeWhenSavingAnEmptyParameterMap() throws Exception {
		UpdateJobRequest request = prepareTransformScheduleUpdate(Map.of());
		TriggerKey triggerKey = TriggerKey.triggerKey("transformTrigger", "default");
		when(scheduler.rescheduleJob(eq(triggerKey), any(Trigger.class))).thenReturn(new Date());

		dynamicJobService.updateSchedule(request);

		ArgumentCaptor<JobDetail> savedJob = ArgumentCaptor.forClass(JobDetail.class);
		verify(scheduler).addJob(savedJob.capture(), eq(true), eq(true));
		assertFalse(savedJob.getValue().getJobDataMap().containsKey("BATCH_SIZE"));
		assertEquals(50, TransformWorkerJob.resolveBatchSize(savedJob.getValue().getJobDataMap()));
	}

	private UpdateJobRequest prepareTransformScheduleUpdate(Map<String, Object> params) throws Exception {
		UpdateJobRequest request = new UpdateJobRequest();
		request.setJobName("transform");
		request.setScheduleType(ScheduleType.SIMPLE);
		request.setScheduleExpr("60");
		request.setParams(params);

		JobKey jobKey = JobKey.jobKey("transform", "default");
		TriggerKey triggerKey = TriggerKey.triggerKey("transformTrigger", "default");
		when(scheduler.checkExists(jobKey)).thenReturn(true);
		when(scheduler.getTrigger(triggerKey))
				.thenReturn(TriggerBuilder.newTrigger().withIdentity(triggerKey).forJob(jobKey).build());
		when(scheduler.getJobDetail(jobKey))
				.thenReturn(JobBuilder.newJob(TransformWorkerJob.class)
						.withIdentity(jobKey).usingJobData("BATCH_SIZE", "80").build());
		return request;
	}
	
	@Test
	void runJobs_모든Job이존재하면_모두실행요청한다() throws Exception {
		
		// 1. 테스트용 Job 정보를 생성한다.
		JobTargetDto job1 = createTarget("job1", "group1");
		JobTargetDto job2 = createTarget("job2", "group2");
		
		JobBatchRequest request = new JobBatchRequest();
		request.setJobs(List.of(job1, job2));
		
		// 2. 두 Job 모두 Quartz에 존재하도록 설정한다.
		when(scheduler.checkExists(JobKey.jobKey("job1", "group1")))
				.thenReturn(true);
		
		when(scheduler.checkExists(JobKey.jobKey("job2", "group2")))
				.thenReturn(true);
		
		// 3. 일괄 즉시 실행을 요청한다.
		JobBatchResponse response = dynamicJobService.runJobs(request);
		
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
		JobTargetDto job1 = createTarget("job1", "group1");
		JobTargetDto job2 = createTarget("job2", "group2");
		
		JobBatchRequest request = new JobBatchRequest();
		request.setJobs(List.of(job1, job2));
		
		// 2. 첫 번째 Job만 Quartz에 존재하도록 설정한다.
		when(scheduler.checkExists(JobKey.jobKey("job1", "group1")))
				.thenReturn(true);
		
		when(scheduler.checkExists(JobKey.jobKey("job2", "group2")))
				.thenReturn(false);
		
		// 3. 일괄 즉시 실행을 요청한다.
		JobBatchResponse response = dynamicJobService.runJobs(request);
		
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
		JobBatchRequest request = new JobBatchRequest();
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
	
	private JobTargetDto createTarget(String jobName, String jobGroup) {
		
		JobTargetDto target = new JobTargetDto();
		target.setJobName(jobName);
		target.setJobGroup(jobGroup);
		
		return target;
	}
	
	@Test
	void resumeJobs_모든Job이존재하면_모두재시작한다() throws Exception {

		// 1. 테스트용 Job 정보를 생성한다.
		JobTargetDto job1 = createTarget("job1", "group1");
		JobTargetDto job2 = createTarget("job2", "group2");

		JobBatchRequest request = new JobBatchRequest();
		request.setJobs(List.of(job1, job2));

		// 2. 두 Job 모두 Quartz에 존재하도록 설정한다.
		when(scheduler.checkExists(JobKey.jobKey("job1", "group1")))
				.thenReturn(true);

		when(scheduler.checkExists(JobKey.jobKey("job2", "group2")))
				.thenReturn(true);

		// 3. 일괄 재시작을 요청한다.
		JobBatchResponse response = dynamicJobService.resumeJobs(request);

		// 4. 전체 재시작 결과를 검증한다.
		assertEquals(2, response.getTotalCount());
		assertEquals(2, response.getSuccessCount());
		assertEquals(0, response.getFailCount());

		// 5. Quartz 재시작 요청이 각각 호출됐는지 검증한다.
		verify(scheduler).resumeJob(JobKey.jobKey("job1", "group1"));
		verify(scheduler).resumeJob(JobKey.jobKey("job2", "group2"));
	}

	@Test
	void resumeJobs_일부Job이존재하지않아도_나머지는계속재시작한다() throws Exception {

		// 1. 테스트용 Job 정보를 생성한다.
		JobTargetDto job1 = createTarget("job1", "group1");
		JobTargetDto job2 = createTarget("job2", "group2");

		JobBatchRequest request = new JobBatchRequest();
		request.setJobs(List.of(job1, job2));

		// 2. 첫 번째 Job만 Quartz에 존재하도록 설정한다.
		when(scheduler.checkExists(JobKey.jobKey("job1", "group1")))
				.thenReturn(true);

		when(scheduler.checkExists(JobKey.jobKey("job2", "group2")))
				.thenReturn(false);

		// 3. 일괄 재시작을 요청한다.
		JobBatchResponse response = dynamicJobService.resumeJobs(request);

		// 4. 부분 성공 결과를 검증한다.
		assertEquals(2, response.getTotalCount());
		assertEquals(1, response.getSuccessCount());
		assertEquals(1, response.getFailCount());

		// 5. 존재하는 Job만 재시작 요청됐는지 검증한다.
		verify(scheduler).resumeJob(JobKey.jobKey("job1", "group1"));
		verify(scheduler, never()).resumeJob(JobKey.jobKey("job2", "group2"));
	}

	@Test
	void resumeJobs_재시작할Job이없으면_예외가발생한다() {

		// 1. 빈 Job 목록을 생성한다.
		JobBatchRequest request = new JobBatchRequest();
		request.setJobs(Collections.emptyList());

		// 2. 일괄 재시작 요청 시 예외가 발생하는지 검증한다.
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> dynamicJobService.resumeJobs(request)
		);

		// 3. 예외 메시지를 검증한다.
		assertEquals(
				"재시작할 Job을 하나 이상 선택해야 합니다.",
				exception.getMessage()
		);
	}
	
	@Test
	void pauseJobs_모든Job이존재하면_모두중지한다() throws Exception {

		// 1. 테스트용 Job 정보를 생성한다.
		JobTargetDto job1 = createTarget("job1", "group1");
		JobTargetDto job2 = createTarget("job2", "group2");

		JobBatchRequest request = new JobBatchRequest();
		request.setJobs(List.of(job1, job2));

		// 2. 두 Job 모두 Quartz에 존재하도록 설정한다.
		when(scheduler.checkExists(JobKey.jobKey("job1", "group1")))
				.thenReturn(true);

		when(scheduler.checkExists(JobKey.jobKey("job2", "group2")))
				.thenReturn(true);

		// 3. 일괄 중지를 요청한다.
		JobBatchResponse response = dynamicJobService.pauseJobs(request);

		// 4. 전체 중지 결과를 검증한다.
		assertEquals(2, response.getTotalCount());
		assertEquals(2, response.getSuccessCount());
		assertEquals(0, response.getFailCount());

		// 5. Quartz 중지 요청이 각각 호출됐는지 검증한다.
		verify(scheduler).pauseJob(JobKey.jobKey("job1", "group1"));
		verify(scheduler).pauseJob(JobKey.jobKey("job2", "group2"));
	}

	@Test
	void pauseJobs_일부Job이존재하지않아도_나머지는계속중지한다() throws Exception {

		// 1. 테스트용 Job 정보를 생성한다.
		JobTargetDto job1 = createTarget("job1", "group1");
		JobTargetDto job2 = createTarget("job2", "group2");

		JobBatchRequest request = new JobBatchRequest();
		request.setJobs(List.of(job1, job2));

		// 2. 첫 번째 Job만 Quartz에 존재하도록 설정한다.
		when(scheduler.checkExists(JobKey.jobKey("job1", "group1")))
				.thenReturn(true);

		when(scheduler.checkExists(JobKey.jobKey("job2", "group2")))
				.thenReturn(false);

		// 3. 일괄 중지를 요청한다.
		JobBatchResponse response = dynamicJobService.pauseJobs(request);

		// 4. 부분 성공 결과를 검증한다.
		assertEquals(2, response.getTotalCount());
		assertEquals(1, response.getSuccessCount());
		assertEquals(1, response.getFailCount());

		// 5. 존재하는 Job만 중지 요청됐는지 검증한다.
		verify(scheduler).pauseJob(JobKey.jobKey("job1", "group1"));
		verify(scheduler, never()).pauseJob(JobKey.jobKey("job2", "group2"));
	}

	@Test
	void pauseJobs_중지할Job이없으면_예외가발생한다() {

		// 1. 빈 Job 목록을 생성한다.
		JobBatchRequest request = new JobBatchRequest();
		request.setJobs(Collections.emptyList());

		// 2. 일괄 중지 요청 시 예외가 발생하는지 검증한다.
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> dynamicJobService.pauseJobs(request)
		);

		// 3. 예외 메시지를 검증한다.
		assertEquals(
				"중지할 Job을 하나 이상 선택해야 합니다.",
				exception.getMessage()
		);
	}
	
}
