package com.kji.scheduler.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 가공 결과 상세 정보를 담는다.
 */
@Data
public class TransformResultDetailDto {
	
	private Long transformResultId;
	private Long transformTaskId;
	private Long rawDataId;
	private Long externalApiId;
	private String apiName;
	private Long transformRuleId;
	private String ruleName;
	private Integer versionNo;
	private String dataFormat;
	private String resultData;
	private LocalDateTime createdAt;
	
}