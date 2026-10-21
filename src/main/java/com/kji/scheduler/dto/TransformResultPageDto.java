package com.kji.scheduler.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 가공 결과 목록과 페이징 정보를 담는다.
 */
@Data
@AllArgsConstructor
public class TransformResultPageDto {
	
	private List<TransformResultListDto> content;
	private long totalCount;
	private int page;
	private int size;
	
}