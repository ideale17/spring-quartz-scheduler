package com.kji.scheduler.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kji.scheduler.dto.AiAnalysisDataDto;
import com.kji.scheduler.dto.AiAnalysisDatasetDto;
import com.kji.scheduler.dto.AiAnalysisFieldDto;
import com.kji.scheduler.dto.AiAnalysisPreviewDto;
import com.kji.scheduler.dto.AiAnalysisRequestDto;
import com.kji.scheduler.dto.AiAnalysisResponseDto;
import com.kji.scheduler.dto.AiAnalysisSearchDto;
import com.kji.scheduler.dto.AiFieldStatisticsDto;
import com.kji.scheduler.mapper.AiAnalysisMapper;

@Service
public class AiAnalysisService {
	
	private static final int MAX_RESULT_COUNT = 50;
	private static final int MAX_RESULT_LENGTH = 100000;
	private static final int MAX_TOTAL_LENGTH = 500000;
	private static final int MAX_RECORD_COUNT = 5000;
	private static final int MAX_PERIOD_DAYS = 31;
	private static final int MAX_AI_DATA_LENGTH = 100000;
	
	private final AiAnalysisMapper mapper;
	private final ObjectMapper objectMapper;
	private final OpenAiAnalysisClient openAiAnalysisClient;
	
	public AiAnalysisService(AiAnalysisMapper mapper, ObjectMapper objectMapper,
			OpenAiAnalysisClient openAiAnalysisClient) {
		
		this.mapper = mapper;
		this.objectMapper = objectMapper;
		this.openAiAnalysisClient = openAiAnalysisClient;
	}
	
	/**
	 * AI 분석 대상 가공 결과를 미리보기 조회한다.
	 *
	 * @param searchDto AI 분석 대상 검색조건
	 * @return 분석 대상 가공 결과 및 조회 제한 정보
	 * @throws IllegalArgumentException 검색조건 또는 데이터 크기가 올바르지 않은 경우
	 */
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
		
		long periodDays = ChronoUnit.DAYS.between(searchDto.getStartDate(), searchDto.getEndDate()) + 1;
		
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
	 * AI 분석 실행 요청을 검증하고 AI 분석 결과를 반환한다.
	 *
	 * @param request AI 분석 실행 요청
	 * @return AI 분석 결과
	 * @throws IllegalArgumentException 분석 요청 또는 데이터셋이 올바르지 않은 경우
	 * @throws IllegalStateException AI 분석 실행에 실패한 경우
	 */
	public AiAnalysisResponseDto executeAnalysis(AiAnalysisRequestDto request) {
		
		// 1. 분석 요청 정보를 검증한다.
		if (request.getPrompt() == null || request.getPrompt().isBlank()) {
			throw new IllegalArgumentException("AI 분석 질문을 입력해주세요.");
		}
		
		if (request.getPrompt().length() > 2000) {
			throw new IllegalArgumentException("AI 분석 질문은 최대 2000자까지 입력할 수 있습니다.");
		}
		
		// 2. AI API 연결 설정을 확인한다.
		openAiAnalysisClient.validateConfiguration();
		
		// 3. 요청 조건을 기존 검색 DTO로 변환한다.
		AiAnalysisSearchDto searchDto = new AiAnalysisSearchDto();
		searchDto.setExternalApiId(request.getExternalApiId());
		searchDto.setTransformRuleId(request.getTransformRuleId());
		searchDto.setVersionNo(request.getVersionNo());
		searchDto.setStartDate(request.getStartDate());
		searchDto.setEndDate(request.getEndDate());
		
		// 4. 기존 데이터셋 조회 기능으로 분석 가능 여부를 확인한다.
		AiAnalysisDatasetDto dataset = getAnalysisDataset(searchDto);
		
		if (dataset.getTotalSourceCount() == 0 || dataset.getRecordCount() == 0) {
			throw new IllegalArgumentException("분석할 데이터가 없습니다.");
		}
		
		if (dataset.isHasMore()) {
			throw new IllegalArgumentException(
					"분석 대상 데이터가 최대 조회 건수를 초과했습니다. 분석 기간을 줄여주세요.");
		}
		
		// 5. AI가 요청한 도구에 따라 데이터 조회 또는 통계 계산을 수행한다.
		String answer = openAiAnalysisClient.analyze(
				request.getPrompt(),
				dataset,
				() -> getAnalysisData(searchDto),
				fieldName -> calculateFieldStatistics(searchDto, fieldName)
		);
		
		// 6. 분석 결과를 반환한다.
		AiAnalysisResponseDto response = new AiAnalysisResponseDto();
		response.setAnswer(answer);
		response.setModel(openAiAnalysisClient.getModel());
		response.setTotalSourceCount(dataset.getTotalSourceCount());
		response.setSourceCount(dataset.getSourceCount());
		response.setRecordCount(dataset.getRecordCount());
		
		return response;
	}
	
	/**
	 * AI 분석에 필요한 실제 가공 데이터를 조회한다.
	 *
	 * @param searchDto AI 분석 대상 검색조건
	 * @return AI에 전달할 JSON 데이터
	 * @throws IllegalArgumentException 조회 범위나 전송 데이터 크기가 올바르지 않은 경우
	 */
	public String getAnalysisData(AiAnalysisSearchDto searchDto) {
		
		// 1. 기존 검증 및 조회 로직을 재사용한다.
		AiAnalysisPreviewDto preview = getAnalysisPreview(searchDto);
		
		// 2. 전체 가공 결과 건수를 확인한다.
		long totalSourceCount = mapper.countAnalysisData(searchDto);
		
		if (totalSourceCount > preview.getResultCount()) {
			throw new IllegalArgumentException(
					"전체 데이터를 조회할 수 없습니다. 분석 기간을 줄여주세요.");
		}
		
		if (totalSourceCount == 0) {
			throw new IllegalArgumentException("분석할 데이터가 없습니다.");
		}
		
		// 3. AI에 전달할 데이터 구조를 생성한다.
		ObjectNode data = objectMapper.createObjectNode();
		
		data.put("externalApiId", searchDto.getExternalApiId());
		data.put("transformRuleId", searchDto.getTransformRuleId());
		data.put("versionNo", searchDto.getVersionNo());
		data.put("startDate", searchDto.getStartDate().toString());
		data.put("endDate", searchDto.getEndDate().toString());
		data.put("sourceCount", preview.getResultCount());
		data.put("complete", true);
		
		ArrayNode results = data.putArray("results");
		
		// 4. 실제 가공 결과 JSON을 데이터에 포함한다.
		for (AiAnalysisDataDto result : preview.getResults()) {
			
			JsonNode resultJson;
			
			try {
				resultJson = objectMapper.readTree(result.getResultData());
			} catch (JsonProcessingException e) {
				throw new IllegalArgumentException(
						"가공 결과 JSON 형식이 올바르지 않습니다. ID: "
						+ result.getTransformResultId(), e);
			}
			
			if (resultJson == null || (!resultJson.isObject() && !resultJson.isArray())) {
				throw new IllegalArgumentException(
						"가공 결과는 JSON 객체 또는 배열이어야 합니다. ID: "
						+ result.getTransformResultId());
			}
			
			ObjectNode item = results.addObject();
			item.put("transformResultId", result.getTransformResultId());
			item.put("createdAt", String.valueOf(result.getCreatedAt()));
			item.set("data", resultJson);
			
			// 5. AI 전송용 JSON 크기를 제한한다.
			if (data.toString().length() > MAX_AI_DATA_LENGTH) {
				throw new IllegalArgumentException(
						"AI에 전달할 데이터 크기가 제한을 초과했습니다. 분석 기간을 줄여주세요.");
			}
		}
		
		// 6. 검증된 JSON 데이터를 반환한다.
		return data.toString();
	}
	
	/**
	 * 선택한 숫자 필드의 통계 정보를 계산한다.
	 *
	 * @param searchDto AI 분석 대상 검색조건
	 * @param fieldName 분석할 숫자 필드명
	 * @return 숫자 필드 통계 결과
	 * @throws IllegalArgumentException 검색조건이나 숫자 필드가 올바르지 않은 경우
	 */
	public AiFieldStatisticsDto calculateFieldStatistics(AiAnalysisSearchDto searchDto, String fieldName) {
		
		// 1. 분석할 필드명을 검증한다.
		if (fieldName == null || fieldName.isBlank()) {
			throw new IllegalArgumentException("통계를 계산할 필드명이 없습니다.");
		}
		
		if (fieldName.length() > 100) {
			throw new IllegalArgumentException("통계 필드명은 최대 100자까지 지정할 수 있습니다.");
		}
		
		// 2. 기존 검색조건 검증 및 가공 결과 조회 기능을 재사용한다.
		AiAnalysisPreviewDto preview = getAnalysisPreview(searchDto);
		long totalSourceCount = mapper.countAnalysisData(searchDto);

		if (totalSourceCount == 0) {
			throw new IllegalArgumentException("분석할 데이터가 없습니다.");
		}

		if (preview.isHasMore() || totalSourceCount > preview.getResultCount()) {
			throw new IllegalArgumentException("전체 데이터를 조회할 수 없습니다. 분석 기간을 줄여주세요.");
		}

		// 3. 통계 계산 변수를 초기화한다.
		BigDecimal sum = BigDecimal.ZERO;
		BigDecimal min = null;
		BigDecimal max = null;

		int totalRecordCount = 0;
		int validCount = 0;
		int nullCount = 0;
		int missingCount = 0;
		int invalidCount = 0;
		int presentCount = 0;

		// 4. 가공 결과에 포함된 JSON 레코드를 순회한다.
		for (AiAnalysisDataDto result : preview.getResults()) {

			JsonNode root;

			try {
				root = objectMapper.readTree(result.getResultData());
			} catch (JsonProcessingException e) {
				throw new IllegalArgumentException(
						"가공 결과 JSON 형식이 올바르지 않습니다. ID: "
						+ result.getTransformResultId(), e);
			}

			if (root == null || (!root.isObject() && !root.isArray())) {
				throw new IllegalArgumentException(
						"가공 결과는 JSON 객체 또는 배열이어야 합니다. ID: "
						+ result.getTransformResultId());
			}

			// 5. JSON 객체와 배열을 동일한 방식으로 처리한다.
			List<JsonNode> records = new ArrayList<>();

			if (root.isArray()) {
				root.forEach(records::add);
			} else {
				records.add(root);
			}

			for (JsonNode record : records) {

				if (!record.isObject()) {
					throw new IllegalArgumentException(
							"분석 대상 레코드는 JSON 객체여야 합니다. ID: "
							+ result.getTransformResultId());
				}

				totalRecordCount++;

				if (totalRecordCount > MAX_RECORD_COUNT) {
					throw new IllegalArgumentException(
							"분석 데이터셋은 최대 5000개 레코드까지 처리할 수 있습니다.");
				}

				// 6. 필드 존재 여부와 NULL을 구분한다.
				JsonNode value = record.get(fieldName);

				if (value == null) {
					missingCount++;
					continue;
				}

				presentCount++;

				if (value.isNull()) {
					nullCount++;
					continue;
				}

				// 7. 숫자 또는 쉼표가 포함된 숫자 문자열을 변환한다.
				if (!value.isNumber() && !value.isTextual()) {
					invalidCount++;
					continue;
				}

				String numericValue = value.asText().replace(",", "").trim();

				try {

					BigDecimal number = new BigDecimal(numericValue);

					// 8. 합계, 최솟값, 최댓값을 계산한다.
					sum = sum.add(number);

					if (min == null || number.compareTo(min) < 0) {
						min = number;
					}

					if (max == null || number.compareTo(max) > 0) {
						max = number;
					}

					validCount++;

				} catch (NumberFormatException e) {
					invalidCount++;
				}
			}
		}

		// 9. 필드가 모든 레코드에서 누락되었다면 오류로 처리한다.
		if (presentCount == 0) {
			throw new IllegalArgumentException("분석 데이터에 존재하지 않는 필드입니다: " + fieldName);
		}

		// 10. 유효한 숫자가 있는 경우 평균을 계산한다.
		BigDecimal average = null;

		if (validCount > 0) {
			average = sum.divide(BigDecimal.valueOf(validCount), 4, RoundingMode.HALF_UP);
		}

		// 11. 통계 계산 결과를 반환한다.
		AiFieldStatisticsDto statistics = new AiFieldStatisticsDto();

		statistics.setFieldName(fieldName);
		statistics.setTotalSourceCount(totalSourceCount);
		statistics.setSourceCount(preview.getResultCount());
		statistics.setTotalRecordCount(totalRecordCount);

		statistics.setValidCount(validCount);
		statistics.setNullCount(nullCount);
		statistics.setMissingCount(missingCount);
		statistics.setInvalidCount(invalidCount);

		statistics.setSum(validCount > 0 ? sum : null);
		statistics.setMin(min);
		statistics.setMax(max);
		statistics.setAverage(average);

		statistics.setComplete(true);

		return statistics;
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