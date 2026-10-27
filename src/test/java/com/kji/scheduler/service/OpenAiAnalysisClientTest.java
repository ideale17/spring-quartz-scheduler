package com.kji.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kji.scheduler.dto.AiAnalysisDatasetDto;

class OpenAiAnalysisClientTest {

	private ObjectMapper objectMapper;
	private HttpClient httpClient;
	private OpenAiAnalysisClient openAiAnalysisClient;
	private AiAnalysisDatasetDto dataset;

	@BeforeEach
	void setUp() {

		// 1. 실제 HTTP 통신 대신 Mock 클라이언트를 생성한다.
		objectMapper = new ObjectMapper();
		httpClient = mock(HttpClient.class);

		// 2. 테스트용 API 설정으로 클라이언트를 생성한다.
		openAiAnalysisClient = new OpenAiAnalysisClient(
				objectMapper,
				true,
				"test-api-key",
				"gpt-5-mini",
				30000,
				httpClient);

		// 3. 테스트용 데이터셋 정보를 구성한다.
		dataset = new AiAnalysisDatasetDto();
		dataset.setTotalSourceCount(2);
		dataset.setSourceCount(2);
		dataset.setRecordCount(3);
		dataset.setDuplicateCandidateCount(0);
	}

	@Test
	void getDatasetInfo_도구호출_성공() throws Exception {

		// 1. AI가 데이터셋 정보 조회 도구를 요청하도록 구성한다.
		String toolResponse = """
				{
				  "status": "completed",
				  "output": [
				    {
				      "type": "function_call",
				      "name": "getDatasetInfo",
				      "call_id": "call_001",
				      "arguments": "{}"
				    }
				  ]
				}
				""";

		// 2. 도구 결과를 받은 AI가 최종 답변하도록 구성한다.
		String finalResponse = """
				{
				  "status": "completed",
				  "output": [
				    {
				      "type": "message",
				      "content": [
				        {
				          "type": "output_text",
				          "text": "총 3개의 레코드가 있습니다."
				        }
				      ]
				    }
				  ]
				}
				""";

		mockResponses(toolResponse, finalResponse);

		// 3. 데이터셋 정보 분석을 실행한다.
		String answer = openAiAnalysisClient.analyze(
				"전체 레코드 건수를 알려줘.",
				dataset,
				() -> {
					throw new AssertionError("실제 데이터 조회가 실행되면 안 됩니다.");
				});

		// 4. 최종 답변과 HTTP 요청 횟수를 검증한다.
		assertEquals("총 3개의 레코드가 있습니다.", answer);

		verify(httpClient, times(2)).send(
				any(HttpRequest.class),
				any(HttpResponse.BodyHandler.class));
	}

	@Test
	void getAnalysisData_도구호출_성공() throws Exception {

		// 1. AI가 실제 분석 데이터 조회 도구를 요청하도록 구성한다.
		String toolResponse = """
				{
				  "status": "completed",
				  "output": [
				    {
				      "type": "function_call",
				      "name": "getAnalysisData",
				      "call_id": "call_002",
				      "arguments": "{}"
				    }
				  ]
				}
				""";

		String finalResponse = """
				{
				  "status": "completed",
				  "output": [
				    {
				      "type": "message",
				      "content": [
				        {
				          "type": "output_text",
				          "text": "거래금액 평균은 200입니다."
				        }
				      ]
				    }
				  ]
				}
				""";

		mockResponses(toolResponse, finalResponse);

		// 2. 실제 데이터 조회 함수의 호출 횟수를 확인한다.
		int[] dataQueryCount = {0};

		String answer = openAiAnalysisClient.analyze(
				"거래금액 평균을 계산해줘.",
				dataset,
				() -> {
					dataQueryCount[0]++;
					return "{\"results\":[{\"data\":{\"amount\":100}},{\"data\":{\"amount\":300}}]}";
				});

		// 3. 실제 데이터 조회와 AI 응답을 검증한다.
		assertEquals(1, dataQueryCount[0]);
		assertEquals("거래금액 평균은 200입니다.", answer);

		verify(httpClient, times(2)).send(
				any(HttpRequest.class),
				any(HttpResponse.BodyHandler.class));
	}

	@Test
	void 허용되지_않은_도구호출_차단() throws Exception {

		// 1. 등록되지 않은 도구를 요청하는 응답을 구성한다.
		String toolResponse = """
				{
				  "status": "completed",
				  "output": [
				    {
				      "type": "function_call",
				      "name": "executeSql",
				      "call_id": "call_003",
				      "arguments": "{}"
				    }
				  ]
				}
				""";

		mockResponses(toolResponse);

		// 2. 허용되지 않은 도구 호출이 차단되는지 확인한다.
		IllegalStateException exception = assertThrows(
				IllegalStateException.class,
				() -> openAiAnalysisClient.analyze(
						"SQL을 실행해줘.",
						dataset,
						() -> "{}"));

		assertEquals("허용되지 않은 AI 도구가 요청되었습니다.", exception.getMessage());

		verify(httpClient, times(1)).send(
				any(HttpRequest.class),
				any(HttpResponse.BodyHandler.class));
	}

	@Test
	void AI기능_비활성화시_API호출_차단() {

		// 1. AI 기능이 비활성화된 클라이언트를 생성한다.
		OpenAiAnalysisClient disabledClient = new OpenAiAnalysisClient(
				objectMapper,
				false,
				"test-api-key",
				"gpt-5-mini",
				30000,
				httpClient);

		// 2. API 호출 전에 차단되는지 확인한다.
		IllegalStateException exception = assertThrows(
				IllegalStateException.class,
				() -> disabledClient.analyze(
						"데이터를 분석해줘.",
						dataset,
						() -> "{}"));

		assertEquals("AI 분석 기능이 비활성화되어 있습니다.", exception.getMessage());

		verifyNoInteractions(httpClient);
	}

	/**
	 * OpenAI HTTP 응답을 순서대로 설정한다.
	 *
	 * @param responseBodies 테스트용 JSON 응답
	 */
	@SuppressWarnings("unchecked")
	private void mockResponses(String... responseBodies) throws Exception {

		// 1. 각 JSON 응답에 해당하는 Mock HTTP 응답을 생성한다.
		HttpResponse<String>[] responses = new HttpResponse[responseBodies.length];

		for (int i = 0; i < responseBodies.length; i++) {
			HttpResponse<String> response = mock(HttpResponse.class);
			when(response.statusCode()).thenReturn(200);
			when(response.body()).thenReturn(responseBodies[i]);
			responses[i] = response;
		}

		// 2. HTTP 요청 순서에 따라 Mock 응답을 반환한다.
		when(httpClient.send(
				any(HttpRequest.class),
				any(HttpResponse.BodyHandler.class)))
				.thenReturn(responses[0],
						java.util.Arrays.copyOfRange(responses, 1, responses.length));
	}
	
	@Test
	void getAnalysisData_도구결과_HTTP요청본문_검증() throws Exception {

		// 1. OpenAI의 도구 호출 응답을 구성한다.
		String toolResponse = """
				{
				  "status": "completed",
				  "output": [
				    {
				      "type": "function_call",
				      "name": "getAnalysisData",
				      "call_id": "call_analysis_001",
				      "arguments": "{}"
				    }
				  ]
				}
				""";

		// 2. 도구 결과를 받은 후의 AI 응답을 구성한다.
		String finalResponse = """
				{
				  "status": "completed",
				  "output": [
				    {
				      "type": "message",
				      "content": [
				        {
				          "type": "output_text",
				          "text": "거래금액 평균은 200입니다."
				        }
				      ]
				    }
				  ]
				}
				""";

		mockResponses(toolResponse, finalResponse);

		// 3. AI 도구에 전달할 실제 JSON 데이터를 구성한다.
		String analysisData = """
				{
				  "sourceCount": 2,
				  "complete": true,
				  "results": [
				    {"data": {"amount": 100}},
				    {"data": {"amount": 300}}
				  ]
				}
				""";

		// 4. 분석을 실행한다.
		String answer = openAiAnalysisClient.analyze(
				"거래금액 평균을 계산해줘.",
				dataset,
				() -> analysisData);

		assertEquals("거래금액 평균은 200입니다.", answer);

		// 5. 실제 HTTP 요청 객체 두 개를 캡처한다.
		ArgumentCaptor<HttpRequest> requestCaptor =
				ArgumentCaptor.forClass(HttpRequest.class);

		verify(httpClient, times(2)).send(
				requestCaptor.capture(),
				any(HttpResponse.BodyHandler.class));

		HttpRequest firstRequest = requestCaptor.getAllValues().get(0);
		HttpRequest secondRequest = requestCaptor.getAllValues().get(1);

		// 6. 두 번째 HTTP 요청 본문을 JSON으로 변환한다.
		String requestBody = readRequestBody(secondRequest);
		JsonNode requestJson = objectMapper.readTree(requestBody);

		JsonNode inputs = requestJson.path("input");

		assertTrue(inputs.isArray());

		// 7. 첫 번째 도구 호출 식별자와 결과 전달 정보를 확인한다.
		boolean functionCallFound = false;
		boolean toolOutputFound = false;

		for (JsonNode input : inputs) {

			if ("function_call".equals(input.path("type").asText())) {
				assertEquals("call_analysis_001", input.path("call_id").asText());
				assertEquals("getAnalysisData", input.path("name").asText());

				functionCallFound = true;
			}

			if ("function_call_output".equals(input.path("type").asText())) {
				assertEquals("call_analysis_001", input.path("call_id").asText());

				// JSON 내용을 구조적으로 비교한다.
				JsonNode actualData = objectMapper.readTree(
						input.path("output").asText());

				JsonNode expectedData = objectMapper.readTree(analysisData);

				assertEquals(expectedData, actualData);
				toolOutputFound = true;
			}
		}

		assertTrue(functionCallFound, "최초 도구 호출 정보가 누락되었습니다.");
		assertTrue(toolOutputFound, "도구 실행 결과가 누락되었습니다.");

		// 8. OpenAI 요청 설정과 API 키 노출 여부를 확인한다.
		assertEquals("gpt-5-mini", requestJson.path("model").asText());
		assertFalse(requestJson.path("store").asBoolean(true));
		assertFalse(requestBody.contains("test-api-key"));

		assertEquals("Bearer test-api-key",
				secondRequest.headers().firstValue("Authorization").orElse(null));

		// 9. 첫 번째 요청과 두 번째 요청 모두 Responses API를 사용해야 한다.
		assertEquals("https://api.openai.com/v1/responses", firstRequest.uri().toString());
		assertEquals(firstRequest.uri(), secondRequest.uri());
	}
	
	/**
	 * 테스트용 HTTP 요청 본문을 문자열로 변환한다.
	 *
	 * @param request HTTP 요청 객체
	 * @return UTF-8 요청 본문
	 * @throws Exception 요청 본문 변환에 실패한 경우
	 */
	private String readRequestBody(HttpRequest request) throws Exception {

		// 1. HTTP 요청의 BodyPublisher를 확인한다.
		HttpRequest.BodyPublisher publisher = request.bodyPublisher()
				.orElseThrow(() -> new IllegalStateException("HTTP 요청 본문이 없습니다."));

		// 2. 수신한 데이터를 저장할 객체를 생성한다.
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		CompletableFuture<String> result = new CompletableFuture<>();

		// 3. HTTP 본문을 구독하여 UTF-8 문자열로 변환한다.
		publisher.subscribe(new Flow.Subscriber<ByteBuffer>() {

			private Flow.Subscription subscription;

			@Override
			public void onSubscribe(Flow.Subscription subscription) {
				this.subscription = subscription;
				this.subscription.request(1);
			}

			@Override
			public void onNext(ByteBuffer item) {

				byte[] bytes = new byte[item.remaining()];
				item.get(bytes);
				output.write(bytes, 0, bytes.length);

				subscription.request(1);
			}

			@Override
			public void onError(Throwable throwable) {
				result.completeExceptionally(throwable);
			}

			@Override
			public void onComplete() {
				result.complete(output.toString(StandardCharsets.UTF_8));
			}
		});

		// 4. 본문 변환 결과를 반환한다.
		return result.get(5, TimeUnit.SECONDS);
	}
	
}