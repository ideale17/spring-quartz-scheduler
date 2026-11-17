
package com.kji.scheduler.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * 데이터 적재 모델의 필드 매핑 설정 정보를 담는다.
 */
@Data
public class AnalysisModelFieldDto {
	
	private Long analysisFieldId;
	private Long analysisModelVersionId;
	private String targetColumn;
	private List<String> sourceFields = new ArrayList<>();
	private String convertType;
	private String convertFormat;
	private String requiredYn;
	private Integer sortOrder;
	
}