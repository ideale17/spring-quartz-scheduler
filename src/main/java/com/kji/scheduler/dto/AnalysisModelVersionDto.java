
package com.kji.scheduler.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 데이터 적재 모델의 버전별 설정 정보를 담는다.
 */
@Data
public class AnalysisModelVersionDto {
	
	private Long analysisModelVersionId;
	private Long analysisModelId;
	private Integer versionNo;
	private Long transformRuleVersionId;
	private LocalDateTime createdAt;
	
}