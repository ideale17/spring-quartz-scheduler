
package com.kji.scheduler.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kji.scheduler.dto.ExternalApiDto;
import com.kji.scheduler.dto.ExternalApiExecutionSnapshotDto;
import com.kji.scheduler.dto.ExternalApiPagingDto;
import com.kji.scheduler.dto.ExternalApiParamDto;
import com.kji.scheduler.dto.ExternalApiParamRepeatValueDto;
import com.kji.scheduler.dto.ExternalApiRestartConfigDto;
import com.kji.scheduler.mapper.ExternalApiExecutionSnapshotMapper;

/**
 * External API 최초 실행 당시 설정 스냅샷을 저장한다.
 */
@Service
public class ExternalApiExecutionSnapshotService {
	
	private static final int SNAPSHOT_VERSION = 1;
	
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
		settings.put("params", buildParamSettings(params));
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
	 * 최초 실행 당시 파라미터 설정 목록을 생성한다.
	 *
	 * @param params 파라미터 설정 목록
	 * @return 스냅샷 파라미터 목록
	 */
	private List<Map<String, Object>> buildParamSettings(List<ExternalApiParamDto> params) {
		
		List<Map<String, Object>> result = new ArrayList<>();
		
		// 1. 등록된 파라미터를 순서대로 처리한다.
		for (ExternalApiParamDto param : params) {
			
			Map<String, Object> item = new LinkedHashMap<>();
			
			item.put("paramId", param.getParamId());
			item.put("paramLocation", param.getParamLocation());
			item.put("paramName", param.getParamName());
			item.put("valueType", param.getValueType());
			item.put("paramValue", param.getParamValue());
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
			
			// 2. LIST 반복 값도 실행 당시 설정 그대로 저장한다.
			List<Map<String, Object>> repeatValues = new ArrayList<>();
			
			if (param.getRepeatValues() != null) {
				
				for (ExternalApiParamRepeatValueDto repeat : param.getRepeatValues()) {
					
					Map<String, Object> repeatItem = new LinkedHashMap<>();
					
					repeatItem.put("repeatValueId", repeat.getRepeatValueId());
					repeatItem.put("sortOrder", repeat.getSortOrder());
					repeatItem.put("paramValue", repeat.getParamValue());
					
					repeatValues.add(repeatItem);
				}
			}
			
			// 3. 반복 값과 파라미터 설정을 결과 목록에 추가한다.
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
	 * 최초 실행 당시 저장한 설정 스냅샷을 조회한다.
	 *
	 * @param executionId 최초 실행 식별자
	 * @return 실행 설정 스냅샷
	 * @throws IllegalArgumentException 실행 식별자가 올바르지 않은 경우
	 * @throws IllegalStateException 스냅샷이 없거나 올바르지 않은 경우
	 */
	public ExternalApiExecutionSnapshotDto getSnapshot(String executionId) {
		
		// 1. 실행 식별자를 검증한다.
		if (executionId == null || executionId.isBlank()) {
			throw new IllegalArgumentException("External API 실행 식별자가 없습니다.");
		}
		
		// 2. 최초 실행의 설정 스냅샷을 조회한다.
		ExternalApiExecutionSnapshotDto snapshot = mapper.findSnapshotByExecutionId(executionId);
		
		if (snapshot == null) {
			throw new IllegalStateException("External API 실행 설정 스냅샷이 존재하지 않습니다. executionId: " + executionId);
		}
		
		// 3. 지원하는 스냅샷 버전인지 확인한다.
		if (!Integer.valueOf(SNAPSHOT_VERSION).equals(snapshot.getSnapshotVersion())) {
			throw new IllegalStateException("지원하지 않는 External API 설정 스냅샷 버전입니다.");
		}
		
		// 4. 실행 기준 시각과 스냅샷 데이터가 존재하는지 확인한다.
		if (snapshot.getExecutionBaseTime() == null
				|| snapshot.getSnapshotData() == null
				|| snapshot.getSnapshotData().isBlank()) {
			throw new IllegalStateException("External API 실행 설정 스냅샷이 올바르지 않습니다.");
		}
		
		return snapshot;
	}
	

	/**
	 * 최초 실행의 스냅샷을 복원하고 현재 인증정보를 적용한다.
	 *
	 * @param snapshot 최초 실행 스냅샷
	 * @param currentApi 현재 External API 설정
	 * @param currentParams 현재 파라미터 설정 목록
	 * @return RESTART 실행 설정
	 */
	public ExternalApiRestartConfigDto restoreRestartConfig(
			ExternalApiExecutionSnapshotDto snapshot,
			ExternalApiDto currentApi,
			List<ExternalApiParamDto> currentParams) {
		
		// 1. 스냅샷 버전과 실행 기준 시각을 검증한다.
		if (snapshot == null
				|| !Integer.valueOf(SNAPSHOT_VERSION).equals(snapshot.getSnapshotVersion())
				|| snapshot.getExecutionBaseTime() == null) {
			throw new IllegalStateException("재처리할 External API 스냅샷 정보가 올바르지 않습니다.");
		}
		
		try {
			
			// 2. 스냅샷 JSON을 읽는다.
			JsonNode root = objectMapper.readTree(snapshot.getSnapshotData());
			
			if (root == null || !root.isObject() || !root.path("api").isObject() || !root.path("params").isArray()) {
				throw new IllegalStateException("External API 스냅샷 JSON 구조가 올바르지 않습니다.");
			}
			
			// 3. API 실행 당시 기본 설정을 복원한다.
			ExternalApiDto externalApi = objectMapper.treeToValue(root.get("api"), ExternalApiDto.class);
			
			if (!Objects.equals(externalApi.getExternalApiId(), currentApi.getExternalApiId())) {
				throw new IllegalStateException("원본 스냅샷과 현재 External API가 일치하지 않습니다.");
			}
			
			// 4. 인증 방식과 전달 위치는 현재 설정을 사용한다.
			externalApi.setAuthType(currentApi.getAuthType());
			externalApi.setAuthLocation(currentApi.getAuthLocation());
			externalApi.setAuthKey(currentApi.getAuthKey());

			// 5. 인증에 필요한 실제 값도 현재 설정을 사용한다.
			externalApi.setAuthValue(currentApi.getAuthValue());
			externalApi.setAuthUsername(currentApi.getAuthUsername());
			externalApi.setAuthPassword(currentApi.getAuthPassword());

			// 6. API 사용 여부는 현재 설정을 따른다.
			externalApi.setEnabled(currentApi.getEnabled());
			
			// 7. 스냅샷의 파라미터 설정을 복원한다.
			List<ExternalApiParamDto> params = new ArrayList<>();
			
			for (JsonNode paramNode : root.get("params")) {
				
				// 8. 파라미터 JSON 구조를 검증한다.
				if (!paramNode.isObject()) {
					throw new IllegalStateException("스냅샷 파라미터 구조가 올바르지 않습니다.");
				}
				
				// 9. 스냅샷 JSON을 파라미터 DTO로 변환한다.
				ExternalApiParamDto param = objectMapper.treeToValue(paramNode, ExternalApiParamDto.class);
				
				params.add(param);
			}
			
			// 10. 원본 페이징 설정을 복원한다.
			ExternalApiPagingDto paging = null;
			
			if (root.hasNonNull("paging")) {
				paging = objectMapper.treeToValue(root.get("paging"), ExternalApiPagingDto.class);
			}
			
			// 11. RESTART 설정을 반환한다.
			ExternalApiRestartConfigDto config = new ExternalApiRestartConfigDto();
			config.setExternalApi(externalApi);
			config.setParams(params);
			config.setPaging(paging);
			config.setExecutionBaseTime(snapshot.getExecutionBaseTime());
			
			return config;
			
		} catch (JsonProcessingException | IllegalArgumentException e) {
			throw new IllegalStateException("External API 스냅샷 설정 복원에 실패했습니다.", e);
		}
	}
	
}