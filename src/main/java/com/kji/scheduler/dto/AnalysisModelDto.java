
package com.kji.scheduler.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 데이터 적재 모델의 기본 정보를 담는다.
 */
@Data
public class AnalysisModelDto {
	
	private Long analysisModelId;
	private String modelName;
	private String description;
	private String targetSchema;
	private String targetTable;
	private String enabled;
	private Integer currentVersion;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	
}
