
package com.kji.scheduler.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * External API 자동 가공 규칙 연결 설정을 담는다.
 */
@Data
public class TransformAutoConfigDto {
	
	private Long autoConfigId;
	private Long externalApiId;
	private Long transformRuleId;
	private Long transformRuleVersionId;
	private String ruleName;
	private Integer currentVersion;
	private String ruleEnabled;
	private String enabled;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	
}
