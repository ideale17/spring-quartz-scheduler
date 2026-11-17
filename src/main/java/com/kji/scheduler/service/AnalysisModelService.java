package com.kji.scheduler.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kji.scheduler.dto.AnalysisModelDto;
import com.kji.scheduler.dto.AnalysisModelFieldDto;
import com.kji.scheduler.dto.AnalysisModelFieldRowDto;
import com.kji.scheduler.dto.AnalysisModelRequestDto;
import com.kji.scheduler.dto.AnalysisModelSearchDto;
import com.kji.scheduler.dto.AnalysisModelVersionDto;
import com.kji.scheduler.dto.TransformRuleFieldDto;
import com.kji.scheduler.dto.TransformRuleVersionDto;
import com.kji.scheduler.mapper.AnalysisModelMapper;
import com.kji.scheduler.mapper.TransformRuleMapper;

@Service
public class AnalysisModelService {
	
	private static final Set<String> ALLOWED_TARGET_TABLES =
			Set.of("public.real_estate_sale_trade");
	
	private static final Set<String> RESERVED_COLUMNS =
			Set.of(
					"sale_trade_id",
					"analysis_load_source_id",
					"source_item_no",
					"created_at");
	
	private static final Set<String> CONVERT_TYPES =
			Set.of(
					"DIRECT",
					"NUMBER_NORMALIZE",
					"DATE_PARSE",
					"DATE_PARTS");
	
	private final AnalysisModelMapper mapper;
	private final TransformRuleMapper transformRuleMapper;
	private final ObjectMapper objectMapper;
	
	public AnalysisModelService(
			AnalysisModelMapper mapper,
			TransformRuleMapper transformRuleMapper,
			ObjectMapper objectMapper) {
		this.mapper = mapper;
		this.transformRuleMapper = transformRuleMapper;
		this.objectMapper = objectMapper;
	}
	
	/**
	 * 검색조건에 해당하는 데이터 적재 모델 목록을 조회한다.
	 */
	@Transactional(readOnly = true)
	public List<AnalysisModelDto> getAnalysisModelList(AnalysisModelSearchDto searchDto) {
		
		// 1. 검색조건을 검증한다.
		if (searchDto == null) {
			searchDto = new AnalysisModelSearchDto();
		}
		
		String modelName = searchDto.getModelName();
		
		if (modelName != null) {
			modelName = modelName.trim();
			
			if (modelName.length() > 100) {
				throw new IllegalArgumentException("모델명 검색조건은 100자 이하여야 합니다.");
			}
			
			searchDto.setModelName(modelName);
		}
		
		String enabled = searchDto.getEnabled();
		
		if (enabled != null && !enabled.isEmpty() && !"Y".equals(enabled) && !"N".equals(enabled)) {
			throw new IllegalArgumentException("올바르지 않은 사용 여부 검색조건입니다.");
		}
		
		// 2. 검색조건에 해당하는 모델 목록을 조회한다.
		return mapper.findAnalysisModelList(searchDto);
	}
	

	/**
	 * 데이터 적재 모델의 현재 버전 및 필드 매핑을 조회한다.
	 *
	 * @param analysisModelId 분석 모델 식별자
	 * @return 현재 버전의 분석 모델 상세 정보
	 */
	@Transactional(readOnly = true)
	public AnalysisModelRequestDto getAnalysisModelDetail(Long analysisModelId) {
		
		// 1. 분석 모델 기본 정보를 조회한다.
		AnalysisModelDto model = getAnalysisModel(analysisModelId);
		
		// 2. 현재 분석 모델 버전 정보를 조회한다.
		AnalysisModelVersionDto version = mapper.findCurrentAnalysisModelVersion(analysisModelId);
		
		if (version == null) {
			throw new IllegalStateException("현재 데이터 적재 모델 버전을 찾을 수 없습니다.");
		}
		
		// 3. 해당 버전의 필드 매핑 정보를 조회한다.
		List<AnalysisModelFieldDto> fields = getAnalysisModelFields(version.getAnalysisModelVersionId());
		
		// 4. 상세 조회 응답을 구성한다.
		AnalysisModelRequestDto result = new AnalysisModelRequestDto();
		result.setModel(model);
		result.setVersion(version);
		result.setFields(fields);
		
		return result;
	}
	
	/**
	 * 데이터 적재 모델의 전체 버전 이력을 조회한다.
	 *
	 * @param analysisModelId 분석 모델 식별자
	 * @return 분석 모델 버전 목록
	 */
	@Transactional(readOnly = true)
	public List<AnalysisModelVersionDto> getAnalysisModelVersionList(Long analysisModelId) {
		
		// 1. 분석 모델 존재 여부를 확인한다.
		getAnalysisModel(analysisModelId);
		
		// 2. 해당 모델의 버전 이력을 조회한다.
		return mapper.findAnalysisModelVersionList(analysisModelId);
	}
	
	/**
	 * 선택한 데이터 적재 모델 버전의 상세 정보를 조회한다.
	 *
	 * @param analysisModelId 분석 모델 식별자
	 * @param analysisModelVersionId 조회 대상 모델 버전 식별자
	 * @return 선택한 버전의 분석 모델 상세 정보
	 */
	@Transactional(readOnly = true)
	public AnalysisModelRequestDto getAnalysisModelVersionDetail(
			Long analysisModelId, Long analysisModelVersionId) {
		
		// 1. 분석 모델 기본 정보를 조회한다.
		AnalysisModelDto model = getAnalysisModel(analysisModelId);
		
		// 2. 요청한 버전이 해당 모델에 속하는지 확인한다.
		if (analysisModelVersionId == null || analysisModelVersionId < 1) {
			throw new IllegalArgumentException("데이터 적재 모델 버전 식별자가 올바르지 않습니다.");
		}
		
		AnalysisModelVersionDto version = mapper.findAnalysisModelVersion(analysisModelVersionId);
		
		if (version == null || !analysisModelId.equals(version.getAnalysisModelId())) {
			throw new IllegalArgumentException("데이터 적재 모델 버전을 찾을 수 없습니다.");
		}
		
		// 3. 선택한 버전의 필드 매핑 정보를 조회한다.
		List<AnalysisModelFieldDto> fields = getAnalysisModelFields(analysisModelVersionId);
		
		// 4. 버전별 상세 조회 응답을 구성한다.
		AnalysisModelRequestDto result = new AnalysisModelRequestDto();
		result.setModel(model);
		result.setVersion(version);
		result.setFields(fields);
		
		return result;
	}
	
	/**
	 * 분석 모델 기본 정보를 단건 조회하고 존재 여부를 검증한다.
	 *
	 * @param analysisModelId 분석 모델 식별자
	 * @return 분석 모델 기본 정보
	 */
	private AnalysisModelDto getAnalysisModel(Long analysisModelId) {
		
		// 1. 분석 모델 식별자를 검증한다.
		if (analysisModelId == null || analysisModelId < 1) {
			throw new IllegalArgumentException("데이터 적재 모델 식별자가 올바르지 않습니다.");
		}
		
		// 2. 분석 모델을 조회한다.
		AnalysisModelDto model = mapper.findAnalysisModel(analysisModelId);
		
		if (model == null) {
			throw new IllegalArgumentException(
					"데이터 적재 모델을 찾을 수 없습니다. analysisModelId: " + analysisModelId);
		}
		
		return model;
	}
	
	/**
	 * 모델 버전의 필드 매핑을 조회하고 JSONB 원본 필드 목록을 변환한다.
	 *
	 * @param analysisModelVersionId 모델 버전 식별자
	 * @return 필드 매핑 목록
	 */
	private List<AnalysisModelFieldDto> getAnalysisModelFields(Long analysisModelVersionId) {
		
		// 1. 모델 버전에 등록된 필드 매핑을 조회한다.
		List<AnalysisModelFieldRowDto> rows =
				mapper.findAnalysisModelFieldList(analysisModelVersionId);
		
		List<AnalysisModelFieldDto> fields = new ArrayList<>();
		
		// 2. DB 조회 결과를 API 응답 DTO로 변환한다.
		for (AnalysisModelFieldRowDto row : rows) {
			
			AnalysisModelFieldDto field = new AnalysisModelFieldDto();
			field.setAnalysisFieldId(row.getAnalysisFieldId());
			field.setAnalysisModelVersionId(row.getAnalysisModelVersionId());
			field.setTargetColumn(row.getTargetColumn());
			field.setConvertType(row.getConvertType());
			field.setConvertFormat(row.getConvertFormat());
			field.setRequiredYn(row.getRequiredYn());
			field.setSortOrder(row.getSortOrder());
			
			// 3. SOURCE_FIELDS JSONB 문자열을 List<String>으로 변환한다.
			try {
				List<String> sourceFields = objectMapper.readValue(
						row.getSourceFieldsJson(),
						new TypeReference<List<String>>() {});
				
				field.setSourceFields(sourceFields);
				
			} catch (JsonProcessingException e) {
				throw new IllegalStateException(
						"데이터 적재 모델 필드 매핑의 원본 필드 변환에 실패했습니다. analysisFieldId: "
								+ row.getAnalysisFieldId(), e);
			}
			
			fields.add(field);
		}
		
		// 4. 변환된 필드 매핑 목록을 반환한다.
		return fields;
	}
	
	/**
	 * 데이터 적재 모델과 최초 버전 및 필드 매핑을 등록한다.
	 *
	 * @param request 모델 등록 정보
	 * @return 생성된 분석 모델 식별자
	 */
	@Transactional
	public Long createAnalysisModel(AnalysisModelRequestDto request) {
		
		// 1. 모델 등록 요청을 검증한다.
		validateRequest(request);
		
		AnalysisModelDto model = request.getModel();
		AnalysisModelVersionDto version = request.getVersion();
		
		// 2. 모델 기본 정보를 등록한다.
		model.setAnalysisModelId(null);
		model.setCurrentVersion(1);
		
		if (mapper.insertAnalysisModel(model) != 1 || model.getAnalysisModelId() == null) {
			throw new IllegalStateException("데이터 적재 모델 등록에 실패했습니다.");
		}
		
		// 3. 최초 모델 버전을 등록한다.
		version.setAnalysisModelVersionId(null);
		version.setAnalysisModelId(model.getAnalysisModelId());
		version.setVersionNo(1);
		
		if (mapper.insertAnalysisModelVersion(version) != 1 || version.getAnalysisModelVersionId() == null) {
			throw new IllegalStateException("데이터 적재 모델 버전 등록에 실패했습니다.");
		}
		
		// 4. 최초 버전의 필드 매핑을 등록한다.
		for (AnalysisModelFieldDto field : request.getFields()) {
			field.setAnalysisFieldId(null);
			field.setAnalysisModelVersionId(version.getAnalysisModelVersionId());
			
			String sourceFieldsJson;
			
			try {
				sourceFieldsJson = objectMapper.writeValueAsString(field.getSourceFields());
			} catch (JsonProcessingException e) {
				throw new IllegalStateException("원본 필드 목록 변환에 실패했습니다.", e);
			}
			
			if (mapper.insertAnalysisModelField(field, sourceFieldsJson) != 1) {
				throw new IllegalStateException("필드 매핑 등록에 실패했습니다. targetColumn: " + field.getTargetColumn());
			}
		}
		
		// 5. 생성된 모델 ID를 반환한다.
		return model.getAnalysisModelId();
	}
	
	/**
	 * 모델 기본 정보, Transform 필드 및 DB 컬럼 매핑을 검증한다.
	 */
	private void validateRequest(AnalysisModelRequestDto request) {
		
		// 1. 필수 요청 정보를 확인한다.
		if (request == null || request.getModel() == null || request.getVersion() == null) {
			throw new IllegalArgumentException("데이터 적재 모델 등록 정보가 없습니다.");
		}
		
		AnalysisModelDto model = request.getModel();
		AnalysisModelVersionDto version = request.getVersion();
		
		// 2. 모델 기본 정보를 검증한다.
		model.setModelName(normalize(model.getModelName()));
		
		if (model.getModelName() == null) {
			throw new IllegalArgumentException("데이터 적재 모델명은 필수입니다.");
		}
		
		if (model.getModelName().length() > 100) {
			throw new IllegalArgumentException("데이터 적재 모델명은 100자 이하여야 합니다.");
		}
		
		if (model.getDescription() != null && model.getDescription().length() > 500) {
			throw new IllegalArgumentException("데이터 적재 모델 설명은 500자 이하여야 합니다.");
		}
		
		if (model.getEnabled() == null || model.getEnabled().isBlank()) {
			model.setEnabled("Y");
		}
		
		if (!"Y".equals(model.getEnabled()) && !"N".equals(model.getEnabled())) {
			throw new IllegalArgumentException("모델 사용 여부가 올바르지 않습니다.");
		}
		
		// 3. 적재 대상 테이블을 검증한다.
		String targetSchema = normalize(model.getTargetSchema());
		String targetTable = normalize(model.getTargetTable());
		
		if (targetSchema == null) {
			targetSchema = "public";
		}
		
		if (targetTable == null) {
			throw new IllegalArgumentException("적재 대상 테이블은 필수입니다.");
		}
		
		targetSchema = targetSchema.toLowerCase(Locale.ROOT);
		targetTable = targetTable.toLowerCase(Locale.ROOT);
		
		if (targetSchema.length() > 100 || targetTable.length() > 100) {
			throw new IllegalArgumentException("대상 스키마 또는 테이블명이 너무 깁니다.");
		}
		
		if (!ALLOWED_TARGET_TABLES.contains(targetSchema + "." + targetTable)) {
			throw new IllegalArgumentException("허용되지 않은 적재 대상 테이블입니다.");
		}
		
		model.setTargetSchema(targetSchema);
		model.setTargetTable(targetTable);
		
		// 4. 지정한 Transform 버전이 존재하는지 검증한다.
		Long transformRuleVersionId = version.getTransformRuleVersionId();
		
		if (transformRuleVersionId == null || transformRuleVersionId < 1) {
			throw new IllegalArgumentException("가공 규칙 버전을 선택해야 합니다.");
		}
		
		TransformRuleVersionDto transformVersion = transformRuleMapper.findTransformRuleVersion(transformRuleVersionId);
		
		if (transformVersion == null) {
			throw new IllegalArgumentException("가공 규칙 버전을 찾을 수 없습니다.");
		}
		
		// 5. 선택한 Transform 버전의 출력 필드를 조회한다.
		List<TransformRuleFieldDto> transformFields = transformRuleMapper.findTransformRuleFieldList(transformRuleVersionId);
		
		Set<String> sourceNames = new HashSet<>();
		
		for (TransformRuleFieldDto transformField : transformFields) {
			sourceNames.add(transformField.getTargetName());
		}
		
		// 6. 실제 업무 테이블 컬럼 목록을 조회한다.
		List<String> targetColumns = mapper.findTargetColumnNames(targetSchema, targetTable);
		
		if (targetColumns.isEmpty()) {
			throw new IllegalArgumentException("적재 대상 테이블 정보를 찾을 수 없습니다.");
		}
		
		Set<String> allowedColumns = new HashSet<>(targetColumns);
		
		// 7. 필드 매핑 목록을 검증한다.
		if (request.getFields() == null || request.getFields().isEmpty()) {
			throw new IllegalArgumentException("필드 매핑을 최소 1개 이상 등록해야 합니다.");
		}
		
		Set<String> mappedColumns = new HashSet<>();
		
		for (int i = 0; i < request.getFields().size(); i++) {
			AnalysisModelFieldDto field = request.getFields().get(i);
			
			if (field == null) {
				throw new IllegalArgumentException("필드 매핑 정보가 올바르지 않습니다.");
			}
			
			String targetColumn = normalize(field.getTargetColumn());
			
			if (targetColumn == null || targetColumn.length() > 100) {
				throw new IllegalArgumentException("대상 컬럼명이 올바르지 않습니다.");
			}
			
			targetColumn = targetColumn.toLowerCase(Locale.ROOT);
			field.setTargetColumn(targetColumn);
			
			if (!allowedColumns.contains(targetColumn)) {
				throw new IllegalArgumentException("DB에 존재하지 않는 대상 컬럼입니다: " + targetColumn);
			}
			
			if (RESERVED_COLUMNS.contains(targetColumn)) {
				throw new IllegalArgumentException("시스템 관리 컬럼은 직접 매핑할 수 없습니다: " + targetColumn);
			}
			
			if (!mappedColumns.add(targetColumn)) {
				throw new IllegalArgumentException("중복된 대상 컬럼입니다: " + targetColumn);
			}
			
			// 8. 원본 JSONB 필드명을 검증한다.
			List<String> sources = field.getSourceFields();
			
			if (sources == null || sources.isEmpty()) {
				throw new IllegalArgumentException("원본 필드를 선택해야 합니다: " + targetColumn);
			}
			
			if (sources.stream().anyMatch(source -> source == null || source.isBlank())) {
				throw new IllegalArgumentException("빈 원본 필드명이 포함되어 있습니다.");
			}
			
			List<String> normalizedSources = sources.stream().map(String::trim).toList();
			
			if (new HashSet<>(normalizedSources).size() != normalizedSources.size()) {
				throw new IllegalArgumentException("원본 필드가 중복되었습니다: " + targetColumn);
			}
			
			for (String source : normalizedSources) {
				if (!sourceNames.contains(source)) {
					throw new IllegalArgumentException("가공 규칙에 없는 출력 필드입니다: " + source);
				}
			}
			
			field.setSourceFields(normalizedSources);
			
			// 9. 필드 변환 설정을 검증한다.
			String convertType = normalize(field.getConvertType());
			
			if (convertType == null) {
				convertType = "DIRECT";
			}
			
			if (!CONVERT_TYPES.contains(convertType)) {
				throw new IllegalArgumentException("지원하지 않는 변환 방식입니다: " + convertType);
			}
			
			if ("DATE_PARTS".equals(convertType)) {
				if (normalizedSources.size() != 3) {
					throw new IllegalArgumentException("DATE_PARTS는 연, 월, 일 필드 3개가 필요합니다.");
				}
			} else if (normalizedSources.size() != 1) {
				throw new IllegalArgumentException("선택한 변환 방식은 원본 필드 1개만 사용할 수 있습니다.");
			}
			
			field.setConvertType(convertType);
			field.setConvertFormat(normalize(field.getConvertFormat()));
			
			if (field.getConvertFormat() != null && field.getConvertFormat().length() > 100) {
				throw new IllegalArgumentException("날짜 변환 형식은 100자 이하여야 합니다.");
			}
			
			if ("DATE_PARSE".equals(convertType) && field.getConvertFormat() == null) {
				throw new IllegalArgumentException("DATE_PARSE는 날짜 입력 형식이 필요합니다.");
			}
			
			if (field.getRequiredYn() == null || field.getRequiredYn().isBlank()) {
				field.setRequiredYn("N");
			}
			
			if (!"Y".equals(field.getRequiredYn()) && !"N".equals(field.getRequiredYn())) {
				throw new IllegalArgumentException("필드 필수 여부가 올바르지 않습니다.");
			}
			
			// 10. 필드 순서를 화면의 배열 순서로 설정한다.
			field.setSortOrder(i);
		}
		
		// 11. DB의 NOT NULL 컬럼이 매핑되어 있는지 검증한다.
		List<String> requiredColumns = mapper.findRequiredTargetColumnNames(targetSchema, targetTable);
		
		for (String requiredColumn : requiredColumns) {
			
			// 분석 적재 시스템이 자동으로 채우는 관리 컬럼은 제외한다.
			if (RESERVED_COLUMNS.contains(requiredColumn)) {
				continue;
			}
			
			if (!mappedColumns.contains(requiredColumn)) {
				throw new IllegalArgumentException("필수 DB 컬럼에 대한 매핑이 없습니다: " + requiredColumn);
			}
			
			// 필수 DB 컬럼은 매핑 설정에서도 필수로 지정해야 한다.
			for (AnalysisModelFieldDto field : request.getFields()) {
				if (requiredColumn.equals(field.getTargetColumn()) && !"Y".equals(field.getRequiredYn())) {
					throw new IllegalArgumentException("필수 DB 컬럼은 필수 매핑으로 설정해야 합니다: " + requiredColumn);
				}
			}
		}
	}
	
	/**
	 * 공백 문자열을 null로 변환한다.
	 */
	private String normalize(String value) {
		
		if (value == null || value.isBlank()) {
			return null;
		}
		
		return value.trim();
	}
	
	/**
	 * 데이터 적재 모델을 수정한다.
	 *
	 * 기본 정보만 변경된 경우 현재 버전을 유지하고,
	 * Transform 버전 또는 필드 매핑이 변경된 경우 새 모델 버전을 생성한다.
	 *
	 * @param analysisModelId 수정 대상 모델 식별자
	 * @param request 수정 요청 정보
	 */
	@Transactional
	public void updateAnalysisModel(Long analysisModelId, AnalysisModelRequestDto request) {
		
		// 1. 모델 식별자와 수정 요청 정보를 검증한다.
		if (analysisModelId == null || analysisModelId < 1) {
			throw new IllegalArgumentException("데이터 적재 모델 식별자가 올바르지 않습니다.");
		}
		
		if (request == null || request.getModel() == null || request.getVersion() == null) {
			throw new IllegalArgumentException("데이터 적재 모델 수정 정보가 없습니다.");
		}
		
		// 2. 수정 화면에서 조회한 모델 버전과 현재 버전을 비교한다.
		Integer expectedVersion = request.getModel().getCurrentVersion();
		AnalysisModelDto savedModel = getAnalysisModel(analysisModelId);
		
		if (expectedVersion == null || !expectedVersion.equals(savedModel.getCurrentVersion())) {
			throw new IllegalStateException("데이터 적재 모델이 변경되었거나 버전 정보가 없습니다. 다시 조회해 주세요.");
		}
		
		// 3. 변경할 설정을 기존 등록 API와 동일하게 검증한다.
		validateRequest(request);
		
		AnalysisModelDto model = request.getModel();
		AnalysisModelVersionDto version = request.getVersion();
		
		// 4. 적재 대상 업무 테이블은 변경할 수 없도록 제한한다.
		if (!Objects.equals(savedModel.getTargetSchema(), model.getTargetSchema()) || !Objects.equals(savedModel.getTargetTable(), model.getTargetTable())) {
			throw new IllegalArgumentException("적재 대상 테이블은 변경할 수 없습니다. 새로운 모델을 등록해 주세요.");
		}
		
		// 5. 현재 버전의 설정과 필드 매핑을 조회한다.
		AnalysisModelVersionDto savedVersion = mapper.findCurrentAnalysisModelVersion(analysisModelId);
		
		if (savedVersion == null) {
			throw new IllegalStateException("현재 데이터 적재 모델 버전을 찾을 수 없습니다.");
		}
		
		List<AnalysisModelFieldDto> savedFields = getAnalysisModelFields(savedVersion.getAnalysisModelVersionId());
		
		// 6. 기본 정보 변경 여부를 확인한다.
		boolean basicChanged =
				!Objects.equals(savedModel.getModelName(), model.getModelName())
				|| !Objects.equals(savedModel.getDescription(), model.getDescription())
				|| !Objects.equals(savedModel.getEnabled(), model.getEnabled());
		
		// 7. 버전 생성이 필요한 변경인지 확인한다.
		boolean mappingChanged =
				isAnalysisModelChanged(
						savedVersion,
						savedFields,
						version,
						request.getFields());
		
		// 8. 수정할 항목이 없다면 DB를 변경하지 않는다.
		if (!basicChanged && !mappingChanged) {
			return;
		}
		
		// 9. 기본 정보가 변경되었다면 수정한다.
		if (basicChanged) {
			model.setAnalysisModelId(analysisModelId);
			model.setCurrentVersion(expectedVersion);
			
			if (mapper.updateAnalysisModel(model) != 1) {
				throw new IllegalStateException("데이터 적재 모델이 변경되었습니다. 다시 조회해 주세요.");
			}
		}
		
		// 10. 매핑 설정이 변경되지 않았다면 종료한다.
		if (!mappingChanged) {
			return;
		}
		
		// 11. 새 모델 버전을 등록한다.
		int nextVersion = savedModel.getCurrentVersion() + 1;
		
		version.setAnalysisModelVersionId(null);
		version.setAnalysisModelId(analysisModelId);
		version.setVersionNo(nextVersion);
		
		if (mapper.insertAnalysisModelVersion(version) != 1 || version.getAnalysisModelVersionId() == null) {
			throw new IllegalStateException("새 데이터 적재 모델 버전 등록에 실패했습니다.");
		}
		
		// 12. 새 버전에 필드 매핑을 모두 등록한다.
		for (AnalysisModelFieldDto field : request.getFields()) {
			field.setAnalysisFieldId(null);
			field.setAnalysisModelVersionId(version.getAnalysisModelVersionId());
			
			String sourceFieldsJson;
			
			try {
				sourceFieldsJson = objectMapper.writeValueAsString(field.getSourceFields());
			} catch (JsonProcessingException e) {
				throw new IllegalStateException("원본 필드 목록 변환에 실패했습니다.", e);
			}
			
			if (mapper.insertAnalysisModelField(field, sourceFieldsJson) != 1) {
				throw new IllegalStateException("새 버전의 필드 매핑 등록에 실패했습니다. targetColumn: " + field.getTargetColumn());
			}
		}
		
		// 13. 현재 모델 버전 번호를 변경한다.
		if (mapper.updateAnalysisModelCurrentVersion(analysisModelId, expectedVersion, nextVersion) != 1) {
			throw new IllegalStateException("데이터 적재 모델의 버전이 변경되었습니다. 다시 조회해 주세요.");
		}
	}
	
	/**
	 * 분석 모델의 버전 설정 및 필드 매핑 변경 여부를 확인한다.
	 *
	 * @param savedVersion 저장된 모델 버전
	 * @param savedFields 저장된 필드 매핑 목록
	 * @param version 수정 요청 버전
	 * @param fields 수정 요청 필드 매핑 목록
	 * @return 변경 여부
	 */
	private boolean isAnalysisModelChanged(
			AnalysisModelVersionDto savedVersion,
			List<AnalysisModelFieldDto> savedFields,
			AnalysisModelVersionDto version,
			List<AnalysisModelFieldDto> fields) {
		
		// 1. 사용할 Transform 규칙 버전이 변경되었는지 확인한다.
		if (!Objects.equals(
				savedVersion.getTransformRuleVersionId(),
				version.getTransformRuleVersionId())) {
			return true;
		}
		
		// 2. 필드 개수가 변경되었는지 확인한다.
		if (savedFields.size() != fields.size()) {
			return true;
		}
		
		// 3. 필드별 매핑 설정을 순서대로 비교한다.
		for (int i = 0; i < fields.size(); i++) {
			
			AnalysisModelFieldDto saved = savedFields.get(i);
			AnalysisModelFieldDto changed = fields.get(i);
			
			if (!Objects.equals(saved.getTargetColumn(), changed.getTargetColumn())
					|| !Objects.equals(saved.getSourceFields(), changed.getSourceFields())
					|| !Objects.equals(saved.getConvertType(), changed.getConvertType())
					|| !Objects.equals(saved.getConvertFormat(), changed.getConvertFormat())
					|| !Objects.equals(saved.getRequiredYn(), changed.getRequiredYn())
					|| !Objects.equals(saved.getSortOrder(), changed.getSortOrder())) {
				return true;
			}
		}
		
		return false;
	}
	
}