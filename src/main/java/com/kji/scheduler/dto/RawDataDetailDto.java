package com.kji.scheduler.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class RawDataDetailDto {
	
	private Long rawDataId;
	private String executionId;
	private Long externalApiId;
	private String apiName;
	private Integer requestSequence;
	private String responseBody;
	private String contentType;
	private LocalDateTime collectedAt;
	
}