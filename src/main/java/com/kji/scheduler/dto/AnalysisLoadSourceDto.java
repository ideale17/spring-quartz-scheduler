package com.kji.scheduler.dto;

import lombok.Data;

/**
 * 데이터 적재 작업 실행에 필요한 정보를 담는다.
 */
@Data
public class AnalysisLoadSourceDto {
	
	private Long analysisLoadSourceId;
	private Long analysisModelId;
	private Long analysisModelVersionId;
	private Long transformResultId;
	private String status;
	private String targetSchema;
	private String targetTable;
	private String resultData;
	
}