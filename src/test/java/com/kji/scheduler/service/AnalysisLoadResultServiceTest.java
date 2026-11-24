package com.kji.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kji.scheduler.dto.AnalysisLoadResultPageDto;
import com.kji.scheduler.dto.AnalysisLoadResultSearchDto;
import com.kji.scheduler.dto.AnalysisModelDto;
import com.kji.scheduler.dto.AnalysisTargetColumnDto;
import com.kji.scheduler.mapper.AnalysisLoadMapper;
import com.kji.scheduler.mapper.AnalysisModelMapper;
import com.kji.scheduler.mapper.TransformRuleMapper;

class AnalysisLoadResultServiceTest {
	
	private static final String TABLE = "real_estate_sale_trade";
	private static final String TARGET = "\"public\".\"real_estate_sale_trade\"";
	
	private AnalysisModelMapper modelMapper;
	private AnalysisLoadMapper loadMapper;
	private AnalysisModelService modelService;
	private AnalysisLoadResultService service;
	
	@BeforeEach
	void setUp() {
		
		modelMapper = mock(AnalysisModelMapper.class);
		loadMapper = mock(AnalysisLoadMapper.class);
		
		ObjectMapper objectMapper = new ObjectMapper();
		
		modelService = new AnalysisModelService(
				modelMapper,
				mock(TransformRuleMapper.class),
				objectMapper);
		
		service = new AnalysisLoadResultService(
				modelMapper,
				modelService,
				loadMapper,
				objectMapper);
	}
	
	@Test
	void 중지된_모델의_저장된_값과_컬럼_정보를_반환한다() {
		
		// 1. 적재 모델 및 검색조건을 설정한다.
		givenModel("public", TABLE);
		
		AnalysisLoadResultSearchDto searchDto = createSearchDto(1L, 2, 50);
		
		// 2. 대상 테이블의 컬럼 정보를 설정한다.
		AnalysisTargetColumnDto id = column("sale_trade_id");
		AnalysisTargetColumnDto price = column("price_10k_krw");
		AnalysisTargetColumnDto area = column("exclu_use_ar");
		
		when(modelMapper.findTargetColumnList("public", TABLE))
				.thenReturn(List.of(id, price, area));
		
		when(loadMapper.findResultPrimaryKeyColumns("public", TABLE))
				.thenReturn(List.of("sale_trade_id"));
		
		// 3. 적재 결과 조회 데이터를 설정한다.
		when(loadMapper.countAnalysisLoadResults(TARGET)).thenReturn(51L);
		
		when(loadMapper.findAnalysisLoadResults(
				searchDto,
				TARGET,
				"\"sale_trade_id\", \"price_10k_krw\", \"exclu_use_ar\"",
				"\"sale_trade_id\" DESC"))
				.thenReturn(List.of(
						"{\"sale_trade_id\":1,\"price_10k_krw\":null,\"exclu_use_ar\":84.9123}"));
		
		// 4. 적재 결과 목록을 조회한다.
		AnalysisLoadResultPageDto result = service.getAnalysisLoadResults(searchDto);
		
		// 5. 적재 결과와 페이징 정보를 검증한다.
		assertEquals(List.of(id, price, area), result.getColumns());
		assertEquals(51L, result.getTotalCount());
		assertEquals(2, result.getPage());
		assertEquals(50, result.getSize());
		assertEquals(50L, searchDto.getOffset());
		assertEquals(1, result.getContent().size());
		
		assertTrue(result.getContent().get(0).path("price_10k_krw").isNull());
		
		assertEquals(
				new BigDecimal("84.9123"),
				result.getContent().get(0).path("exclu_use_ar").decimalValue());
	}
	
	@Test
	void 허용되지_않은_테이블은_행_조회에_사용하지_않는다() {
		
		// 1. 허용되지 않은 대상 테이블과 검색조건을 설정한다.
		givenModel("public", "app_user");
		
		AnalysisLoadResultSearchDto searchDto = createSearchDto(1L, 1, 50);
		
		// 2. 허용되지 않은 대상 테이블 조회를 검증한다.
		assertThrows(IllegalArgumentException.class,
				() -> service.getAnalysisLoadResults(searchDto));
		
		// 3. 실제 적재 데이터 조회가 실행되지 않는지 확인한다.
		verifyNoInteractions(loadMapper);
	}
	
	@Test
	void 유효하지_않은_페이지는_모델을_조회하기_전에_거절한다() {
		
		// 1. 잘못된 페이징 정보를 설정한다.
		AnalysisLoadResultSearchDto invalidPage = createSearchDto(1L, 0, 50);
		AnalysisLoadResultSearchDto invalidSize = createSearchDto(1L, 1, 101);
		
		// 2. 잘못된 페이징 조건을 검증한다.
		assertThrows(IllegalArgumentException.class,
				() -> service.getAnalysisLoadResults(invalidPage));
		
		assertThrows(IllegalArgumentException.class,
				() -> service.getAnalysisLoadResults(invalidSize));
		
		// 3. DB 조회가 실행되지 않는지 확인한다.
		verifyNoInteractions(modelMapper, loadMapper);
	}
	
	@Test
	void 적재_모델_식별자가_없으면_조회하지_않는다() {
		
		// 1. 적재 모델 식별자가 없는 검색조건을 설정한다.
		AnalysisLoadResultSearchDto searchDto = createSearchDto(null, 1, 50);
		
		// 2. 적재 모델 식별자 검증을 확인한다.
		assertThrows(IllegalArgumentException.class,
				() -> service.getAnalysisLoadResults(searchDto));
		
		// 3. DB 조회가 실행되지 않는지 확인한다.
		verifyNoInteractions(modelMapper, loadMapper);
	}
	
	@Test
	void 조회_범위를_벗어난_페이지는_빈_목록을_반환한다() {
		
		// 1. 적재 모델과 검색조건을 설정한다.
		givenModel("public", TABLE);
		
		AnalysisLoadResultSearchDto searchDto = createSearchDto(1L, 2, 50);
		
		// 2. 대상 테이블 및 전체 건수를 설정한다.
		AnalysisTargetColumnDto id = column("sale_trade_id");
		
		when(modelMapper.findTargetColumnList("public", TABLE))
				.thenReturn(List.of(id));
		
		when(loadMapper.findResultPrimaryKeyColumns("public", TABLE))
				.thenReturn(List.of("sale_trade_id"));
		
		when(loadMapper.countAnalysisLoadResults(TARGET)).thenReturn(10L);
		
		// 3. 적재 결과 목록을 조회한다.
		AnalysisLoadResultPageDto result = service.getAnalysisLoadResults(searchDto);
		
		// 4. 전체 건수와 빈 목록을 검증한다.
		assertEquals(10L, result.getTotalCount());
		assertEquals(2, result.getPage());
		assertEquals(50, result.getSize());
		assertTrue(result.getContent().isEmpty());
		
		verify(loadMapper, never()).findAnalysisLoadResults(
				any(AnalysisLoadResultSearchDto.class),
				anyString(),
				anyString(),
				anyString());
	}
	
	@Test
	void 기본키가_없는_테이블은_조회하지_않는다() {
		
		// 1. 적재 모델과 검색조건을 설정한다.
		givenModel("public", TABLE);
		
		AnalysisLoadResultSearchDto searchDto = createSearchDto(1L, 1, 50);
		
		// 2. 기본키가 없는 대상 테이블 정보를 설정한다.
		when(modelMapper.findTargetColumnList("public", TABLE))
				.thenReturn(List.of(column("price_10k_krw")));
		
		when(loadMapper.findResultPrimaryKeyColumns("public", TABLE))
				.thenReturn(List.of());
		
		// 3. 기본키 검증을 확인한다.
		assertThrows(IllegalArgumentException.class,
				() -> service.getAnalysisLoadResults(searchDto));
		
		// 4. 전체 건수 및 적재 데이터 조회가 실행되지 않는지 확인한다.
		verify(loadMapper, never()).countAnalysisLoadResults(anyString());
		
		verify(loadMapper, never()).findAnalysisLoadResults(
				any(AnalysisLoadResultSearchDto.class),
				anyString(),
				anyString(),
				anyString());
	}
	
	/**
	 * 테스트에 사용할 적재 모델 정보를 설정한다.
	 */
	private void givenModel(String schema, String table) {
		
		AnalysisModelDto model = new AnalysisModelDto();
		model.setAnalysisModelId(1L);
		model.setEnabled("N");
		model.setTargetSchema(schema);
		model.setTargetTable(table);
		
		when(modelMapper.findAnalysisModel(1L)).thenReturn(model);
	}
	
	/**
	 * 테스트에 사용할 검색조건을 생성한다.
	 */
	private AnalysisLoadResultSearchDto createSearchDto(Long analysisModelId, int page, int size) {
		
		AnalysisLoadResultSearchDto searchDto = new AnalysisLoadResultSearchDto();
		searchDto.setAnalysisModelId(analysisModelId);
		searchDto.setPage(page);
		searchDto.setSize(size);
		
		return searchDto;
	}
	
	/**
	 * 테스트에 사용할 대상 컬럼 정보를 생성한다.
	 */
	private AnalysisTargetColumnDto column(String name) {
		
		AnalysisTargetColumnDto column = new AnalysisTargetColumnDto();
		column.setColumnName(name);
		
		return column;
	}
	
}
