package com.kji.scheduler.dto;

import lombok.Data;

/**
 * External API 기본 정보 수정에 필요한 데이터를 담는다.
 */
@Data
public class ExternalApiBasicDto {
	
	private String apiName;
	private String apiUrl;
	private String httpMethod;
	private String enabled;
	
	private String retryEnabled;
	private Integer maxRetryCount;
	private Integer retryIntervalSec;
	
	private String description;
	
}
