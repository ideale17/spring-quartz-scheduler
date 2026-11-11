
package com.kji.scheduler.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kji.scheduler.dto.ExternalApiDto;
import com.kji.scheduler.dto.ExternalApiExecutionSnapshotDto;
import com.kji.scheduler.dto.ExternalApiPagingDto;
import com.kji.scheduler.dto.ExternalApiParamDto;
import com.kji.scheduler.dto.ExternalApiParamRepeatValueDto;
import com.kji.scheduler.mapper.ExternalApiExecutionSnapshotMapper;

/**
 * External API 최초 실행 당시 설정 스냅샷을 저장한다.
 */
@Service
public class ExternalApiExecutionSnapshotService {
	
	private static final int SNAPSHOT_VERSION = 1;
	
	private static final Pattern SENSITIVE_PARAM_NAME = Pattern.compile(
			"(?i).*(api[_-]?key|service[_-]?key|access[_-]?key|"
					+ "token|secret|password|passwd|authorization|"
					+ "credential|signature).*"
	);
	
	private final ExternalApiExecutionSnapshotMapper mapper;
	private final ObjectMapper objectMapper;
	
	public ExternalApiExecutionSnapshotService(
			ExternalApiExecutionSnapshotMapper mapper,
			ObjectMapper objectMapper) {
		this.mapper = mapper;
		this.objectMapper = objectMapper;
	}
	
	/**
	 * 최초 실행 당시의 API 설정을 스냅샷으로 저장한다.
	 *
	 * @param executionId 실행 식별자
	 * @param executionTime 실행 기준 시각
	 * @param externalApi API 설정
	 * @param params 파라미터 설정 목록
	 * @param paging 페이징 설정
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void saveSnapshot(
			String executionId,
			LocalDateTime executionTime,
			ExternalApiDto externalApi,
			List<ExternalApiParamDto> params,
			ExternalApiPagingDto paging) {
		
		// 1. 스냅샷 저장에 필요한 정보를 검증한다.
		if (executionId == null || executionId.isBlank()) {
			throw new IllegalArgumentException("External API 실행 식별자가 없습니다.");
		}
		
		if (executionTime == null || externalApi == null || params == null) {
			throw new IllegalArgumentException("External API 실행 설정이 올바르지 않습니다.");
		}
		
		// 2. 실행 당시 설정에서 저장 가능한 항목만 구성한다.
		Map<String, Object> settings = new LinkedHashMap<>();
		settings.put("api", buildApiSettings(externalApi));
		settings.put("params", buildParamSettings(externalApi, params));
		settings.put("paging", paging == null ? null : buildPagingSettings(paging));
		
		// 3. 실행 설정을 JSON 문자열로 변환한다.
		String snapshotData;
		
		try {
			snapshotData = objectMapper.writeValueAsString(settings);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("External API 설정 스냅샷 직렬화에 실패했습니다.", e);
		}
		
		// 4. 스냅샷 저장 DTO를 생성한다.
		ExternalApiExecutionSnapshotDto snapshot = new ExternalApiExecutionSnapshotDto();
		snapshot.setExecutionId(executionId);
		snapshot.setSnapshotVersion(SNAPSHOT_VERSION);
		snapshot.setExecutionBaseTime(executionTime);
		snapshot.setSnapshotData(snapshotData);
		
		// 5. 최초 실행 설정을 저장한다.
		if (mapper.insertSnapshot(snapshot) != 1) {
			throw new IllegalStateException("External API 설정 스냅샷 저장에 실패했습니다.");
		}
		
	}
	
	/**
	 * 실제 인증 비밀값을 제외한 API 설정을 생성한다.
	 *
	 * @param externalApi API 설정
	 * @return 저장 가능한 API 설정
	 */
	private Map<String, Object> buildApiSettings(ExternalApiDto externalApi) {
		
		// 1. API 실행에 필요한 기본 정보를 구성한다.
		Map<String, Object> api = new LinkedHashMap<>();
		
		api.put("externalApiId", externalApi.getExternalApiId());
		api.put("apiUrl", externalApi.getApiUrl());
		api.put("httpMethod", externalApi.getHttpMethod());
		api.put("retryEnabled", externalApi.getRetryEnabled());
		api.put("maxRetryCount", externalApi.getMaxRetryCount());
		api.put("retryIntervalSec", externalApi.getRetryIntervalSec());
		
		// 2. 인증 방식과 위치 정보만 보존한다.
		api.put("authType", externalApi.getAuthType());
		api.put("authLocation", externalApi.getAuthLocation());
		api.put("authKey", externalApi.getAuthKey());
		
		return api;
	}
	
	/**
	 * 민감정보를 제외한 파라미터 설정 목록을 생성한다.
	 *
	 * @param externalApi API 인증 설정
	 * @param params 파라미터 설정 목록
	 * @return 스냅샷 파라미터 목록
	 */
	private List<Map<String, Object>> buildParamSettings(
			ExternalApiDto externalApi,
			List<ExternalApiParamDto> params) {
		
		List<Map<String, Object>> result = new ArrayList<>();
		
		// 1. 등록된 파라미터를 순서대로 처리한다.
		for (ExternalApiParamDto param : params) {
			
			Map<String, Object> item = new LinkedHashMap<>();
			
			item.put("paramId", param.getParamId());
			item.put("paramLocation", param.getParamLocation());
			item.put("paramName", param.getParamName());
			item.put("valueType", param.getValueType());
			item.put("valueFormat", param.getValueFormat());
			item.put("offsetUnit", param.getOffsetUnit());
			item.put("offsetValue", param.getOffsetValue());
			item.put("repeatType", param.getRepeatType());
			item.put("repeatUnit", param.getRepeatUnit());
			item.put("repeatStart", param.getRepeatStart());
			item.put("repeatEnd", param.getRepeatEnd());
			item.put("repeatStep", param.getRepeatStep());
			item.put("requiredYn", param.getRequiredYn());
			item.put("sortOrder", param.getSortOrder());
			
			// 2. 비밀값으로 판단되면 원본 값 대신 현재 설정 참조를 기록한다.
			if (isSensitive(externalApi, param.getParamName(), param.getParamValue())) {
				item.put("paramValue", null);
				item.put("valueSource", "CURRENT_CONFIG");
			} else {
				item.put("paramValue", param.getParamValue());
			}
			
			// 3. LIST 반복 값에도 동일한 민감정보 정책을 적용한다.
			List<Map<String, Object>> repeatValues = new ArrayList<>();
			
			if (param.getRepeatValues() != null) {
				for (ExternalApiParamRepeatValueDto repeat : param.getRepeatValues()) {
					
					Map<String, Object> repeatItem = new LinkedHashMap<>();
					repeatItem.put("repeatValueId", repeat.getRepeatValueId());
					repeatItem.put("sortOrder", repeat.getSortOrder());
					
					if (isSensitive(externalApi, param.getParamName(), repeat.getParamValue())) {
						repeatItem.put("paramValue", null);
						repeatItem.put("valueSource", "CURRENT_CONFIG");
					} else {
						repeatItem.put("paramValue", repeat.getParamValue());
					}
					
					repeatValues.add(repeatItem);
				}
			}
			
			item.put("repeatValues", repeatValues);
			result.add(item);
		}
		
		return result;
	}
	
	/**
	 * 페이징 실행에 필요한 설정을 생성한다.
	 *
	 * @param paging 페이징 설정
	 * @return 저장 가능한 페이징 설정
	 */
	private Map<String, Object> buildPagingSettings(ExternalApiPagingDto paging) {
		
		Map<String, Object> result = new LinkedHashMap<>();
		
		result.put("enabled", paging.getEnabled());
		result.put("paginationType", paging.getPaginationType());
		result.put("terminationType", paging.getTerminationType());
		result.put("pageParamLocation", paging.getPageParamLocation());
		result.put("pageParamName", paging.getPageParamName());
		result.put("pageStart", paging.getPageStart());
		result.put("sizeParamLocation", paging.getSizeParamLocation());
		result.put("sizeParamName", paging.getSizeParamName());
		result.put("pageSize", paging.getPageSize());
		result.put("totalCountPath", paging.getTotalCountPath());
		result.put("maxRequestCount", paging.getMaxRequestCount());
		
		return result;
	}
	
	/**
	 * 인증정보에 해당하는 파라미터인지 확인한다.
	 *
	 * @param externalApi API 인증 설정
	 * @param paramName 파라미터 이름
	 * @param paramValue 파라미터 값
	 * @return 민감정보 여부
	 */
	private boolean isSensitive(
			ExternalApiDto externalApi,
			String paramName,
			String paramValue) {
		
		// 1. 알려진 인증 파라미터 이름인지 확인한다.
		if (paramName != null && SENSITIVE_PARAM_NAME.matcher(paramName).matches()) {
			return true;
		}
		
		// 2. 등록된 API Key 이름과 같은지 확인한다.
		if ("API_KEY".equals(externalApi.getAuthType())
				&& paramName != null
				&& paramName.equalsIgnoreCase(externalApi.getAuthKey())) {
			return true;
		}
		
		// 3. 기존 인증 비밀값이 포함되어 있는지 확인한다.
		return containsSecret(paramValue, externalApi.getAuthValue())
				|| containsSecret(paramValue, externalApi.getAuthPassword());
	}
	
	/**
	 * 문자열에 인증 비밀값이 포함되어 있는지 확인한다.
	 *
	 * @param value 검사할 문자열
	 * @param secret 인증 비밀값
	 * @return 비밀값 포함 여부
	 */
	private boolean containsSecret(String value, String secret) {
		return value != null && secret != null && !secret.isBlank() && value.contains(secret);
	}
	
}