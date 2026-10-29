
package com.kji.scheduler.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.kji.scheduler.dto.ExternalApiParamDto;

@Component
public class DynamicParameterResolver {
	
	// External API 파라미터의 실제 실행 값을 생성한다.
	public String resolve(ExternalApiParamDto param, LocalDateTime executionTime) {
		
		// 1. 파라미터 정보가 없으면 예외를 발생시킨다.
		if (param == null) {
			throw new IllegalArgumentException("External API 파라미터 정보가 없습니다.");
		}
		
		// 2. 실행 기준 시간이 없으면 예외를 발생시킨다.
		if (executionTime == null) {
			throw new IllegalArgumentException("동적 파라미터 실행 기준 시간이 없습니다.");
		}
		
		// 3. 값 생성 방식을 확인한다.
		String valueType = param.getValueType();
		
		if (valueType == null || valueType.isBlank()) {
			throw new IllegalArgumentException("External API 파라미터 VALUE_TYPE이 없습니다. paramName: " + param.getParamName());
		}
		
		// 4. VALUE_TYPE에 따라 실제 값을 생성한다.
		return switch (valueType) {
		
			case "STATIC" -> param.getParamValue();
			
			case "CURRENT_DATE" -> formatDateTime(param, executionTime);
			
			case "CURRENT_TIME" -> formatDateTime(param, executionTime);
			
			case "CURRENT_DATETIME" -> formatDateTime(param, executionTime);
			
			default -> throw new IllegalArgumentException("지원하지 않는 External API 파라미터 VALUE_TYPE입니다. " + "paramName: " + param.getParamName() + ", valueType: " + valueType);
		};
	}
	
	// 날짜/시간 동적 파라미터를 지정된 형식으로 변환한다.
	private String formatDateTime(ExternalApiParamDto param, LocalDateTime executionTime) {
		
		// 1. 날짜/시간 형식이 없으면 예외를 발생시킨다.
		String valueFormat = param.getValueFormat();
		
		if (valueFormat == null || valueFormat.isBlank()) {
			throw new IllegalArgumentException("동적 파라미터 VALUE_FORMAT이 없습니다. paramName: " + param.getParamName());
		}
		
		try {
			
			// 2. 설정된 오프셋을 실행 기준 시간에 적용한다.
			LocalDateTime targetTime = applyOffset(param, executionTime);
			
			// 3. 등록된 형식으로 날짜와 시간을 변환한다.
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(valueFormat);
			
			return targetTime.format(formatter);
			
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("동적 파라미터 날짜/시간 변환에 실패했습니다. "
					+ "paramName: " + param.getParamName() + ", valueFormat: " + valueFormat, e);
		}
	}
	
	/**
	 * 설정된 오프셋에 따라 실행 기준 날짜와 시간을 이동한다.
	 *
	 * @param param 파라미터 설정 정보
	 * @param executionTime 실행 기준 시간
	 * @return 오프셋이 적용된 날짜와 시간
	 */
	private LocalDateTime applyOffset(ExternalApiParamDto param, LocalDateTime executionTime) {
		
		// 1. 이동 단위가 설정되지 않았으면 기존 실행 기준 시간을 반환한다.
		String offsetUnit = param.getOffsetUnit();
		
		if (offsetUnit == null || offsetUnit.isBlank()) {
			return executionTime;
		}
		
		// 2. 이동 값을 확인한다. 설정되지 않은 경우 0을 사용한다.
		int offsetValue = param.getOffsetValue() == null ? 0 : param.getOffsetValue();
		
		// 3. 이동 단위에 따라 실행 기준 날짜와 시간을 계산한다.
		return switch (offsetUnit) {
		
			case "MINUTE" -> executionTime.plusMinutes(offsetValue);
			
			case "HOUR" -> executionTime.plusHours(offsetValue);
			
			case "DAY" -> executionTime.plusDays(offsetValue);
			
			case "MONTH" -> executionTime.plusMonths(offsetValue);
			
			case "YEAR" -> executionTime.plusYears(offsetValue);
			
			default -> throw new IllegalArgumentException("지원하지 않는 날짜/시간 오프셋 단위입니다. "
					+ "paramName: " + param.getParamName() + ", offsetUnit: " + offsetUnit);
		};
	}
	
}