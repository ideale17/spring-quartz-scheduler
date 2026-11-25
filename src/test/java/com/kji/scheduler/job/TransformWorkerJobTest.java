package com.kji.scheduler.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;

import com.kji.scheduler.service.TransformWorkerService;

class TransformWorkerJobTest {

	private final TransformWorkerService worker = mock(TransformWorkerService.class);
	private final TransformWorkerJob job = new TransformWorkerJob(worker);
	private final JobExecutionContext context = mock(JobExecutionContext.class);
	private final JobDataMap parameters = new JobDataMap();

	@BeforeEach
	void setUp() throws Exception {
		Scheduler scheduler = mock(Scheduler.class);
		when(context.getScheduler()).thenReturn(scheduler);
		when(scheduler.getContext()).thenReturn(new SchedulerContext());
		when(context.getMergedJobDataMap()).thenReturn(parameters);
	}

	@Test
	void defaultsToFiftyWhenBatchSizeIsMissing() throws Exception {
		job.execute(context);

		verify(worker).processBatch(50);
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 80, 100})
	void usesBatchSizeFromJobDataMap(int batchSize) throws Exception {
		parameters.put("BATCH_SIZE", String.valueOf(batchSize));

		job.execute(context);

		verify(worker).processBatch(batchSize);
	}

	@Test
	void supportsAnIntegerBatchSize() throws Exception {
		parameters.put("BATCH_SIZE", 25);

		job.execute(context);

		verify(worker).processBatch(25);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"", "0", "-1", "101", "1.5", "abc"})
	void rejectsInvalidBatchSizeBeforeProcessing(String batchSize) {
		parameters.put("BATCH_SIZE", batchSize);

		JobExecutionException exception = assertThrows(JobExecutionException.class, () -> job.execute(context));

		assertEquals("BATCH_SIZE는 1~100 사이의 정수여야 합니다.", exception.getMessage());
		verifyNoInteractions(worker);
	}
}
