package com.kji.scheduler.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * 데이터 적재 작업 등록 요청 정보.
 */
@Data
public class AnalysisLoadRequestDto {
	
	private Long analysisModelId;
	private List<Long> transformResultIds = new ArrayList<>();
	
}