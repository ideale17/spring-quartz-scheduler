
package com.kji.scheduler.dto;

import java.util.List;

import lombok.Data;

/**
 * External API 자동 가공 규칙 연결 저장 요청을 담는다.
 */
@Data
public class TransformAutoConfigRequestDto {
	
	private List<TransformAutoConfigDto> configs;
	
}
