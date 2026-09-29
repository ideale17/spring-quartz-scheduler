package com.kji.scheduler.service;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.kji.scheduler.dto.CollectRawDataDto;
import com.kji.scheduler.dto.ExternalApiCallLogDto;
import com.kji.scheduler.dto.ExternalApiDto;
import com.kji.scheduler.dto.ExternalApiParamDto;

/**
 * External API 요청 생성, 인증 적용 및 호출을 처리한다.
 *
 * @author kji
 * @since 2026. 9. 28.
 */
@Service
public class ExternalApiExecutionService {
	
	private static final Logger log = LoggerFactory.getLogger(ExternalApiExecutionService.class);
	
	private final RestTemplate restTemplate;
	private final ExternalApiService externalApiService;
	private final DynamicParameterResolver dynamicParameterResolver;
	private final ExternalApiCallLogService externalApiCallLogService;
	private final CollectRawDataService collectRawDataService;
	
	public ExternalApiExecutionService(ExternalApiService externalApiService,
			DynamicParameterResolver dynamicParameterResolver,
			RestTemplate restTemplate,
			ExternalApiCallLogService externalApiCallLogService,
			CollectRawDataService collectRawDataService) {
		this.externalApiService = externalApiService;
		this.dynamicParameterResolver = dynamicParameterResolver;
		this.restTemplate = restTemplate;
		this.externalApiCallLogService = externalApiCallLogService;
		this.collectRawDataService = collectRawDataService;
	}
	
	/**
	 * External API를 호출하고 호출 이력을 저장한다.
	 *
	 * @param externalApiId		외부 API 식별자
	 * @param fireInstanceId 	Quartz 실행 인스턴스 식별자
	 */
	public void execute(Long externalApiId, String fireInstanceId) {
		
		// 1. External API 실행 기준 시간과 실행 식별자를 한 번만 생성한다.
		LocalDateTime executionTime = LocalDateTime.now();
		String executionId = UUID.randomUUID().toString();
		
		// 2. External API 기본 정보를 조회한다.
		ExternalApiDto externalApi = externalApiService.getExternalApi(externalApiId);
		
		// 3. External API 사용 가능 여부를 확인한다.
		if (!"Y".equals(externalApi.getEnabled())) {
			throw new IllegalStateException("사용 중지된 External API입니다. externalApiId: " + externalApiId);
		}
		
		// 4. External API 인증 설정을 검증한다.
		validateAuthentication(externalApi);
		
		// 5. External API 파라미터 목록을 조회한다.
		List<ExternalApiParamDto> params = externalApiService.getExternalApiParamList(externalApiId);
		
		// 6. 요청 위치별 파라미터를 저장할 Map을 생성한다.
		Map<String, String> headers = new LinkedHashMap<>();
		Map<String, String> queryParams = new LinkedHashMap<>();
		Map<String, String> bodyParams = new LinkedHashMap<>();
		
		// 7. 각 파라미터의 실제 실행 값을 생성한다.
		for (ExternalApiParamDto param : params) {
			
			String resolvedValue = dynamicParameterResolver.resolve(param, executionTime);
			
			// 8. 필수 파라미터의 값 존재 여부를 확인한다.
			validateRequiredParam(param, resolvedValue);
			
			// 9. 값이 없는 선택 파라미터는 요청에서 제외한다.
			if (resolvedValue == null) {
				continue;
			}
			
			// 10. 파라미터 전달 위치에 따라 요청 정보를 분리한다.
			switch (param.getParamLocation()) {
			
				case "HEADER" ->
					headers.put(param.getParamName(), resolvedValue);
				
				case "QUERY" ->
					queryParams.put(param.getParamName(), resolvedValue);
				
				case "BODY" ->
					bodyParams.put(param.getParamName(), resolvedValue);
				
				default ->
					throw new IllegalArgumentException(
							"지원하지 않는 External API 파라미터 위치입니다. "
									+ "paramName: " + param.getParamName()
									+ ", paramLocation: " + param.getParamLocation()
					);
			}
		}
		
		// 11. External API 인증 정보를 요청에 적용한다.
		applyAuthentication(externalApi, headers, queryParams);
		
		// 12. External API 요청 정보 생성 결과를 기록한다.
		log.info(
				"External API 요청 정보 생성 완료. "
						+ "externalApiId: {}, apiName: {}, method: {}, "
						+ "headerParams: {}, queryParams: {}, bodyParams: {}",
				externalApiId,
				externalApi.getApiName(),
				externalApi.getHttpMethod(),
				headers.keySet(),
				queryParams.keySet(),
				bodyParams.keySet()
		);
		
		// 13. External API 최대 호출 횟수를 계산한다.
		int maxAttempts = 1;
		
		if ("Y".equals(externalApi.getRetryEnabled())) {
			maxAttempts += externalApi.getMaxRetryCount();
		}
		
		// 14. External API를 호출하고 실패 시 설정된 정책에 따라 재시도한다.
		for (int attemptNo = 1; attemptNo <= maxAttempts; attemptNo++) {
			try {
				executeAttempt(externalApi, executionId, fireInstanceId, attemptNo, headers, queryParams, bodyParams);
				return;
				
			} catch (RestClientResponseException | ResourceAccessException e) {
				
				// 15. 재시도 가능한 오류인지 확인한다.
				boolean retryable = isRetryable(e);
				boolean hasNextAttempt = attemptNo < maxAttempts;
				
				if (!retryable || !hasNextAttempt) {
					throw e;
				}
				
				// 16. 다음 호출 전 재시도 간격만큼 대기한다.
				waitRetryInterval(externalApi, attemptNo);
			}
		}
		
	}
	
	/**
	 * External API 실행에 필요한 인증 정보를 검증한다.
	 *
	 * @param externalApi	인증 설정이 포함된 External API 정보
	 */
	private void validateAuthentication(ExternalApiDto externalApi) {
		
	    String authType = externalApi.getAuthType();
	    
	    // 1. 인증 방식이 없으면 인증 없음으로 처리한다.
	    if (authType == null || authType.isBlank() || "NONE".equals(authType)) {
	        return;
	    }
	    
	    // 2. 인증 방식별 필수 정보를 확인한다.
	    switch (authType) {
	    
	        case "API_KEY" -> {
	        	
	        	String authLocation = externalApi.getAuthLocation();
	        	
	        	// 1. API Key 전달 위치가 설정되어 있는지 확인한다.
	            if (authLocation == null || authLocation.isBlank()) {
	                throw new IllegalStateException("API Key 전달 위치가 설정되지 않았습니다. externalApiId: " + externalApi.getExternalApiId());
	            }
	            
	            // 2. 지원하는 API Key 전달 위치인지 확인한다.
	            if (!"HEADER".equals(authLocation) && !"QUERY".equals(authLocation)) {

	                throw new IllegalStateException(
	                        "지원하지 않는 API Key 전달 위치입니다. authLocation: "
	                                + authLocation
	                                + ", externalApiId: "
	                                + externalApi.getExternalApiId()
	                );
	            }
	            
	            // 3. API Key 이름과 값을 확인한다.
	            if (externalApi.getAuthKey() == null
	                    || externalApi.getAuthKey().isBlank()
	                    || externalApi.getAuthValue() == null
	                    || externalApi.getAuthValue().isBlank()) {
	            	
	                throw new IllegalStateException("API Key 인증 설정이 올바르지 않습니다. externalApiId: " + externalApi.getExternalApiId());
	            }
	            
	        }
	        
	        case "BEARER" -> {
	        	
	        	// 1. Bearer Token 값이 설정되어 있는지 확인한다.
	            if (externalApi.getAuthValue() == null || externalApi.getAuthValue().isBlank()) {
	                throw new IllegalStateException("Bearer Token이 설정되지 않았습니다. externalApiId: " + externalApi.getExternalApiId());
	            }
	            
	        }
	        
	        case "BASIC" -> {
	        	
	        	// 1. 사용자명이 설정되어 있는지 확인한다.
	            if (externalApi.getAuthUsername() == null || externalApi.getAuthUsername().isBlank()) {
	                throw new IllegalStateException("Basic Auth 사용자명이 설정되지 않았습니다. externalApiId: " + externalApi.getExternalApiId());
	            }
	            
	            // 2. 비밀번호가 설정되어 있는지 확인한다.
	            if (externalApi.getAuthPassword() == null || externalApi.getAuthPassword().isBlank()) {
	                throw new IllegalStateException("Basic Auth 비밀번호가 설정되지 않았습니다. externalApiId: " + externalApi.getExternalApiId());
	            }
	            
	        }
	        
	        default -> throw new IllegalStateException("지원하지 않는 External API 인증 방식입니다. authType: " + authType);
	    }
	    
	}
	
	/**
	 * External API 인증 방식에 따라 요청에 인증 정보를 적용한다.
	 *
	 * @param externalApi	인증 설정이 포함된 External API 정보
	 * @param headers		요청에 적용할 HTTP Header 정보
	 * @param queryParams	요청 URL에 적용할 Query Parameter 정보
	 */
	private void applyAuthentication(ExternalApiDto externalApi, Map<String, String> headers, Map<String, String> queryParams) {
		
	    switch (externalApi.getAuthType()) {
	    
	        case "NONE" -> {
	            return;
	        }
	        
	        case "API_KEY" -> {
	            if ("HEADER".equals(externalApi.getAuthLocation())) {
	                headers.put(externalApi.getAuthKey(), externalApi.getAuthValue());
	            } else {
	                queryParams.put(externalApi.getAuthKey(), externalApi.getAuthValue());
	            }
	        }
	        
	        case "BEARER" ->
	            headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + externalApi.getAuthValue());
	            
	        case "BASIC" ->
	            headers.put(
	                    	HttpHeaders.AUTHORIZATION,
	                    	HttpHeaders.encodeBasicAuth(externalApi.getAuthUsername(), externalApi.getAuthPassword(), null)
	                    );
	            
	    }
	    
	}
	
	/**
	 * External API를 한 번 호출하고 호출 이력을 저장한다.
	 *
	 * @param externalApi		호출할 External API 정보
	 * @param executionId		API 실행 단위 식별자
	 * @param fireInstanceId	Quartz 실행 인스턴스 식별자
	 * @param attemptNo			현재 호출 시도 횟수
	 * @param headers			요청에 적용할 HTTP Header 정보
	 * @param queryParams		요청 URL에 적용할 Query Parameter 정보
	 * @param bodyParams		요청 Body에 적용할 Parameter 정보
	 */
	private void executeAttempt(
			ExternalApiDto externalApi,
			String executionId,
			String fireInstanceId,
			int attemptNo,
			Map<String, String> headers,
			Map<String, String> queryParams,
			Map<String, String> bodyParams) {
		
		// 1. External API 호출 시작 이력을 저장한다.
		ExternalApiCallLogDto callLog = new ExternalApiCallLogDto();
		callLog.setExternalApiId(externalApi.getExternalApiId());
		callLog.setExecutionId(executionId);
		callLog.setFireInstanceId(fireInstanceId);
		callLog.setAttemptNo(attemptNo);
		
		externalApiCallLogService.insertStart(callLog);
		
		// 2. External API 호출 소요 시간 측정을 시작한다.
		long startTime = System.nanoTime();
		
		try {
			
			// 3. External API를 호출한다.
			ResponseEntity<String> response = callExternalApi(externalApi, headers, queryParams, bodyParams);
			
			// 4. External API 응답 원본 데이터를 구성한다.
			CollectRawDataDto rawData = new CollectRawDataDto();
			rawData.setExecutionId(executionId);
			rawData.setExternalApiId(externalApi.getExternalApiId());
			rawData.setResponseBody(response.getBody());
			
			if (response.getHeaders().getContentType() != null) {
				rawData.setContentType(response.getHeaders().getContentType().toString());
			}
			
			// 5. External API 응답 원본 데이터를 저장한다.
			collectRawDataService.insertCollectRawData(rawData);
			
			// 6. External API 호출 성공 정보를 저장한다.
			long runMillis = (System.nanoTime() - startTime) / 1_000_000;
			
			callLog.setHttpStatus(response.getStatusCode().value());
			callLog.setRunMillis(runMillis);
			
			externalApiCallLogService.markSuccess(callLog);
			
			// 7. External API 호출 결과를 기록한다.
			log.info(
					"External API 호출 완료. externalApiId: {}, apiName: {}, attemptNo: {}, statusCode: {}, runMillis: {}",
					externalApi.getExternalApiId(),
					externalApi.getApiName(),
					attemptNo,
					response.getStatusCode(),
					runMillis
			);
			
		} catch (RestClientResponseException e) {
			
			// 8. HTTP 오류 응답 정보를 저장한다.
			long runMillis = (System.nanoTime() - startTime) / 1_000_000;
			String errorMessage = buildHttpErrorMessage(e);
			
			callLog.setHttpStatus(e.getStatusCode().value());
			callLog.setRunMillis(runMillis);
			
			externalApiCallLogService.markFailed(callLog, errorMessage);
			
			log.error(
					"External API 호출 실패. externalApiId: {}, apiName: {}, attemptNo: {}, statusCode: {}, responseBody: {}",
					externalApi.getExternalApiId(),
					externalApi.getApiName(),
					attemptNo,
					e.getStatusCode(),
					e.getResponseBodyAsString(),
					e
			);
			
			throw e;
			
		} catch (ResourceAccessException e) {
			
			// 9. External API 통신 오류 정보를 저장한다.
			long runMillis = (System.nanoTime() - startTime) / 1_000_000;
			String errorMessage = buildResourceAccessErrorMessage(e);
			
			callLog.setRunMillis(runMillis);
			
			externalApiCallLogService.markFailed(callLog, errorMessage);
			
			log.error(
					"External API 통신 실패. externalApiId: {}, apiName: {}, attemptNo: {}",
					externalApi.getExternalApiId(),
					externalApi.getApiName(),
					attemptNo,
					e
			);
			
			throw e;
			
		} catch (Exception e) {
			
			// 10. External API 호출 중 발생한 오류 정보를 저장한다.
			long runMillis = (System.nanoTime() - startTime) / 1_000_000;
			String errorMessage = buildErrorMessage(e);
			
			callLog.setRunMillis(runMillis);
			
			externalApiCallLogService.markFailed(callLog, errorMessage);
			
			log.error(
					"External API 호출 중 오류 발생. externalApiId: {}, apiName: {}, attemptNo: {}",
					externalApi.getExternalApiId(),
					externalApi.getApiName(),
					attemptNo,
					e
			);
			
			throw e;
		}
	}
	
	/**
	 * External API 호출 오류가 재시도 대상인지 확인한다.
	 *
	 * @param e	External API 호출 중 발생한 예외
	 * @return	재시도 가능한 오류이면 true, 아니면 false
	 */
	private boolean isRetryable(Exception e) {
		
		// 1. 연결 실패나 응답 시간 초과 등의 통신 오류는 재시도한다.
		if (e instanceof ResourceAccessException) {
			return true;
		}
		
		// 2. HTTP 오류가 아니면 재시도하지 않는다.
		if (!(e instanceof RestClientResponseException responseException)) {
			return false;
		}
		
		// 3. 일시적인 HTTP 오류만 재시도한다.
		int statusCode = responseException.getStatusCode().value();
		
		return statusCode == 408
				|| statusCode == 429
				|| responseException.getStatusCode().is5xxServerError();
	}
	
	/**
	 * 다음 External API 재시도 전 설정된 시간만큼 대기한다.
	 *
	 * @param externalApi	재시도 설정이 포함된 External API 정보
	 * @param attemptNo		현재 호출 시도 횟수
	 */
	private void waitRetryInterval(ExternalApiDto externalApi, int attemptNo) {
		
		long retryIntervalMillis = externalApi.getRetryIntervalSec() * 1000L;
		
		log.info(
				"External API 재시도 대기. externalApiId: {}, apiName: {}, attemptNo: {}, retryIntervalSec: {}",
				externalApi.getExternalApiId(),
				externalApi.getApiName(),
				attemptNo,
				externalApi.getRetryIntervalSec()
		);
		
		try {
			Thread.sleep(retryIntervalMillis);
			
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("External API 재시도 대기 중 인터럽트가 발생했습니다.", e);
		}
		
	}
	
	/**
	 * HTTP 오류 응답의 저장용 메시지를 생성한다.
	 *
	 * @param e	External API HTTP 오류 응답 예외
	 * @return	HTTP 상태에 대응하는 저장용 오류 메시지
	 */
	private String buildHttpErrorMessage(RestClientResponseException e) {
		int statusCode = e.getStatusCode().value();
		
		return switch (statusCode) {
			case 400 -> "외부 API 요청 정보 오류";
			case 401 -> "외부 API 인증 실패";
			case 403 -> "외부 API 접근 권한 없음";
			case 404 -> "외부 API 요청 대상 없음";
			case 408 -> "외부 API 요청 시간 초과";
			case 429 -> "외부 API 호출 한도 초과";
			case 502 -> "외부 API 게이트웨이 오류";
			case 503 -> "외부 API 서비스 사용 불가";
			case 504 -> "외부 API 응답 시간 초과";
			default -> {
				if (e.getStatusCode().is4xxClientError()) {
					yield "외부 API 요청 오류";
				}
				
				if (e.getStatusCode().is5xxServerError()) {
					yield "외부 API 서버 오류";
				}
				
				yield "외부 API 호출 실패";
			}
		};
	}
	
	/**
	 * External API 통신 오류의 저장용 메시지를 생성한다.
	 *
	 * @param e	External API 통신 중 발생한 접근 예외
	 * @return	통신 오류 원인에 대응하는 저장용 오류 메시지
	 */
	private String buildResourceAccessErrorMessage(ResourceAccessException e) {
		
		Throwable cause = e.getCause();
		
		while (cause != null) {
			if (cause instanceof SocketTimeoutException) {
				return "외부 API 응답 시간 초과";
			}
			
			if (cause instanceof ConnectException) {
				return "외부 API 연결 실패";
			}
			
			cause = cause.getCause();
		}
		
		return "외부 API 통신 오류";
	}
	
	/**
	 * 일반 오류의 저장용 메시지를 생성한다.
	 *
	 * @param e	External API 호출 중 발생한 일반 예외
	 * @return	예외 유형과 메시지를 포함한 저장용 오류 메시지
	 */
	private String buildErrorMessage(Exception e) {
		
		String message = e.getMessage();
		
		if (message == null || message.isBlank()) {
			return e.getClass().getSimpleName();
		}
		
		return e.getClass().getSimpleName() + " - " + message;
	}
	
	/**
	 * 필수 External API 파라미터의 값을 검증한다.
	 *
	 * @param param				External API 파라미터 정보
	 * @param resolvedValue		실제 실행 시 사용할 파라미터 값
	 */
	private void validateRequiredParam(ExternalApiParamDto param, String resolvedValue) {
		
		// 1. 선택 파라미터는 검증하지 않는다.
		if (!"Y".equals(param.getRequiredYn())) {
			return;
		}
		
		// 2. 필수 파라미터의 값이 없으면 예외를 발생시킨다.
		if (resolvedValue == null || resolvedValue.isBlank()) {
			throw new IllegalArgumentException("필수 External API 파라미터 값이 없습니다. " + "paramName: " + param.getParamName());
		}
	}
	
	/**
	 * External API를 호출한다.
	 *
	 * @param externalApi	호출할 External API 정보
	 * @param headers		요청에 적용할 HTTP Header 정보
	 * @param queryParams	요청 URL에 적용할 Query Parameter 정보
	 * @param bodyParams	요청 Body에 적용할 Parameter 정보
	 * @return				External API HTTP 응답 정보
	 */
	private ResponseEntity<String> callExternalApi(
			ExternalApiDto externalApi,
			Map<String, String> headers,
			Map<String, String> queryParams,
			Map<String, String> bodyParams) {
		
		// 1. Query Parameter를 포함한 호출 URL을 생성한다.
		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(externalApi.getApiUrl());
		
		for (Map.Entry<String, String> entry : queryParams.entrySet()) {
			uriBuilder.queryParam(entry.getKey(), entry.getValue());
		}
		
		URI uri = uriBuilder.build(true).toUri();
		
		// 2. HTTP Header를 생성한다.
		HttpHeaders httpHeaders = new HttpHeaders();
		
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			httpHeaders.set(entry.getKey(), entry.getValue());
		}
		
		// 3. Body가 존재하면 JSON Content-Type을 기본값으로 설정한다.
		if (!bodyParams.isEmpty() && httpHeaders.getContentType() == null) {
			httpHeaders.setContentType(MediaType.APPLICATION_JSON);
		}
		
		// 4. HTTP 요청 객체를 생성한다.
		HttpEntity<?> requestEntity;
		
		if (bodyParams.isEmpty()) {
			requestEntity = new HttpEntity<>(httpHeaders);
			
		} else {
			requestEntity = new HttpEntity<>(bodyParams, httpHeaders);
		}
		
		// 5. 등록된 HTTP Method를 변환한다.
		HttpMethod httpMethod;
		
		try {
			httpMethod = HttpMethod.valueOf(externalApi.getHttpMethod());
			
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(
					"지원하지 않는 External API HTTP Method입니다. "
							+ "externalApiId: " + externalApi.getExternalApiId()
							+ ", httpMethod: " + externalApi.getHttpMethod(),
					e
			);
		}
		
		// 6. External API를 호출한다.
		return restTemplate.exchange(
				uri,
				httpMethod,
				requestEntity,
				String.class
		);
	}
	
}