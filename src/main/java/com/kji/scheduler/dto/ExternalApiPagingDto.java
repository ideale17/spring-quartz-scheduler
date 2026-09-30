package com.kji.scheduler.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * ExternalApiPagingDto 클래스.
 *
 * @author kji
 * @since 2026. 9. 30.
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