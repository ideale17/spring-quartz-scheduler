package com.kji.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.kji.scheduler.dto.ExternalApiParamDto;

class DynamicParameterResolverTest {
	
	private DynamicParameterResolver dynamicParameterResolver;
	
	private LocalDateTime executionTime;
	
	@BeforeEach
	void setUp() {
		
		// 1. 테스트 대상 객체를 생성한다.
		dynamicParameterResolver = new DynamicParameterResolver();
		
		// 2. 모든 테스트에서 사용할 실행 기준 시간을 고정한다.
		executionTime = LocalDateTime.of(
				2026,
				9,
				9,
				15,
				55,
				30
		);
	}
	
	@Test
	void staticValueResolveTest() {
		
		// 1. 고정값 파라미터를 생성한다.
		ExternalApiParamDto param = createParam(
				"nx",
				"STATIC",
				"60",
				null
		);
		
		// 2. 실제 값을 생성한다.
		String result = dynamicParameterResolver.resolve(
				param,
				executionTime
		);
		
		// 3. 등록된 고정값이 그대로 반환되는지 확인한다.
		assertEquals("60", result);
	}
	
	@Test
	void currentDateResolveTest() {
		
		// 1. 현재 날짜 파라미터를 생성한다.
		ExternalApiParamDto param = createParam(
				"baseDate",
				"CURRENT_DATE",
				null,
				"yyyyMMdd"
		);
		
		// 2. 실제 값을 생성한다.
		String result = dynamicParameterResolver.resolve(
				param,
				executionTime
		);
		
		// 3. 실행 기준 날짜가 지정된 형식으로 변환되는지 확인한다.
		assertEquals("20260909", result);
	}
	
	@Test
	void currentTimeResolveTest() {
		
		// 1. 현재 시간 파라미터를 생성한다.
		ExternalApiParamDto param = createParam(
				"baseTime",
				"CURRENT_TIME",
				null,
				"HHmm"
		);
		
		// 2. 실제 값을 생성한다.
		String result = dynamicParameterResolver.resolve(
				param,
				executionTime
		);
		
		// 3. 실행 기준 시간이 지정된 형식으로 변환되는지 확인한다.
		assertEquals("1555", result);
	}
	
	@Test
	void currentDateTimeResolveTest() {
		
		// 1. 현재 일시 파라미터를 생성한다.
		ExternalApiParamDto param = createParam(
				"requestDateTime",
				"CURRENT_DATETIME",
				null,
				"yyyyMMddHHmmss"
		);
		
		// 2. 실제 값을 생성한다.
		String result = dynamicParameterResolver.resolve(
				param,
				executionTime
		);
		
		// 3. 실행 기준 일시가 지정된 형식으로 변환되는지 확인한다.
		assertEquals("20260909155530", result);
	}
	
	@Test
	void unsupportedValueTypeTest() {
		
		// 1. 지원하지 않는 VALUE_TYPE 파라미터를 생성한다.
		ExternalApiParamDto param = createParam(
				"baseDate",
				"YESTERDAY",
				null,
				"yyyyMMdd"
		);
		
		// 2. 예외가 발생하는지 확인한다.
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> dynamicParameterResolver.resolve(
						param,
						executionTime
				)
		);
		
		// 3. 예외 메시지를 확인한다.
		assertEquals(
				"지원하지 않는 External API 파라미터 VALUE_TYPE입니다. "
						+ "paramName: baseDate, valueType: YESTERDAY",
				exception.getMessage()
		);
	}
	
	@Test
	void invalidValueFormatTest() {
		
		// 1. 잘못된 VALUE_FORMAT 파라미터를 생성한다.
		ExternalApiParamDto param = createParam(
				"baseDate",
				"CURRENT_DATE",
				null,
				"INVALID_FORMAT_[["
		);
		
		// 2. 예외가 발생하는지 확인한다.
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> dynamicParameterResolver.resolve(
						param,
						executionTime
				)
		);
		
		// 3. 잘못된 형식임을 알 수 있는 예외 메시지인지 확인한다.
		assertEquals(
		        "동적 파라미터 날짜/시간 변환에 실패했습니다. "
		                + "paramName: baseDate, valueFormat: INVALID_FORMAT_[[",
		        exception.getMessage()
		);
	}
	
	/**
	 * 날짜·시간 오프셋 계산 결과를 검증한다.
	 *
	 * @param valueType 파라미터 값 유형
	 * @param valueFormat 날짜·시간 포맷
	 * @param offsetUnit 이동 단위
	 * @param offsetValue 이동 값
	 * @param executionTime 실행 기준 시간
	 * @param expectedValue 예상 결과
	 */
	@ParameterizedTest(name = "{index}: {0}, {2}, {3} -> {5}")
	@MethodSource("offsetTestCases")
	void offsetResolveTest(
			String valueType,
			String valueFormat,
			String offsetUnit,
			Integer offsetValue,
			LocalDateTime executionTime,
			String expectedValue) {
		
		// 1. 날짜·시간 오프셋 파라미터를 생성한다.
		ExternalApiParamDto param = createParam("DEAL_YMD", valueType, null, valueFormat);
		param.setOffsetUnit(offsetUnit);
		param.setOffsetValue(offsetValue);
		
		// 2. 실제 API 전달 값을 생성한다.
		String result = dynamicParameterResolver.resolve(param, executionTime);
		
		// 3. 예상한 날짜·시간으로 변환되었는지 확인한다.
		assertEquals(expectedValue, result);
	}
	
	// 날짜·시간 오프셋 계산에 사용할 테스트 데이터를 생성한다.
	private static Stream<Arguments> offsetTestCases() {
		return Stream.of(
				// 전월 데이터를 조회한다.
				Arguments.of("CURRENT_DATE", "yyyyMM", "MONTH", -1,
						LocalDateTime.of(2026, 11, 15, 12, 0), "202610"),
				
				// 7일 전 데이터를 조회한다.
				Arguments.of("CURRENT_DATE", "yyyyMMdd", "DAY", -7,
						LocalDateTime.of(2026, 11, 15, 12, 0), "20261108"),
				
				// 전년도 데이터를 조회한다.
				Arguments.of("CURRENT_DATE", "yyyy", "YEAR", -1,
						LocalDateTime.of(2026, 11, 15, 12, 0), "2025"),
				
				// 2시간 전 데이터를 조회한다.
				Arguments.of("CURRENT_TIME", "HHmm", "HOUR", -2,
						LocalDateTime.of(2026, 11, 15, 12, 0), "1000"),
				
				// 30분 후 데이터를 조회한다.
				Arguments.of("CURRENT_TIME", "HHmm", "MINUTE", 30,
						LocalDateTime.of(2026, 11, 15, 12, 0), "1230"),
				
				// 다음 날 일시를 조회한다.
				Arguments.of("CURRENT_DATETIME", "yyyyMMddHHmm", "DAY", 1,
						LocalDateTime.of(2026, 11, 15, 12, 0), "202611161200"),
				
				// 연도가 변경되는 시점의 전월을 계산한다.
				Arguments.of("CURRENT_DATE", "yyyyMM", "MONTH", -1,
						LocalDateTime.of(2027, 1, 15, 12, 0), "202612"),
				
				// 3월 31일에서 전월을 계산한다.
				Arguments.of("CURRENT_DATE", "yyyyMMdd", "MONTH", -1,
						LocalDateTime.of(2026, 3, 31, 12, 0), "20260228"),
				
				// 오프셋 설정이 없는 기존 파라미터를 확인한다.
				Arguments.of("CURRENT_DATE", "yyyyMM", null, null,
						LocalDateTime.of(2026, 11, 15, 12, 0), "202611"),
				
				// 이동 값이 0이면 실행 기준 날짜를 유지한다.
				Arguments.of("CURRENT_DATE", "yyyyMM", "MONTH", 0,
						LocalDateTime.of(2026, 11, 15, 12, 0), "202611")
		);
	}
	
	@Test
	void staticOffsetIgnoredTest() {
		
		// 1. STATIC 파라미터에 오프셋을 설정한다.
		ExternalApiParamDto param = createParam("LAWD_CD", "STATIC", "11500", null);
		param.setOffsetUnit("MONTH");
		param.setOffsetValue(-1);
		
		// 2. 실제 값을 생성한다.
		String result = dynamicParameterResolver.resolve(param, executionTime);
		
		// 3. 오프셋과 관계없이 고정값이 반환되는지 확인한다.
		assertEquals("11500", result);
	}
	
	// 테스트용 External API 파라미터를 생성한다.
	private ExternalApiParamDto createParam(
			String paramName,
			String valueType,
			String paramValue,
			String valueFormat) {
		
		ExternalApiParamDto param = new ExternalApiParamDto();
		
		param.setParamName(paramName);
		param.setValueType(valueType);
		param.setParamValue(paramValue);
		param.setValueFormat(valueFormat);
		
		return param;
	}
	
}