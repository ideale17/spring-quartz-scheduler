package com.kji.scheduler.dto;

import java.util.List;

import lombok.Data;

/**
 * 적재 모델 정보와 적재 대상 목록을 함께 반환한다.
 */
@Data
public class AnalysisLoadPageDto {
	
	private AnalysisModelDto model;
	private AnalysisModelVersionDto version;
	private Integer transformVersionNo;
	private List<AnalysisLoadListDto> content;
	private long totalCount;
	private int page;
	private int size;
	
	public AnalysisLoadPageDto(
			AnalysisModelDto model,
			AnalysisModelVersionDto version,
			Integer transformVersionNo,
			List<AnalysisLoadListDto> content,
			long totalCount,
			int page,
			int size) {
		
		this.model = model;
		this.version = version;
		this.transformVersionNo = transformVersionNo;
		this.content = content;
		this.totalCount = totalCount;
		this.page = page;
		this.size = size;
	}
	
}