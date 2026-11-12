package com.kji.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kji.scheduler.dto.ExternalApiDto;
import com.kji.scheduler.dto.ExternalApiExecutionLogDto;
import com.kji.scheduler.dto.ExternalApiExecutionSnapshotDto;
import com.kji.scheduler.mapper.ExternalApiExecutionLogMapper;
import com.kji.scheduler.mapper.ExternalApiExecutionSnapshotMapper;
import com.kji.scheduler.dto.ExternalApiCallLogDto;
import com.kji.scheduler.dto.ExternalApiParamDto;
import com.kji.scheduler.dto.ExternalApiParamRepeatValueDto;

class ExternalApiExecutionServiceTest {

	private static final String ORIGINAL_ID = "original-run";
	private static final String SOURCE_ID = "failed-restart";
	private static final String ORIGINAL_URL = "https://example.test/original";

	private final ExternalApiService externalApiService = mock(ExternalApiService.class);
	private final ExternalApiExecutionLogMapper executionLogMapper = mock(ExternalApiExecutionLogMapper.class);
	private final ExternalApiExecutionSnapshotMapper snapshotMapper = mock(ExternalApiExecutionSnapshotMapper.class);
	private final ExternalApiRequestLogService requestLogService = mock(ExternalApiRequestLogService.class);
	private final ExternalApiCallLogService callLogService = mock(ExternalApiCallLogService.class);
	
	private ExternalApiExecutionService executionService;
	private MockRestServiceServer server;

	@BeforeEach
	void setUp() {

		// 1. 외부 HTTP 요청을 확인할 테스트 서버를 연결한다.
		RestTemplate restTemplate = new RestTemplate();
		server = MockRestServiceServer.bindTo(restTemplate).build();

		// 2. 실제 스냅샷 복원과 파라미터 조합 생성 로직을 연결한다.
		ObjectMapper objectMapper = new ObjectMapper();
		executionService = new ExternalApiExecutionService(
				externalApiService,
				new ExternalApiParameterCombinationService(new DynamicParameterResolver()),
				restTemplate,
				callLogService,
				new ExternalApiExecutionLogService(executionLogMapper),
				requestLogService,
				mock(CollectRawDataService.class),
				new ExternalApiExecutionSnapshotService(snapshotMapper, objectMapper),
				objectMapper);
	}

	@Test
	void restartUsesOriginalSnapshotAndCurrentAuthentication() {

		// 1. 실패한 재실행 이력과 최초 실행의 스냅샷을 준비한다.
		when(executionLogMapper.findExecutionById(SOURCE_ID)).thenReturn(executionLog(SOURCE_ID, ORIGINAL_ID, "FAILED"));
		when(executionLogMapper.findExecutionById(ORIGINAL_ID)).thenReturn(executionLog(ORIGINAL_ID, null, "FAILED"));
		when(snapshotMapper.findSnapshotByExecutionId(ORIGINAL_ID)).thenReturn(originalSnapshot());

		ExternalApiDto currentApi = new ExternalApiDto();
		currentApi.setExternalApiId(10L);
		currentApi.setApiUrl("https://example.test/changed");
		currentApi.setHttpMethod("POST");
		currentApi.setEnabled("Y");
		currentApi.setAuthType("API_KEY");
		currentApi.setAuthLocation("HEADER");
		currentApi.setAuthKey("X-Api-Key");
		currentApi.setAuthValue("current-test-key");

		when(externalApiService.getExternalApi(10L)).thenReturn(currentApi);
		when(externalApiService.getExternalApiParamList(10L)).thenReturn(List.of());

		// 2. 최초 기준 시각의 전날과 현재 인증 키로 1, 2페이지를 순서대로 호출해야 한다.
		expectPage("1");
		expectPage("2");

		executionService.restart(SOURCE_ID);

		// 3. 새 실행의 이력은 직접 재처리한 실행에 연결되고 성공으로 종료되어야 한다.
		server.verify();

		ArgumentCaptor<ExternalApiExecutionLogDto> logCaptor =
				ArgumentCaptor.forClass(ExternalApiExecutionLogDto.class);
		verify(executionLogMapper).updateFinish(logCaptor.capture());

		ExternalApiExecutionLogDto finished = logCaptor.getValue();
		assertNotEquals(ORIGINAL_ID, finished.getExecutionId());
		assertNotEquals(SOURCE_ID, finished.getExecutionId());
		assertEquals(SOURCE_ID, finished.getSourceExecutionId());
		assertEquals("RESTART", finished.getReprocessMode());
		assertEquals("SUCCESS", finished.getStatus());
		assertNull(finished.getFireInstanceId());

		verify(requestLogService).findOriginalRequestId(SOURCE_ID, 1, 1);
		verify(requestLogService).findOriginalRequestId(SOURCE_ID, 1, 2);
		verify(snapshotMapper, never()).insertSnapshot(any());
	}

	@ParameterizedTest
	@ValueSource(strings = {"SUCCESS", "RUNNING"})
	void restartRejectsExecutionThatHasNotFailed(String status) {

		// 1. 실패 상태가 아닌 실행 이력을 준비한다.
		when(executionLogMapper.findExecutionById(SOURCE_ID)).thenReturn(executionLog(SOURCE_ID, null, status));

		// 2. 재실행 요청을 거부하고 신규 실행 이력을 생성하지 않아야 한다.
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> executionService.restart(SOURCE_ID));

		assertEquals("실패한 External API 실행만 재처리할 수 있습니다.", exception.getMessage());
		verify(executionLogMapper, never()).insertStart(any());
		verifyNoInteractions(snapshotMapper, externalApiService, requestLogService);
	}

	private void expectPage(String page) {
		server.expect(requestTo(ORIGINAL_URL + "?date=20260314&page=" + page + "&size=10"))
				.andExpect(method(HttpMethod.GET))
				.andExpect(queryParam("page", page))
				.andExpect(header("X-Api-Key", "current-test-key"))
				.andRespond(withSuccess("{\"totalCount\":15}", MediaType.APPLICATION_JSON));
	}

	private ExternalApiExecutionLogDto executionLog(String executionId, String sourceExecutionId, String status) {
		ExternalApiExecutionLogDto executionLog = new ExternalApiExecutionLogDto();
		executionLog.setExecutionId(executionId);
		executionLog.setSourceExecutionId(sourceExecutionId);
		executionLog.setExternalApiId(10L);
		executionLog.setStatus(status);
		return executionLog;
	}

	private ExternalApiExecutionSnapshotDto originalSnapshot() {
		ExternalApiExecutionSnapshotDto snapshot = new ExternalApiExecutionSnapshotDto();
		snapshot.setExecutionId(ORIGINAL_ID);
		snapshot.setSnapshotVersion(1);
		snapshot.setExecutionBaseTime(LocalDateTime.of(2026, 3, 15, 9, 0));
		snapshot.setSnapshotData("""
				{
				  "api": {
				    "externalApiId": 10,
				    "apiUrl": "https://example.test/original",
				    "httpMethod": "GET",
				    "retryEnabled": "N",
				    "authType": "NONE"
				  },
				  "params": [{
				    "paramLocation": "QUERY",
				    "paramName": "date",
				    "valueType": "CURRENT_DATE",
				    "valueFormat": "yyyyMMdd",
				    "offsetUnit": "DAY",
				    "offsetValue": -1,
				    "requiredYn": "Y"
				  }],
				  "paging": {
				    "enabled": "Y",
				    "paginationType": "PAGE",
				    "terminationType": "TOTAL_COUNT",
				    "pageParamLocation": "QUERY",
				    "pageParamName": "page",
				    "pageStart": 1,
				    "sizeParamLocation": "QUERY",
				    "sizeParamName": "size",
				    "pageSize": 10,
				    "totalCountPath": "totalCount",
				    "maxRequestCount": 3
				  }
				}
				""");
		return snapshot;
	}
	
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void restartCreatesNewAttemptHistory(boolean succeedsOnLastAttempt) {

		// 1. 최초 실행과 재실행의 논리적 요청에 서로 다른 ID를 부여한다.
		ExternalApiDto api = restartTestApi();
		api.setRetryEnabled("Y");
		api.setMaxRetryCount(2);
		api.setRetryIntervalSec(0);

		when(externalApiService.getExternalApi(10L)).thenReturn(api);
		when(externalApiService.getExternalApiParamList(10L)).thenReturn(List.of());
		when(snapshotMapper.insertSnapshot(any())).thenReturn(1);
		when(requestLogService.saveRequest(any(), anyString(), anyInt(), anyInt(),
				isNull(), any(), anyMap(), anyMap())).thenReturn(101L, 201L);

		// 2. 최초 실행은 세 번 실패하고, 재실행은 마지막 응답만 케이스별로 달라진다.
		expectUnavailable();
		expectUnavailable();
		expectUnavailable();
		expectUnavailable();
		expectUnavailable();

		if (succeedsOnLastAttempt) {
			server.expect(requestTo(ORIGINAL_URL))
					.andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
		} else {
			expectUnavailable();
		}

		// 3. 최초 실행에서 실제로 저장한 스냅샷과 실패 이력을 재실행에 사용한다.
		assertThrows(HttpServerErrorException.class, () -> executionService.execute(10L, "quartz-run"));

		ArgumentCaptor<ExternalApiExecutionSnapshotDto> snapshotCaptor =
				ArgumentCaptor.forClass(ExternalApiExecutionSnapshotDto.class);
		verify(snapshotMapper).insertSnapshot(snapshotCaptor.capture());

		ExternalApiExecutionSnapshotDto snapshot = snapshotCaptor.getValue();
		String originalId = snapshot.getExecutionId();
		when(executionLogMapper.findExecutionById(originalId)).thenReturn(executionLog(originalId, null, "FAILED"));
		when(snapshotMapper.findSnapshotByExecutionId(originalId)).thenReturn(snapshot);
		when(requestLogService.findOriginalRequestId(originalId, 1, null)).thenReturn(101L);

		// 4. 현재 재시도 설정을 꺼도 재실행은 최초 스냅샷의 정책을 사용해야 한다.
		api.setRetryEnabled("N");

		if (succeedsOnLastAttempt) {
			executionService.restart(originalId);
		} else {
			HttpServerErrorException exception = assertThrows(
					HttpServerErrorException.class, () -> executionService.restart(originalId));
			assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
		}

		// 5. 실행별로 시도 번호가 1부터 시작하고, 같은 실행의 세 시도는 요청 ID를 공유한다.
		server.verify();

		ArgumentCaptor<ExternalApiCallLogDto> attemptCaptor = ArgumentCaptor.forClass(ExternalApiCallLogDto.class);
		verify(callLogService, times(6)).insertStart(attemptCaptor.capture());

		List<ExternalApiCallLogDto> attempts = attemptCaptor.getAllValues();
		assertEquals(List.of(1, 2, 3, 1, 2, 3), attempts.stream().map(ExternalApiCallLogDto::getAttemptNo).toList());
		assertEquals(List.of(101L, 101L, 101L, 201L, 201L, 201L),
				attempts.stream().map(ExternalApiCallLogDto::getRequestId).toList());
		assertEquals(List.of(503, 503, 503, 503, 503, succeedsOnLastAttempt ? 200 : 503),
				attempts.stream().map(ExternalApiCallLogDto::getHttpStatus).toList());

		// 6. 재실행 요청은 최초 요청에 연결되고 실행 상태는 마지막 응답에 따라 기록된다.
		ArgumentCaptor<ExternalApiExecutionLogDto> logCaptor =
				ArgumentCaptor.forClass(ExternalApiExecutionLogDto.class);
		verify(executionLogMapper, times(2)).updateFinish(logCaptor.capture());

		ExternalApiExecutionLogDto restarted = logCaptor.getAllValues().get(1);
		assertNotEquals(originalId, restarted.getExecutionId());
		assertEquals(originalId, restarted.getSourceExecutionId());
		assertEquals(succeedsOnLastAttempt ? "SUCCESS" : "FAILED", restarted.getStatus());

		verify(requestLogService).saveRequest(any(), eq(restarted.getExecutionId()), eq(1), eq(1),
				isNull(), eq(101L), eq(Map.of()), eq(Map.of()));
		verify(callLogService, times(succeedsOnLastAttempt ? 5 : 6)).markFailed(any(), anyString());
		verify(callLogService, times(succeedsOnLastAttempt ? 1 : 0)).markSuccess(any());
	}

	@Test
	void restartRunsAllParameterCombinationsFromTheBeginning() {

		// 1. 두 번째 조합에서 실패한 이전 실행과 두 LIST 파라미터를 준비한다.
		ExternalApiDto api = restartTestApi();
		when(externalApiService.getExternalApi(10L)).thenReturn(api);
		when(externalApiService.getExternalApiParamList(10L)).thenReturn(List.of());
		when(executionLogMapper.findExecutionById(SOURCE_ID)).thenReturn(executionLog(SOURCE_ID, null, "FAILED"));
		when(requestLogService.findOriginalRequestId(SOURCE_ID, 1, null)).thenReturn(101L);
		when(requestLogService.findOriginalRequestId(SOURCE_ID, 2, null)).thenReturn(102L);
		when(requestLogService.findOriginalRequestId(SOURCE_ID, 3, null)).thenReturn(null);
		when(requestLogService.findOriginalRequestId(SOURCE_ID, 4, null)).thenReturn(null);
		when(snapshotMapper.insertSnapshot(any())).thenReturn(1);

		ExternalApiExecutionSnapshotService snapshotService =
				new ExternalApiExecutionSnapshotService(snapshotMapper, new ObjectMapper());
		snapshotService.saveSnapshot(SOURCE_ID, LocalDateTime.of(2026, 3, 15, 9, 0), api,
				List.of(listParam("region", "seoul", "busan"), listParam("category", "A", "B")), null);

		ArgumentCaptor<ExternalApiExecutionSnapshotDto> snapshotCaptor =
				ArgumentCaptor.forClass(ExternalApiExecutionSnapshotDto.class);
		verify(snapshotMapper).insertSnapshot(snapshotCaptor.capture());
		when(snapshotMapper.findSnapshotByExecutionId(SOURCE_ID)).thenReturn(snapshotCaptor.getValue());

		// 2. 이전 실행에서 성공했던 첫 번째 조합부터 네 조합을 순서대로 호출한다.
		expectCombination("seoul", "A");
		expectCombination("seoul", "B");
		expectCombination("busan", "A");
		expectCombination("busan", "B");

		executionService.restart(SOURCE_ID);

		// 3. 요청 순번과 조합 순번은 1부터 시작하고, 기존 요청 ID는 앞의 두 조합에만 연결된다.
		server.verify();

		ArgumentCaptor<ExternalApiExecutionLogDto> logCaptor =
				ArgumentCaptor.forClass(ExternalApiExecutionLogDto.class);
		verify(executionLogMapper).updateFinish(logCaptor.capture());

		ExternalApiExecutionLogDto restarted = logCaptor.getValue();
		assertEquals("SUCCESS", restarted.getStatus());

		String executionId = restarted.getExecutionId();
		verify(requestLogService).saveRequest(any(), eq(executionId), eq(1), eq(1), isNull(), eq(101L),
				eq(Map.of("region", "seoul", "category", "A")), eq(Map.of()));
		verify(requestLogService).saveRequest(any(), eq(executionId), eq(2), eq(2), isNull(), eq(102L),
				eq(Map.of("region", "seoul", "category", "B")), eq(Map.of()));
		verify(requestLogService).saveRequest(any(), eq(executionId), eq(3), eq(3), isNull(), isNull(),
				eq(Map.of("region", "busan", "category", "A")), eq(Map.of()));
		verify(requestLogService).saveRequest(any(), eq(executionId), eq(4), eq(4), isNull(), isNull(),
				eq(Map.of("region", "busan", "category", "B")), eq(Map.of()));
	}

	private ExternalApiDto restartTestApi() {
		ExternalApiDto api = new ExternalApiDto();
		api.setExternalApiId(10L);
		api.setApiUrl(ORIGINAL_URL);
		api.setHttpMethod("GET");
		api.setEnabled("Y");
		api.setAuthType("NONE");
		api.setRetryEnabled("N");
		return api;
	}

	private ExternalApiParamDto listParam(String name, String firstValue, String secondValue) {
		ExternalApiParamRepeatValueDto first = new ExternalApiParamRepeatValueDto();
		first.setParamValue(firstValue);
		first.setSortOrder(1);

		ExternalApiParamRepeatValueDto second = new ExternalApiParamRepeatValueDto();
		second.setParamValue(secondValue);
		second.setSortOrder(2);

		ExternalApiParamDto param = new ExternalApiParamDto();
		param.setParamName(name);
		param.setParamLocation("QUERY");
		param.setValueType("STATIC");
		param.setRepeatType("LIST");
		param.setRequiredYn("Y");
		param.setRepeatValues(List.of(first, second));
		return param;
	}

	private void expectUnavailable() {
		server.expect(requestTo(ORIGINAL_URL))
				.andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
	}

	private void expectCombination(String region, String category) {
		server.expect(requestTo(ORIGINAL_URL + "?region=" + region + "&category=" + category))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
	}
}