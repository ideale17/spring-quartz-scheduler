
package com.kji.scheduler.dto;

import java.util.List;

import lombok.Data;

/**
 * 수동 가공 작업 일괄 등록 요청 정보를 담는다.
 */
@Data
public class TransformTaskRequestDto {
	
	private Long transformRuleId;
	private List<Long> rawDataIds;
	
}
