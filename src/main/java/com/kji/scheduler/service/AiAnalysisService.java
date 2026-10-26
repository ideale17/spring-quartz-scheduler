package com.kji.scheduler.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kji.scheduler.dto.AiAnalysisDataDto;
import com.kji.scheduler.dto.AiAnalysisDatasetDto;
import com.kji.scheduler.dto.AiAnalysisFieldDto;
import com.kji.scheduler.dto.AiAnalysisPreviewDto;
import com.kji.scheduler.dto.AiAnalysisSearchDto;
import com.kji.scheduler.mapper.AiAnalysisMapper;

@Service
public class AiAnalysisService {
	
	private static final int MAX_RESULT_COUNT = 50;
	private static final int MAX_RESULT_LENGTH = 100000;
	private static final int MAX_TOTAL_LENGTH = 500000;
	private static final int MAX_RECORD_COUNT = 5000;
	private static final int MAX_PERIOD_DAYS = 31;
	
	private final AiAnalysisMapper mapper;
	private final ObjectMapper objectMapper;
	
	public AiAnalysisService(AiAnalysisMapper mapper, ObjectMapper objectMapper) {
		this.mapper = mapper;
		this.objectMapper = objectMapper;
	}
	
	/**
	 * AI 분석 대상 가공 결과를 미리보기 조회한다.
	 *
	 * @param searchDto AI 분석 대상 검색조건
	 * @return 분석 대상 가공 결과 및 조회 제한 정보
	 * @throws IllegalArgumentException 검색조건 또는 데이터 크기가 올바르지 않은 경우
	 */
	@Transactional(readOnly = true)
	public AiAnalysisPreviewDto getAnalysisPreview(AiAnalysisSearchDto searchDto) {
		
		// 1. 검색조건을 검증한다.
		if (searchDto == null) {
			throw new IllegalArgumentException("AI 분석 검색조건이 없습니다.");
		}
		
		if (searchDto.getExternalApiId() == null || searchDto.getExternalApiId() < 1) {
			throw new IllegalArgumentException("External API 식별자가 올바르지 않습니다.");
		}
		
		if (searchDto.getTransformRuleId() == null || searchDto.getTransformRuleId() < 1) {
			throw new IllegalArgumentException("가공 규칙 식별자가 올바르지 않습니다.");
		}
		
		if (searchDto.getVersionNo() == null || searchDto.getVersionNo() < 1) {
			throw new IllegalArgumentException("가공 규칙 버전이 올바르지 않습니다.");
		}
		
		if (searchDto.getStartDate() == null || searchDto.getEndDate() == null) {
			throw new IllegalArgumentException("분석 시작일과 종료일은 필수입니다.");
		}
		
		if (searchDto.getStartDate().isAfter(searchDto.getEndDate())) {
			throw new IllegalArgumentException("분석 시작일은 종료일보다 늦을 수 없습니다.");
		}
		
		long periodDays = ChronoUnit.DAYS.between(
				searchDto.getStartDate(), searchDto.getEndDate()) + 1;
		
		if (periodDays > MAX_PERIOD_DAYS) {
			throw new IllegalArgumentException("분석 기간은 최대 31일까지 지정할 수 있습니다.");
		}
		
		// 2. 검증된 날짜와 조회 제한을 설정한다.
		LocalDateTime startAt = searchDto.getStartDate().atStartOfDay();
		LocalDateTime endAt = searchDto.getEndDate().plusDays(1).atStartOfDay();
		
		searchDto.setStartAt(startAt);
		searchDto.setEndAt(endAt);
		searchDto.setLimit(MAX_RESULT_COUNT + 1);
		
		// 3. 분석 대상 가공 결과를 조회한다.
		List<AiAnalysisDataDto> results = mapper.findAnalysisData(searchDto);
		
		// 4. 최대 조회 건수 초과 여부를 확인한다.
		boolean hasMore = results.size() > MAX_RESULT_COUNT;
		
		if (hasMore) {
			results = results.subList(0, MAX_RESULT_COUNT);
		}
		
		// 5. 가공 결과 데이터 크기를 검증한다.
		long totalLength = 0;
		
		for (AiAnalysisDataDto result : results) {
			
			String resultData = result.getResultData();
			
			if (resultData == null) {
				throw new IllegalArgumentException(
						"분석 대상 가공 결과 데이터가 없습니다. ID: " + result.getTransformResultId());
			}
			
			if (resultData.length() > MAX_RESULT_LENGTH) {
				throw new IllegalArgumentException(
						"가공 결과 데이터 크기가 허용 범위를 초과했습니다. ID: " + result.getTransformResultId());
			}
			
			totalLength += resultData.length();
			
			if (totalLength > MAX_TOTAL_LENGTH) {
				throw new IllegalArgumentException("분석 대상 전체 데이터 크기가 허용 범위를 초과했습니다.");
			}
		}
		
		// 6. 분석 대상 데이터와 조회 제한 정보를 반환한다.
		AiAnalysisPreviewDto preview = new AiAnalysisPreviewDto();
		preview.setResultCount(results.size());
		preview.setHasMore(hasMore);
		preview.setResults(results);
		
		return preview;
	}
	
	/**
	 * 가공 결과를 공통 분석 데이터셋으로 변환하고 구성 정보를 조회한다.
	 *
	 * @param searchDto AI 분석 대상 검색조건
	 * @return 데이터셋의 레코드, 필드 및 중복 후보 정보
	 * @throws IllegalArgumentException 검색조건 또는 가공 데이터가 올바르지 않은 경우
	 */
	@Transactional(readOnly = true)
	public AiAnalysisDatasetDto getAnalysisDataset(AiAnalysisSearchDto searchDto) {
		
		// 1. 기존 미리보기 조회를 통해 검색조건과 데이터 크기를 검증한다.
		AiAnalysisPreviewDto preview = getAnalysisPreview(searchDto);
		
		// 2. 데이터셋 구성 정보를 초기화한다.
		Map<String, AiAnalysisFieldDto> fieldMap = new LinkedHashMap<>();
		Set<JsonNode> uniqueRecords = new HashSet<>();
		
		int recordCount = 0;
		int duplicateCandidateCount = 0;
		
		// 3. 가공 결과별 JSON 배열 또는 객체를 읽는다.
		for (AiAnalysisDataDto result : preview.getResults()) {
			
			JsonNode root;
			
			try {
				root = objectMapper.readTree(result.getResultData());
			} catch (JsonProcessingException e) {
				throw new IllegalArgumentException(
						"가공 결과 JSON 형식이 올바르지 않습니다. ID: "
						+ result.getTransformResultId(), e);
			}
			
			if (root == null || root.isNull()) {
				throw new IllegalArgumentException(
						"가공 결과 JSON이 비어 있습니다. ID: "
						+ result.getTransformResultId());
			}
			
			// 4. JSON 배열 또는 객체에 포함된 레코드를 즉시 집계한다.
			if (root.isArray()) {
				
				for (JsonNode record : root) {
					
					if (!record.isObject()) {
						throw new IllegalArgumentException(
								"가공 결과 배열에는 JSON 객체만 포함할 수 있습니다. ID: "
								+ result.getTransformResultId());
					}
					
					recordCount++;
					
					if (recordCount > MAX_RECORD_COUNT) {
						throw new IllegalArgumentException(
								"분석 데이터셋은 최대 5000개 레코드까지 처리할 수 있습니다.");
					}
					
					if (!uniqueRecords.add(record)) {
						duplicateCandidateCount++;
					}
					
					updateFieldStatistics(record, fieldMap);
				}
				
			} else if (root.isObject()) {
				
				recordCount++;
				
				if (recordCount > MAX_RECORD_COUNT) {
					throw new IllegalArgumentException(
							"분석 데이터셋은 최대 5000개 레코드까지 처리할 수 있습니다.");
				}
				
				if (!uniqueRecords.add(root)) {
					duplicateCandidateCount++;
				}
				
				updateFieldStatistics(root, fieldMap);
				
			} else {
				throw new IllegalArgumentException(
						"가공 결과는 JSON 객체 또는 배열이어야 합니다. ID: "
						+ result.getTransformResultId());
			}
		}
		
		// 5. 필드별 누락 건수를 계산한다.
		for (AiAnalysisFieldDto field : fieldMap.values()) {
			
			field.setMissingCount(recordCount - field.getPresentCount());
			
			if (field.getDataType() == null) {
				field.setDataType("NULL");
			}
		}
		
		// 6. 동일한 검색조건으로 전체 가공 결과 건수를 조회한다.
		long totalSourceCount = mapper.countAnalysisData(searchDto);
		
		// 7. 전체 건수와 실제 처리 건수를 구분하여 반환한다.
		AiAnalysisDatasetDto dataset = new AiAnalysisDatasetDto();
		dataset.setTotalSourceCount(totalSourceCount);
		dataset.setSourceCount(preview.getResultCount());
		dataset.setRecordCount(recordCount);
		dataset.setDuplicateCandidateCount(duplicateCandidateCount);
		dataset.setHasMore(totalSourceCount > preview.getResultCount());
		dataset.setFields(new ArrayList<>(fieldMap.values()));
		
		return dataset;
	}
	
	/**
	 * JSON 레코드의 필드별 통계 정보를 갱신한다.
	 *
	 * @param record 분석 대상 JSON 레코드
	 * @param fieldMap 필드별 통계 정보
	 */
	private void updateFieldStatistics(JsonNode record, Map<String, AiAnalysisFieldDto> fieldMap) {
		
		// 1. 레코드의 필드를 순회한다.
		for (Map.Entry<String, JsonNode> entry : record.properties()) {
			
			String fieldName = entry.getKey();
			JsonNode value = entry.getValue();
			
			// 2. 필드별 통계 정보를 조회하거나 생성한다.
			AiAnalysisFieldDto field = fieldMap.computeIfAbsent(fieldName, key -> {
				AiAnalysisFieldDto dto = new AiAnalysisFieldDto();
				dto.setFieldName(key);
				return dto;
			});
			
			field.setPresentCount(field.getPresentCount() + 1);
			
			// 3. NULL 여부를 확인한다.
			if (value.isNull()) {
				field.setNullCount(field.getNullCount() + 1);
				continue;
			}
			
			// 4. JSON 데이터 타입을 확인한다.
			String dataType = getJsonDataType(value);
			
			if (field.getDataType() == null) {
				field.setDataType(dataType);
			} else if (!field.getDataType().equals(dataType)) {
				field.setDataType("MIXED");
			}
		}
	}
	
	/**
	 * JSON 값의 데이터 타입을 반환한다.
	 *
	 * @param value JSON 값
	 * @return 데이터 타입
	 */
	private String getJsonDataType(JsonNode value) {
		
		if (value.isTextual()) {
			return "STRING";
		}
		
		if (value.isNumber()) {
			return "NUMBER";
		}
		
		if (value.isBoolean()) {
			return "BOOLEAN";
		}
		
		if (value.isArray()) {
			return "ARRAY";
		}
		
		if (value.isObject()) {
			return "OBJECT";
		}
		
		return "UNKNOWN";
	}
}