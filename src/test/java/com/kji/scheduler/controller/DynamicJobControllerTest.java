package com.kji.scheduler.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kji.scheduler.dto.JobBatchResultDto;
import com.kji.scheduler.dto.JobTargetDto;
import com.kji.scheduler.dto.JobBatchRequest;
import com.kji.scheduler.dto.JobBatchResponse;
import com.kji.scheduler.service.DynamicJobService;

class DynamicJobControllerTest {
	
	private DynamicJobService dynamicJobService;
	private MockMvc mockMvc;
	private ObjectMapper objectMapper;
	
	@BeforeEach
	void setUp() {
		
		dynamicJobService = org.mockito.Mockito.mock(DynamicJobService.class);
		
		DynamicJobController controller = new DynamicJobController(dynamicJobService);
		
		mockMvc = MockMvcBuilders
				.standaloneSetup(controller)
				.setMessageConverters(
						new StringHttpMessageConverter(StandardCharsets.UTF_8),
						new MappingJackson2HttpMessageConverter()
				)
				.build();
		
		objectMapper = new ObjectMapper();
	}
	
	@Test
	void runJobs_정상요청이면_200과실행결과를반환한다() throws Exception {
		
		// 1. 요청 데이터를 생성한다.
		JobTargetDto target = new JobTargetDto();
		target.setJobName("job1");
		target.setJobGroup("group1");
		
		JobBatchRequest request = new JobBatchRequest();
		request.setJobs(List.of(target));
		
		// 2. Service가 반환할 실행 결과를 생성한다.
		JobBatchResultDto result = new JobBatchResultDto();
		result.setJobName("job1");
		result.setJobGroup("group1");
		result.setSuccess(true);
		result.setMessage("실행 요청 성공");
		
		JobBatchResponse response = new JobBatchResponse();
		response.setTotalCount(1);
		response.setSuccessCount(1);
		response.setFailCount(0);
		response.setResults(List.of(result));
		
		when(dynamicJobService.runJobs(any(JobBatchRequest.class)))
				.thenReturn(response);
		
		// 3. 일괄 즉시 실행 API를 호출하고 응답을 검증한다.
		mockMvc.perform(
				post("/jobs/runJobs")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
		)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalCount").value(1))
				.andExpect(jsonPath("$.successCount").value(1))
				.andExpect(jsonPath("$.failCount").value(0))
				.andExpect(jsonPath("$.results[0].jobName").value("job1"))
				.andExpect(jsonPath("$.results[0].success").value(true));
	}
	
	@Test
	void runJobs_실행할Job이없으면_400을반환한다() throws Exception {
		
		// 1. 빈 요청 데이터를 생성한다.
		JobBatchRequest request = new JobBatchRequest();
		request.setJobs(List.of());
		
		// 2. Service에서 잘못된 요청 예외가 발생하도록 설정한다.
		when(dynamicJobService.runJobs(any(JobBatchRequest.class)))
				.thenThrow(new IllegalArgumentException(
						"즉시 실행할 Job을 하나 이상 선택해야 합니다."
				));
		
		// 3. API 호출 결과가 400인지 검증한다.
		mockMvc.perform(
				post("/jobs/runJobs")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
		)
				.andExpect(status().isBadRequest())
				.andExpect(content().string(
						"잘못된 요청: 즉시 실행할 Job을 하나 이상 선택해야 합니다."
				));
	}
	
	@Test
	void runJobs_서버오류가발생하면_500을반환한다() throws Exception {
		
		// 1. 정상 형태의 요청 데이터를 생성한다.
		JobTargetDto target = new JobTargetDto();
		target.setJobName("job1");
		target.setJobGroup("group1");
		
		JobBatchRequest request = new JobBatchRequest();
		request.setJobs(List.of(target));
		
		// 2. Service에서 예상하지 못한 오류가 발생하도록 설정한다.
		when(dynamicJobService.runJobs(any(JobBatchRequest.class)))
				.thenThrow(new RuntimeException("테스트 오류"));
		
		// 3. API 호출 결과가 500인지 검증한다.
		mockMvc.perform(
				post("/jobs/runJobs")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
		)
				.andExpect(status().isInternalServerError())
				.andExpect(content().string(
						"서버 내부 오류: 테스트 오류"
				));
	}
	
}