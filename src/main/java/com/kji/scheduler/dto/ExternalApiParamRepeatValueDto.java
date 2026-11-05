
package com.kji.scheduler.dto;

import lombok.Data;

/**
 * External API 파라미터의 반복 목록 값을 담는다.
 */
@Data
public class ExternalApiParamRepeatValueDto {
	
	private Long repeatValueId;
	private Long paramId;
	private String paramValue;
	private Integer sortOrder;
	
}
