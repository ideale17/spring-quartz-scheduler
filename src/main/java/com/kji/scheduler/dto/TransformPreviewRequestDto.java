
package com.kji.scheduler.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * Raw Data 가공 미리보기 요청 정보를 담는다.
 */
@Data
public class TransformPreviewRequestDto {
	
	private String rawData;
	private TransformRuleVersionDto version;
	private List<TransformRuleFieldDto> fields = new ArrayList<>();
	
}
