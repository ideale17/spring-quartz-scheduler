
package com.kji.scheduler.dto;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;

/**
 * 적재 모델 정보와 실제 적재 결과 목록을 함께 반환한다.
 */
@Data
public class AnalysisLoadResultPageDto {
	
	private AnalysisModelDto model;
	private List<AnalysisTargetColumnDto> columns;
	private List<JsonNode> content;
	private long totalCount;
	private int page;
	private int size;
	
	public AnalysisLoadResultPageDto(
			AnalysisModelDto model,
			List<AnalysisTargetColumnDto> columns,
			List<JsonNode> content,
			long totalCount,
			int page,
			int size) {
		
		this.model = model;
		this.columns = columns;
		this.content = content;
		this.totalCount = totalCount;
		this.page = page;
		this.size = size;
	}
	
}