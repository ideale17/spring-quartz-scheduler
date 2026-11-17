package com.kji.scheduler.dto;

import lombok.Data;

/**
 * 데이터 적재 모델의 필드 매핑 DB 조회 결과를 담는다.
 *
 * SOURCE_FIELDS JSONB 컬럼은 문자열로 조회한 후
 * Service에서 List<String>으로 변환한다.
 */
@Data
public class AnalysisModelFieldRowDto {
	
	private Long analysisFieldId;
	private Long analysisModelVersionId;
	private String targetColumn;
	private String sourceFieldsJson;
	private String convertType;
	private String convertFormat;
	private String requiredYn;
	private Integer sortOrder;
	
}