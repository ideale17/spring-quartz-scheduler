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
			
			// 2. 등록된 형식으로 실행 기준 시간을 변환한다.
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(valueFormat);
			
			return executionTime.format(formatter);
			
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("잘못된 동적 파라미터 VALUE_FORMAT입니다. " + "paramName: " + param.getParamName() + ", valueFormat: " + valueFormat, e);
		}
	}
	
}