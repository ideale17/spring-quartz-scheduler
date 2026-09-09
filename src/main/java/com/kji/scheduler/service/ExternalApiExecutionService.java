package com.kji.scheduler.service;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.kji.scheduler.dto.ExternalApiDto;
import com.kji.scheduler.dto.ExternalApiParamDto;

@Service
public class ExternalApiExecutionService {
	
	private static final Logger log = LoggerFactory.getLogger(ExternalApiExecutionService.class);
	
	private final RestTemplate restTemplate;
	private final ExternalApiService externalApiService;
	private final DynamicParameterResolver dynamicParameterResolver;
	
	
	public ExternalApiExecutionService(ExternalApiService externalApiService,
			DynamicParameterResolver dynamicParameterResolver,
			RestTemplate restTemplate) {
		this.externalApiService = externalApiService;
		this.dynamicParameterResolver = dynamicParameterResolver;
		this.restTemplate = restTemplate;
	}
	
	// External API 실행 정보를 생성한다.
	public void execute(Long externalApiId) {
		
		// 1. Job 실행 기준 시간을 한 번만 생성한다.
		LocalDateTime executionTime = LocalDateTime.now();
		
		// 2. External API 기본 정보를 조회한다.
		ExternalApiDto externalApi = externalApiService.getExternalApi(externalApiId);
		
		// 3. External API 사용 가능 여부를 확인한다.
		if (!"Y".equals(externalApi.getEnabled())) {
			throw new IllegalStateException("사용 중지된 External API입니다. externalApiId: " + externalApiId);
		}
		
		// 4. External API 파라미터 목록을 조회한다.
		List<ExternalApiParamDto> params = externalApiService.getExternalApiParamList(externalApiId);
		
		// 5. 요청 위치별 파라미터를 저장할 Map을 생성한다.
		Map<String, String> headers = new LinkedHashMap<>();
		Map<String, String> queryParams = new LinkedHashMap<>();
		Map<String, String> bodyParams = new LinkedHashMap<>();
		
		// 6. 각 파라미터의 실제 실행 값을 생성한다.
		for (ExternalApiParamDto param : params) {
			
			String resolvedValue = dynamicParameterResolver.resolve(param, executionTime);
			
			// 7. 필수 파라미터의 값 존재 여부를 확인한다.
			validateRequiredParam(param, resolvedValue);
			
			// 8. 값이 없는 선택 파라미터는 요청에서 제외한다.
			if (resolvedValue == null) {
				continue;
			}
			
			// 9. 파라미터 전달 위치에 따라 요청 정보를 분리한다.
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
		
		// 10. External API 요청 정보 생성 결과를 기록한다.
		log.info(
				"External API 요청 정보 생성 완료. "
						+ "externalApiId: {}, apiName: {}, method: {}, "
						+ "headerCount: {}, queryCount: {}, bodyCount: {}",
				externalApiId,
				externalApi.getApiName(),
				externalApi.getHttpMethod(),
				headers.size(),
				queryParams.size(),
				bodyParams.size()
		);
		
		// 11. External API를 호출한다.
		ResponseEntity<String> response = callExternalApi(externalApi, headers, queryParams, bodyParams);

		// 12. External API 호출 결과를 기록한다.
		log.info(
				"External API 호출 완료. " + "externalApiId: {}, apiName: {}, statusCode: {}",
				externalApiId,
				externalApi.getApiName(),
				response.getStatusCode()
		);
		
	}
	
	// 필수 External API 파라미터의 값을 검증한다.
	private void validateRequiredParam(ExternalApiParamDto param, String resolvedValue) {
		
		// 1. 선택 파라미터는 검증하지 않는다.
		if (!"Y".equals(param.getRequiredYn())) {
			return;
		}
		
		// 2. 필수 파라미터의 값이 없으면 예외를 발생시킨다.
		if (resolvedValue == null || resolvedValue.isBlank()) {
			throw new IllegalArgumentException(
					"필수 External API 파라미터 값이 없습니다. "
							+ "paramName: " + param.getParamName()
			);
		}
	}
	
	// External API를 호출한다.
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