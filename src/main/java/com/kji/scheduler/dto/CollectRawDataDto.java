package com.kji.scheduler.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * External API를 통해 수집한 원본 응답 데이터를 저장한다.
 *
 * @author kji
 * @since 2026. 9. 29.
 */
@Data
public class CollectRawDataDto {
	
	private Long rawDataId;
	private String executionId;
	private Long externalApiId;
	private String responseBody;
	private String contentType;
	private LocalDateTime collectedAt;

}