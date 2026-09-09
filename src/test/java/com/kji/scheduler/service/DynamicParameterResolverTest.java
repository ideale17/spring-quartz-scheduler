package com.kji.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
				"잘못된 동적 파라미터 VALUE_FORMAT입니다. "
						+ "paramName: baseDate, valueFormat: INVALID_FORMAT_[[",
				exception.getMessage()
		);
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