package com.kji.scheduler.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kji.scheduler.dto.AnalysisLoadColumnValueDto;
import com.kji.scheduler.dto.AnalysisLoadSourceDto;
import com.kji.scheduler.dto.AnalysisModelFieldDto;
import com.kji.scheduler.dto.AnalysisModelRequestDto;
import com.kji.scheduler.dto.AnalysisTargetColumnDto;
import com.kji.scheduler.mapper.AnalysisLoadMapper;
import com.kji.scheduler.mapper.AnalysisModelMapper;

@Service
public class AnalysisLoadExecutionService {
	
	private static final String TARGET_TABLE = "public.real_estate_sale_trade";
	
	private static final Set<String> RESERVED_COLUMNS = Set.of(
			"sale_trade_id",
			"analysis_load_source_id",
			"source_item_no",
			"created_at");
	
	private static final Set<String> CONVERT_TYPES = Set.of(
			"DIRECT",
			"NUMBER_NORMALIZE",
			"DATE_PARSE");
	
	private static final Set<String> NUMBER_TYPES = Set.of(
			"smallint",
			"integer",
			"bigint",
			"numeric",
			"decimal");
	
	private final AnalysisLoadMapper loadMapper;
	private final AnalysisModelMapper modelMapper;
	private final AnalysisModelService modelService;
	private final ObjectMapper objectMapper;
	
	public AnalysisLoadExecutionService(
			AnalysisLoadMapper loadMapper,
			AnalysisModelMapper modelMapper,
			AnalysisModelService modelService,
			ObjectMapper objectMapper) {
		this.loadMapper = loadMapper;
		this.modelMapper = modelMapper;
		this.modelService = modelService;
		this.objectMapper = objectMapper;
	}
	
	/**
	 * 선점한 적재 작업을 실행하고 업무 테이블에 데이터를 저장한다.
	 * 적재 데이터 저장과 SUCCESS 상태 변경은 하나의 트랜잭션으로 처리한다.
	 *
	 * @param analysisLoadSourceId 적재 작업 식별자
	 */
	@Transactional
	public void executeLoad(Long analysisLoadSourceId) {
		
		// 1. 선점한 적재 작업과 가공 결과를 조회한다.
		AnalysisLoadSourceDto source = loadMapper.findAnalysisLoadSource(analysisLoadSourceId);
		
		if (source == null || !"RUNNING".equals(source.getStatus())) {
			throw new IllegalStateException("실행 가능한 데이터 적재 작업을 찾을 수 없습니다.");
		}
		
		// 2. 적재 대상 테이블이 허용된 테이블인지 확인한다.
		String targetSchema = source.getTargetSchema();
		String targetTable = source.getTargetTable();
		
		if (targetSchema == null || targetTable == null || !TARGET_TABLE.equals(targetSchema + "." + targetTable)) {
			throw new IllegalArgumentException("허용되지 않은 적재 대상 테이블입니다.");
		}
		
		// 3. 작업에 확정된 모델 버전과 필드 매핑을 조회한다.
		AnalysisModelRequestDto modelDetail = modelService.getAnalysisModelVersionDetail(
				source.getAnalysisModelId(),
				source.getAnalysisModelVersionId());
		
		List<AnalysisModelFieldDto> fields = modelDetail.getFields();
		
		if (fields == null || fields.isEmpty()) {
			throw new IllegalStateException("적재 모델 필드 매핑 정보가 없습니다.");
		}
		
		// 4. 실제 업무 테이블의 컬럼 정보를 조회한다.
		List<AnalysisTargetColumnDto> columns = modelMapper.findTargetColumnList(targetSchema, targetTable);
		
		if (columns == null || columns.isEmpty()) {
			throw new IllegalStateException("적재 대상 테이블 컬럼 정보를 찾을 수 없습니다.");
		}
		
		Map<String, AnalysisTargetColumnDto> columnMap = new HashMap<>();
		
		for (AnalysisTargetColumnDto column : columns) {
			columnMap.put(column.getColumnName(), column);
		}
		
		// 5. 필드 매핑 및 DB 필수 컬럼을 검증한다.
		validateFieldMappings(fields, columns, columnMap);
		
		// 6. 저장된 가공 결과 JSON을 파싱한다.
		JsonNode result;
		
		try {
			result = objectMapper.readTree(source.getResultData());

		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("가공 결과 JSON 형식이 올바르지 않습니다.", e);
		}
		
		if (result == null || (!result.isObject() && !result.isArray())) {
			throw new IllegalArgumentException("가공 결과는 JSON 객체 또는 배열이어야 합니다.");
		}
		
		// 7. 단일 객체와 배열을 동일한 적재 흐름으로 처리한다.
		List<JsonNode> items = new ArrayList<>();
		
		if (result.isArray()) {
			result.forEach(items::add);
		} else {
			items.add(result);
		}
		
		// 8. 각 가공 결과를 변환하여 업무 테이블에 저장한다.
		int loadedCount = 0;
		
		for (int i = 0; i < items.size(); i++) {
			JsonNode item = items.get(i);
			
			if (item == null || !item.isObject()) {
				throw new IllegalArgumentException("가공 결과 항목은 JSON 객체여야 합니다. sourceItemNo: " + i);
			}
			
			List<AnalysisLoadColumnValueDto> values = new ArrayList<>();
			
			for (AnalysisModelFieldDto field : fields) {
				String targetColumn = field.getTargetColumn();
				String sourceField = field.getSourceFields().get(0);
				
				AnalysisTargetColumnDto column = columnMap.get(targetColumn);
				JsonNode sourceValue = item.get(sourceField);
				
				// 9. 매핑 설정과 DB 컬럼 타입을 기준으로 값을 변환한다.
				Object value = convertValue(sourceValue, field, column);
				
				if (value == null && ("Y".equals(field.getRequiredYn()) || Boolean.TRUE.equals(column.getRequired()))) {
					throw new IllegalArgumentException("필수 적재 값이 없습니다. sourceItemNo: " + i + ", targetColumn: " + targetColumn);
				}
				
				values.add(new AnalysisLoadColumnValueDto(targetColumn, value));
			}
			
			// 10. 변환된 한 건을 업무 테이블에 저장한다.
			if (loadMapper.insertRealEstateSaleTrade(analysisLoadSourceId, i, values) != 1) {
				throw new IllegalStateException("분석 데이터 저장에 실패했습니다. sourceItemNo: " + i);
			}
			
			loadedCount++;
		}
		
		// 11. 전체 저장 성공 후 적재 건수와 상태를 변경한다.
		if (loadMapper.markLoadSuccess(analysisLoadSourceId, loadedCount) != 1) {
			throw new IllegalStateException("적재 작업 성공 상태 변경에 실패했습니다.");
		}
		
	}
	
	/**
	 * 모델의 필드 매핑 및 대상 테이블의 필수 컬럼 설정을 검증한다.
	 *
	 * @param fields 모델 버전별 필드 매핑
	 * @param columns 대상 테이블 컬럼 정보
	 * @param columnMap 대상 컬럼명별 메타데이터
	 */
	private void validateFieldMappings(
			List<AnalysisModelFieldDto> fields,
			List<AnalysisTargetColumnDto> columns,
			Map<String, AnalysisTargetColumnDto> columnMap) {
		
		// 1. 매핑된 컬럼의 중복 여부를 관리한다.
		Set<String> mappedColumns = new HashSet<>();
		
		// 2. DB에 존재하는 일반 컬럼만 매핑하도록 검증한다.
		for (AnalysisModelFieldDto field : fields) {
			String targetColumn = field.getTargetColumn();
			
			if (targetColumn == null
					|| !targetColumn.matches("[a-z][a-z0-9_]*")
					|| RESERVED_COLUMNS.contains(targetColumn)
					|| !columnMap.containsKey(targetColumn)
					|| Boolean.TRUE.equals(columnMap.get(targetColumn).getSystemManaged())
					|| !mappedColumns.add(targetColumn)) {
				throw new IllegalArgumentException("허용되지 않거나 중복된 매핑 컬럼입니다: " + targetColumn);
			}
			
			// 3. 현재 지원하는 단일 원본 필드 매핑인지 검증한다.
			List<String> sourceFields = field.getSourceFields();
			
			if (sourceFields == null || sourceFields.size() != 1 || sourceFields.get(0) == null || sourceFields.get(0).isBlank()) {
				throw new IllegalArgumentException("원본 필드는 한 개여야 합니다. targetColumn: " + targetColumn);
			}
			
			// 4. 지원하는 적재 변환 방식인지 검증한다.
			if (!CONVERT_TYPES.contains(field.getConvertType())) {
				throw new IllegalArgumentException("지원하지 않는 적재 변환 방식입니다: " + field.getConvertType());
			}
		}
		
		// 5. 업무 테이블의 필수 컬럼이 모두 매핑되어 있는지 확인한다.
		for (AnalysisTargetColumnDto column : columns) {
			if (Boolean.TRUE.equals(column.getRequired())
					&& !Boolean.TRUE.equals(column.getSystemManaged())
					&& !mappedColumns.contains(column.getColumnName())) {
				throw new IllegalArgumentException("필수 DB 컬럼 매핑이 없습니다: " + column.getColumnName());
			}
		}
		
	}
	
	/**
	 * 가공 결과의 값을 적재 모델 설정과 DB 컬럼 타입에 맞게 변환한다.
	 *
	 * @param node 원본 JSON 값
	 * @param field 적재 모델 필드 매핑
	 * @param column 대상 DB 컬럼 정보
	 * @return DB 저장에 사용할 값
	 */
	private Object convertValue(JsonNode node, AnalysisModelFieldDto field, AnalysisTargetColumnDto column) {
		
		// 1. NULL 또는 빈 값을 확인한다.
		if (node == null || node.isNull()) {
			return null;
		}
		
		if (!node.isValueNode() || node.isBoolean()) {
			throw new IllegalArgumentException("지원하지 않는 원본 값 형식입니다: " + field.getTargetColumn());
		}
		
		String value = node.asText();
		
		if (value.isBlank()) {
			return null;
		}
		
		String convertType = field.getConvertType();
		String dataType = column.getDataType().toLowerCase(Locale.ROOT);
		
		// 2. DATE_PARSE는 지정된 입력 형식으로 DATE 값을 생성한다.
		if ("DATE_PARSE".equals(convertType)) {
			if (!"date".equals(dataType)) {
				throw new IllegalArgumentException("DATE_PARSE는 DATE 타입 컬럼에서만 사용할 수 있습니다.");
			}
			
			if (field.getConvertFormat() == null || field.getConvertFormat().isBlank()) {
				throw new IllegalArgumentException("날짜 변환 형식이 없습니다. targetColumn: " + field.getTargetColumn());
			}
			
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(field.getConvertFormat());
			
			return LocalDate.parse(value.trim(), formatter);
		}
		
		// 3. NUMBER_NORMALIZE는 숫자 컬럼의 쉼표와 공백을 제거한다.
		if ("NUMBER_NORMALIZE".equals(convertType)) {
			if (!NUMBER_TYPES.contains(dataType)) {
				throw new IllegalArgumentException("NUMBER_NORMALIZE는 숫자 컬럼에서만 사용할 수 있습니다.");
			}
			
			value = value.replace(",", "").replaceAll("\\s+", "");
		}
		
		// 4. 대상 DB 컬럼 타입에 맞는 Java 값으로 변환한다.
		switch (dataType) {
			case "smallint":
				return new BigDecimal(value.trim()).shortValueExact();
				
			case "integer":
				return new BigDecimal(value.trim()).intValueExact();
				
			case "bigint":
				return new BigDecimal(value.trim()).longValueExact();
				
			case "numeric":
			case "decimal":
				return new BigDecimal(value.trim());
				
			case "date":
				return LocalDate.parse(value.trim());
				
			case "character varying":
			case "character":
			case "text":
				return value;
				
			default:
				throw new IllegalArgumentException("지원하지 않는 적재 대상 컬럼 타입입니다: " + dataType);
		}
		
	}
	
}