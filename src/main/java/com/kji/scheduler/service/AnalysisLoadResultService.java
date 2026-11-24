package com.kji.scheduler.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.kji.scheduler.dto.AnalysisLoadResultPageDto;
import com.kji.scheduler.dto.AnalysisLoadResultSearchDto;
import com.kji.scheduler.dto.AnalysisModelDto;
import com.kji.scheduler.dto.AnalysisTargetColumnDto;
import com.kji.scheduler.mapper.AnalysisLoadMapper;
import com.kji.scheduler.mapper.AnalysisModelMapper;

@Service
public class AnalysisLoadResultService {
	
	private final AnalysisModelMapper analysisModelMapper;
	private final AnalysisModelService analysisModelService;
	private final AnalysisLoadMapper analysisLoadMapper;
	private final ObjectReader rowReader;
	
	public AnalysisLoadResultService(
			AnalysisModelMapper analysisModelMapper,
			AnalysisModelService analysisModelService,
			AnalysisLoadMapper analysisLoadMapper,
			ObjectMapper objectMapper) {
		this.analysisModelMapper = analysisModelMapper;
		this.analysisModelService = analysisModelService;
		this.analysisLoadMapper = analysisLoadMapper;
		this.rowReader = objectMapper.reader()
				.with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
	}
	
	/**
	 * 선택한 적재 모델의 대상 테이블에 저장된 데이터를 조회한다.
	 *
	 * @param searchDto 적재 모델 식별자 및 페이징 정보
	 * @return 적재 모델 정보와 실제 적재 결과 목록
	 */
	@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
	public AnalysisLoadResultPageDto getAnalysisLoadResults(AnalysisLoadResultSearchDto searchDto) {
		
		// 1. 검색조건을 검증한다.
		if (searchDto == null || searchDto.getAnalysisModelId() == null || searchDto.getAnalysisModelId() < 1) {
			throw new IllegalArgumentException("데이터 적재 모델을 선택해주세요.");
		}
		
		if (searchDto.getPage() < 1) {
			throw new IllegalArgumentException("페이지 번호는 1 이상이어야 합니다.");
		}
		
		if (searchDto.getSize() < 1 || searchDto.getSize() > 100) {
			throw new IllegalArgumentException("페이지당 조회 건수는 1~100 사이여야 합니다.");
		}
		
		// 2. 적재 모델 기본 정보를 조회한다.
		AnalysisModelDto model = analysisModelMapper.findAnalysisModel(searchDto.getAnalysisModelId());
		
		if (model == null) {
			throw new IllegalArgumentException("데이터 적재 모델을 찾을 수 없습니다.");
		}
		
		// 3. 기존 허용 테이블 정책을 적용하여 대상 컬럼 정보를 조회한다.
		List<AnalysisTargetColumnDto> columns = analysisModelService.getTargetColumnList(
				model.getTargetSchema(),
				model.getTargetTable());
		
		String schema = model.getTargetSchema().trim().toLowerCase(Locale.ROOT);
		String table = model.getTargetTable().trim().toLowerCase(Locale.ROOT);
		
		// 4. 적재 대상 테이블의 기본키 컬럼을 조회한다.
		List<String> primaryKeyColumns = analysisLoadMapper.findResultPrimaryKeyColumns(schema, table);
		
		Set<String> columnNames = columns.stream()
				.map(AnalysisTargetColumnDto::getColumnName)
				.collect(Collectors.toSet());
		
		if (primaryKeyColumns.isEmpty() || !columnNames.containsAll(primaryKeyColumns)) {
			throw new IllegalArgumentException("적재 데이터를 조회하려면 대상 테이블의 기본키 컬럼을 읽을 수 있어야 합니다.");
		}
		
		// 5. 검증된 테이블 및 컬럼 정보로 조회 조건을 구성한다.
		String target = quoteIdentifier(schema) + "." + quoteIdentifier(table);
		
		String projection = columns.stream()
				.map(column -> quoteIdentifier(column.getColumnName()))
				.collect(Collectors.joining(", "));
		
		String sort = primaryKeyColumns.stream()
				.map(column -> quoteIdentifier(column) + " DESC")
				.collect(Collectors.joining(", "));
		
		// 6. 적재 데이터 전체 건수를 조회한다.
		long totalCount = analysisLoadMapper.countAnalysisLoadResults(target);
		
		// 7. 검색조건에 해당하는 적재 데이터를 조회한다.
		List<JsonNode> content = new ArrayList<>();
		
		if (searchDto.getOffset() < totalCount) {
			
			List<String> rows = analysisLoadMapper.findAnalysisLoadResults(
					searchDto,
					target,
					projection,
					sort);
			
			// 8. 조회 결과를 JSON 응답 형식으로 변환한다.
			for (String row : rows) {
				
				try {
					content.add(rowReader.readTree(row));
					
				} catch (JsonProcessingException e) {
					throw new IllegalStateException("적재 데이터를 응답 형식으로 변환하지 못했습니다.", e);
				}
			}
		}
		
		// 9. 적재 모델 정보와 적재 결과 목록을 반환한다.
		return new AnalysisLoadResultPageDto(
				model,
				columns,
				content,
				totalCount,
				searchDto.getPage(),
				searchDto.getSize());
	}
	
	/**
	 * 검증된 SQL 식별자를 큰따옴표로 감싼다.
	 *
	 * @param identifier 스키마, 테이블 또는 컬럼명
	 * @return 인용 처리된 SQL 식별자
	 */
	private String quoteIdentifier(String identifier) {
		return "\"" + identifier.replace("\"", "\"\"") + "\"";
	}
	
}