
package com.kji.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import com.kji.scheduler.crypto.AesGcmEncryptionService;
import com.kji.scheduler.dto.ExternalApiParamDto;
import com.kji.scheduler.mapper.ExternalApiMapper;

/**
 * External API 파라미터의 날짜·시간 오프셋 설정 검증을 테스트한다.
 */
class ExternalApiServiceOffsetTest {
	
	private ExternalApiService externalApiService;
	
	@BeforeEach
	void setUp() {
		
		// 1. DB 및 Quartz 의존성을 Mock으로 생성한다.
		externalApiService = new ExternalApiService(
				mock(ExternalApiMapper.class),
				mock(DynamicJobService.class),
				mock(AesGcmEncryptionService.class));
	}
	
	@ParameterizedTest(name = "정상 {index}: {0}, {1}, {2}")
	@MethodSource("validOffsets")
	void validOffsetTest(String valueType, String offsetUnit, Integer offsetValue) {
		
		// 1. 정상적인 오프셋 파라미터를 생성한다.
		ExternalApiParamDto param = createParam(valueType, offsetUnit, offsetValue);
		
		// 2. 오프셋 검증을 실행한다.
		validateOffset(param);
		
		// 3. 정상 설정이 유지되는지 확인한다.
		assertEquals(offsetUnit, param.getOffsetUnit());
		assertEquals(offsetValue == null ? 0 : offsetValue, param.getOffsetValue());
	}
	
	private static Stream<Arguments> validOffsets() {
		return Stream.of(
				Arguments.of("CURRENT_DATE", "MONTH", -1),
				Arguments.of("CURRENT_DATE", "DAY", -7),
				Arguments.of("CURRENT_TIME", "HOUR", 2),
				Arguments.of("CURRENT_DATETIME", "YEAR", 1),
				Arguments.of("CURRENT_DATE", null, 0));
	}
	
	@ParameterizedTest(name = "오류 {index}: {0}, {1}, {2}")
	@MethodSource("invalidOffsets")
	void invalidOffsetTest(String valueType, String offsetUnit, Integer offsetValue, String expectedMessage) {
		
		// 1. 잘못된 오프셋 파라미터를 생성한다.
		ExternalApiParamDto param = createParam(valueType, offsetUnit, offsetValue);
		
		// 2. 오프셋 검증 시 예외가 발생하는지 확인한다.
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> validateOffset(param));
		
		// 3. 오류 원인이 예외 메시지에 포함되는지 확인한다.
		assertTrue(exception.getMessage().contains(expectedMessage));
	}
	
	private static Stream<Arguments> invalidOffsets() {
		return Stream.of(
				Arguments.of("CURRENT_DATE", "HOUR", -1, "사용할 수 없는"),
				Arguments.of("CURRENT_TIME", "MONTH", -1, "사용할 수 없는"),
				Arguments.of("CURRENT_DATE", "MONTH", null, "이동 값을 입력"),
				Arguments.of("CURRENT_DATE", null, -1, "이동 단위도 필요"),
				Arguments.of("CURRENT_DATE", "YEAR", 10001, "-10000부터 10000까지"));
	}
	
	@Test
	void staticOffsetResetTest() {
		
		// 1. STATIC 파라미터에 잘못 설정된 오프셋을 준비한다.
		ExternalApiParamDto param = createParam("STATIC", "MONTH", -1);
		
		// 2. 오프셋 검증을 실행한다.
		validateOffset(param);
		
		// 3. 사용하지 않는 오프셋이 초기화되는지 확인한다.
		assertNull(param.getOffsetUnit());
		assertEquals(0, param.getOffsetValue());
	}
	
	// 테스트용 파라미터를 생성한다.
	private ExternalApiParamDto createParam(String valueType, String offsetUnit, Integer offsetValue) {
		
		ExternalApiParamDto param = new ExternalApiParamDto();
		param.setParamName("DEAL_YMD");
		param.setValueType(valueType);
		param.setOffsetUnit(offsetUnit);
		param.setOffsetValue(offsetValue);
		
		return param;
	}
	
	// 서비스 내부 오프셋 검증 메서드를 호출한다.
	private void validateOffset(ExternalApiParamDto param) {
		ReflectionTestUtils.invokeMethod(externalApiService, "validateExternalApiParamOffset", param);
	}
}