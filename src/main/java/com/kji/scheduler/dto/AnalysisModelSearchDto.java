
package com.kji.scheduler.dto;

import lombok.Data;

/**
 * 데이터 적재 모델 검색조건을 담는다.
 */
@Data
public class AnalysisModelSearchDto {
	
	private String modelName;
	private String enabled;
	
}
