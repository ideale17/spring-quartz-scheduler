
package com.kji.scheduler.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kji.scheduler.dto.ExternalApiDto;
import com.kji.scheduler.dto.ExternalApiRequestLogDto;
import com.kji.scheduler.dto.ExternalApiRequestParamLogDto;
import com.kji.scheduler.mapper.ExternalApiRequestLogMapper;

import lombok.RequiredArgsConstructor;

/**
 * External API 실행 당시 실제 HTTP 요청 및 파라미터 이력을 저장한다.
 */
@Service
@RequiredArgsConstructor
public class ExternalApiRequestLogService {
	
	private static final String MASKED_VALUE = "******";
	
	private static final Pattern SENSITIVE_PARAM_NAME = Pattern.compile(
			"(?i).*(api[_-]?key|service[_-]?key|access[_-]?key|"
					+ "token|secret|password|passwd|authorization|"
					+ "credential|signature).*"
	);
	
	private final ExternalApiRequestLogMapper mapper;
	
	/**
	 * 논리적 HTTP 요청과 실제 요청 파라미터를 저장한다.
	 *
	 * @param externalApi External API 설정 정보
	 * @param executionId External API 실행 식별자
	 * @param requestSequence 논리적 HTTP 요청 순번
	 * @param combinationNo 1부터 시작하는 파라미터 조합 순번. 과거 실행은 NULL
	 * @param pageNo 실제 요청 페이지 번호
	 * @param originalRequestId 재처리 대상 원본 요청 ID, 일반 실행 시 null
	 * @param queryParams Query 파라미터
	 * @param bodyParams Body 파라미터
	 * @return 생성된 요청 식별자
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Long saveRequest(
			ExternalApiDto externalApi,
			String executionId,
			int requestSequence,
			int combinationNo,
			Integer pageNo,
			Long originalRequestId,
			Map<String, String> queryParams,
			Map<String, String> bodyParams) {
		
		// 1. 논리적 HTTP 요청 정보를 생성한다.
		ExternalApiRequestLogDto request = new ExternalApiRequestLogDto();
		request.setExecutionId(executionId);
		request.setRequestSequence(requestSequence);
		request.setCombinationNo(combinationNo);
		request.setPageNo(pageNo);
		request.setOriginalRequestId(originalRequestId);
		request.setRequestMethod(externalApi.getHttpMethod());
		request.setRequestUrl(buildSafeRequestUrl(externalApi));
		
		// 2. 요청 정보를 저장한다.
		int insertedCount = mapper.insertRequest(request);
		
		// 3. 생성된 요청 식별자를 확인한다.
		if (insertedCount != 1 || request.getRequestId() == null) {
			throw new IllegalStateException("External API 요청 이력 저장에 실패했습니다.");
		}
		
		// 4. 실행 당시 Query 파라미터를 저장한다.
		saveRequestParams(
				request.getRequestId(),
				externalApi,
				"QUERY",
				queryParams
		);
		
		// 5. 실행 당시 Body 파라미터를 저장한다.
		saveRequestParams(
				request.getRequestId(),
				externalApi,
				"BODY",
				bodyParams
		);
		
		// 6. 생성된 논리적 요청 식별자를 반환한다.
		return request.getRequestId();
	}
	
	/**
	 * 실행 당시 실제 요청 파라미터를 행 단위로 저장한다.
	 *
	 * @param requestId 논리적 요청 식별자
	 * @param externalApi External API 인증 설정 정보
	 * @param paramLocation 파라미터 전달 위치
	 * @param params 실제 요청 파라미터
	 */
	private void saveRequestParams(
			Long requestId,
			ExternalApiDto externalApi,
			String paramLocation,
			Map<String, String> params) {
		
		// 1. 요청 파라미터가 없으면 저장하지 않는다.
		if (params == null || params.isEmpty()) {
			return;
		}
		
		// 2. 실행 당시 파라미터를 순회한다.
		for (Map.Entry<String, String> entry : params.entrySet()) {
			
			// 3. 민감정보 여부를 확인하고 저장할 값을 결정한다.
			String paramValue = isSensitiveParam(externalApi, entry.getKey(), entry.getValue())
					? MASKED_VALUE
					: entry.getValue();
			
			// 4. 파라미터 이력 DTO를 생성한다.
			ExternalApiRequestParamLogDto param = new ExternalApiRequestParamLogDto();
			param.setRequestId(requestId);
			param.setParamLocation(paramLocation);
			param.setParamName(entry.getKey());
			param.setParamValue(paramValue);
			
			// 5. 파라미터 이력을 저장한다.
			if (mapper.insertRequestParam(param) != 1) {
				throw new IllegalStateException("External API 요청 파라미터 이력 저장에 실패했습니다.");
			}
		}
	}
	
	/**
	 * 요청 파라미터의 민감정보 여부를 확인한다.
	 *
	 * @param externalApi External API 인증 설정 정보
	 * @param paramName 파라미터 이름
	 * @param paramValue 파라미터 값
	 * @return 민감정보에 해당하면 true
	 */
	private boolean isSensitiveParam(
			ExternalApiDto externalApi,
			String paramName,
			String paramValue) {
		
		// 1. 알려진 민감정보 파라미터 이름을 확인한다.
		if (paramName != null && SENSITIVE_PARAM_NAME.matcher(paramName).matches()) {
			return true;
		}
		
		// 2. 등록된 API Key의 파라미터 이름을 확인한다.
		if ("API_KEY".equals(externalApi.getAuthType())
				&& paramName != null
				&& paramName.equalsIgnoreCase(externalApi.getAuthKey())) {
			return true;
		}
		
		// 3. 등록된 인증정보 값이 포함되어 있는지 확인한다.
		return containsSecret(paramValue, externalApi.getAuthValue())
				|| containsSecret(paramValue, externalApi.getAuthPassword());
	}
	
	/**
	 * 지정한 문자열에 등록된 인증정보가 포함되어 있는지 확인한다.
	 *
	 * @param value 검사할 문자열
	 * @param secret 등록된 인증정보
	 * @return 인증정보가 포함되어 있으면 true
	 */
	private boolean containsSecret(String value, String secret) {
		return value != null && secret != null && !secret.isBlank() && value.contains(secret);
	}
	
	/**
	 * 민감정보를 제외한 요청 URL을 생성한다.
	 *
	 * @param externalApi External API 설정 정보
	 * @return 민감정보를 제외한 요청 URL 또는 null
	 */
	private String buildSafeRequestUrl(ExternalApiDto externalApi) {
		
		// 1. API URL을 파싱한다.
		URI source = URI.create(externalApi.getApiUrl());
		
		// 2. 사용자 정보, Query 및 Fragment를 제외한 URL을 생성한다.
		try {
			URI safeUri = new URI(
					source.getScheme(),
					null,
					source.getHost(),
					source.getPort(),
					source.getPath(),
					null,
					null
			);
			
			String safeUrl = safeUri.toString();
			
			// 3. URL 경로에 알려진 인증정보가 포함되어 있으면 저장하지 않는다.
			if (containsSecret(safeUrl, externalApi.getAuthValue())
					|| containsSecret(safeUrl, externalApi.getAuthPassword())) {
				return null;
			}
			
			// 4. DB 컬럼 길이를 초과하면 저장하지 않는다.
			if (safeUrl.length() > 2000) {
				return null;
			}
			
			return safeUrl;
			
		} catch (URISyntaxException e) {
			throw new IllegalStateException("External API 요청 이력 URL 생성에 실패했습니다.", e);
		}
	}
	
}
