package com.kji.scheduler.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * External API 실행 정보와 수집한 원본 응답 데이터를 전달한다.
 */
@Data
public class CollectRawDataDto {
	
	private Long rawDataId;
	private String executionId;
	private Long externalApiId;
	private Integer requestSequence = 1;
	private String responseBody;
	private String contentType;
	private LocalDateTime collectedAt;

}
