
package com.kji.scheduler.dto;

import lombok.Data;

/**
 * 가공 규칙 목록 검색조건을 담는다.
 */
@Data
public class TransformRuleSearchDto {
	
	private String ruleName;
	private String enabled;
	
}
