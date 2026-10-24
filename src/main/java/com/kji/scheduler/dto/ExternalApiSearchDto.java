
package com.kji.scheduler.dto;

import lombok.Data;

/**
 * External API 목록 검색조건을 담는다.
 */
@Data
public class ExternalApiSearchDto {
	
	private String apiName;
	private String httpMethod;
	private String enabled;
	
}
