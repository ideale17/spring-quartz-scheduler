
package com.kji.scheduler.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * 데이터 적재 모델 등록 및 수정 요청 정보를 담는다.
 */
@Data
public class AnalysisModelRequestDto {
	
	private AnalysisModelDto model;
	private AnalysisModelVersionDto version;
	private List<AnalysisModelFieldDto> fields = new ArrayList<>();
	
}