
package com.kji.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.kji.scheduler.dto.ExternalApiParamDto;
import com.kji.scheduler.dto.ExternalApiParamRepeatValueDto;
import com.kji.scheduler.dto.ExternalApiParameterCombinationDto;

class ExternalApiParameterCombinationServiceTest {
	
	private ExternalApiParameterCombinationService combinationService;
	
	private LocalDateTime executionTime;
	
	@BeforeEach
	void setUp() {
		
		// 1. 기존 동적 파라미터 계산기를 생성한다.
		DynamicParameterResolver resolver = new DynamicParameterResolver();
		
		// 2. 반복 조합 생성 Service를 생성한다.
		combinationService = new ExternalApiParameterCombinationService(resolver);
		
		// 3. 실행 기준 시간을 고정한다.
		executionTime = LocalDateTime.of(2026, 11, 15, 12, 0);
	}
	
	@Test
	void noneCombinationTest() {
		
		// 1. 기존 고정값 파라미터를 생성한다.
		ExternalApiParamDto param = createStaticParam("LAWD_CD", "11680");
		
		// 2. 요청 조합을 생성한다.
		List<ExternalApiParameterCombinationDto> results =
				combinationService.createCombinations(List.of(param), executionTime);
		
		// 3. 기존과 동일하게 조합 한 개가 생성되는지 확인한다.
		assertEquals(1, results.size());
		assertEquals("11680", results.get(0).getQueryParams().get("LAWD_CD"));
	}
	
	@Test
	void listCombinationTest() {
		
		// 1. 지역코드 목록 반복 파라미터를 생성한다.
		ExternalApiParamDto param = createListParam(
				"LAWD_CD", "11680", "11110", "41370"
		);
		
		// 2. 요청 조합을 생성한다.
		List<ExternalApiParameterCombinationDto> results =
				combinationService.createCombinations(List.of(param), executionTime);
		
		// 3. 등록 순서대로 세 개의 조합이 생성되는지 확인한다.
		assertEquals(3, results.size());
		assertEquals("11680", results.get(0).getQueryParams().get("LAWD_CD"));
		assertEquals("11110", results.get(1).getQueryParams().get("LAWD_CD"));
		assertEquals("41370", results.get(2).getQueryParams().get("LAWD_CD"));
	}
	
	@Test
	void rangeCombinationTest() {
		
		// 1. 전월 기준 최근 3개월 반복 파라미터를 생성한다.
		ExternalApiParamDto param = createRangeParam("DEAL_YMD", -2, 0);
		param.setOffsetUnit("MONTH");
		param.setOffsetValue(-1);
		
		// 2. 날짜 조합을 생성한다.
		List<ExternalApiParameterCombinationDto> results =
				combinationService.createCombinations(List.of(param), executionTime);
		
		// 3. 8월부터 10월까지 생성되는지 확인한다.
		assertEquals(3, results.size());
		assertEquals("202608", results.get(0).getQueryParams().get("DEAL_YMD"));
		assertEquals("202609", results.get(1).getQueryParams().get("DEAL_YMD"));
		assertEquals("202610", results.get(2).getQueryParams().get("DEAL_YMD"));
	}
	
	@Test
	void listAndRangeCombinationTest() {
		
		// 1. 지역코드 두 개와 거래월 두 개를 설정한다.
		ExternalApiParamDto region = createListParam("LAWD_CD", "11680", "11110");
		ExternalApiParamDto month = createRangeParam("DEAL_YMD", -1, 0);
		ExternalApiParamDto serviceKey = createStaticParam("serviceKey", "TEST_KEY");
		
		// 2. 전체 요청 조합을 생성한다.
		List<ExternalApiParameterCombinationDto> results =
				combinationService.createCombinations(
						List.of(region, month, serviceKey), executionTime
				);
		
		// 3. 지역 2개와 거래월 2개로 총 4개가 생성되는지 확인한다.
		assertEquals(4, results.size());
		
		// 4. 조합별 파라미터 값과 순서를 확인한다.
		assertCombination(results.get(0), "11680", "202610");
		assertCombination(results.get(1), "11680", "202611");
		assertCombination(results.get(2), "11110", "202610");
		assertCombination(results.get(3), "11110", "202611");
		
		// 5. 반복하지 않는 인증 파라미터가 모든 조합에 포함되는지 확인한다.
		for (ExternalApiParameterCombinationDto result : results) {
			assertEquals("TEST_KEY", result.getQueryParams().get("serviceKey"));
		}
	}
	
	@Test
	void maxCombinationCountTest() {
		
		// 1. 지역코드 11개를 생성한다.
		String[] regions = new String[11];
		
		for (int i = 0; i < regions.length; i++) {
			regions[i] = "REGION_" + i;
		}
		
		ExternalApiParamDto region = createListParam("LAWD_CD", regions);
		
		// 2. 거래월 10개를 생성한다.
		ExternalApiParamDto month = createRangeParam("DEAL_YMD", -9, 0);
		
		// 3. 총 110개 조합을 요청한다.
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> combinationService.createCombinations(
						List.of(region, month), executionTime
				)
		);
		
		// 4. 최대 조합 수 제한 메시지를 확인한다.
		assertEquals(
				"External API 반복 조합은 최대 100개까지 생성할 수 있습니다.",
				exception.getMessage()
		);
	}
	
	@Test
	void invalidRepeatConfigurationTest() {
		
		// 1. 반복 목록이 비어 있는 파라미터를 생성한다.
		ExternalApiParamDto listParam = createStaticParam("LAWD_CD", null);
		listParam.setRepeatType("LIST");
		listParam.setRepeatValues(new ArrayList<>());
		
		// 2. 빈 반복 목록에 대한 예외가 발생하는지 확인한다.
		assertThrows(
				IllegalArgumentException.class,
				() -> combinationService.createCombinations(
						List.of(listParam), executionTime
				)
		);
		
		// 3. 시작값이 종료값보다 큰 날짜 범위를 생성한다.
		ExternalApiParamDto rangeParam = createRangeParam("DEAL_YMD", 0, -2);
		
		// 4. 잘못된 날짜 범위에 대한 예외가 발생하는지 확인한다.
		assertThrows(
				IllegalArgumentException.class,
				() -> combinationService.createCombinations(
						List.of(rangeParam), executionTime
				)
		);
	}
	
	// 고정값 파라미터를 생성한다.
	private ExternalApiParamDto createStaticParam(String name, String value) {
		
		ExternalApiParamDto param = new ExternalApiParamDto();
		param.setParamLocation("QUERY");
		param.setParamName(name);
		param.setValueType("STATIC");
		param.setParamValue(value);
		param.setRepeatType("NONE");
		param.setRequiredYn("Y");
		
		return param;
	}
	
	// 목록 반복 파라미터를 생성한다.
	private ExternalApiParamDto createListParam(String name, String... values) {
		
		ExternalApiParamDto param = createStaticParam(name, null);
		param.setRepeatType("LIST");
		
		List<ExternalApiParamRepeatValueDto> repeatValues = new ArrayList<>();
		
		for (int i = 0; i < values.length; i++) {
			
			ExternalApiParamRepeatValueDto repeatValue = new ExternalApiParamRepeatValueDto();
			repeatValue.setParamValue(values[i]);
			repeatValue.setSortOrder(i + 1);
			
			repeatValues.add(repeatValue);
		}
		
		param.setRepeatValues(repeatValues);
		
		return param;
	}
	
	// 날짜 범위 반복 파라미터를 생성한다.
	private ExternalApiParamDto createRangeParam(String name, int start, int end) {
		
		ExternalApiParamDto param = new ExternalApiParamDto();
		param.setParamLocation("QUERY");
		param.setParamName(name);
		param.setValueType("CURRENT_DATE");
		param.setValueFormat("yyyyMM");
		param.setRepeatType("RANGE");
		param.setRepeatUnit("MONTH");
		param.setRepeatStart(start);
		param.setRepeatEnd(end);
		param.setRepeatStep(1);
		param.setRequiredYn("Y");
		
		return param;
	}
	
	// 생성된 요청 조합의 지역코드와 거래월을 확인한다.
	private void assertCombination(
			ExternalApiParameterCombinationDto combination,
			String region, String month) {
		
		Map<String, String> queryParams = combination.getQueryParams();
		
		assertEquals(region, queryParams.get("LAWD_CD"));
		assertEquals(month, queryParams.get("DEAL_YMD"));
	}
	
}
