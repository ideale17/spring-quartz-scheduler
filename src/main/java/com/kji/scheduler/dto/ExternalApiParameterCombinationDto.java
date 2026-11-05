
package com.kji.scheduler.dto;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Data;

/**
 * External API 반복 실행에 사용할 파라미터 조합을 담는다.
 */
@Data
public class ExternalApiParameterCombinationDto {
	
	private Map<String, String> headers = new LinkedHashMap<>();
	private Map<String, String> queryParams = new LinkedHashMap<>();
	private Map<String, String> bodyParams = new LinkedHashMap<>();
	
}
