package com.kji.scheduler.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * External API 페이징 호출에 필요한 설정 정보를 담는다.
 */
@Data
public class ExternalApiPagingDto {
	
	private Long externalApiId;
	
	private String enabled;
	
	private String paginationType;
	private String terminationType;
	
	private String pageParamLocation;
	private String pageParamName;
	private Integer pageStart;
	
	private String sizeParamLocation;
	private String sizeParamName;
	private Integer pageSize;
	
	private String totalCountPath;
	
	private Integer maxRequestCount;
	
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	
}
