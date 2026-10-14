package com.kji.scheduler.dto;

import lombok.Data;

/**
 * Raw Data 가공 규칙의 추출 필드 설정 정보를 담는다.
 */
@Data
public class TransformRuleFieldDto {
	
	private Long transformFieldId;
	private Long transformRuleVersionId;
	private String sourcePath;
	private String targetName;
	private String dataType;
	private String valueFormat;
	private String requiredYn;
	private Integer sortOrder;
	
}