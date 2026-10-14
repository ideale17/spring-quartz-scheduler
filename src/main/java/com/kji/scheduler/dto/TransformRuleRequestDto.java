package com.kji.scheduler.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * Raw Data 가공 규칙 등록 및 수정 요청 정보를 담는다.
 */
@Data
public class TransformRuleRequestDto {
	
	private TransformRuleDto rule;
	private TransformRuleVersionDto version;
	private List<TransformRuleFieldDto> fields = new ArrayList<>();
	
}