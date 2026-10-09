package com.kji.scheduler.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * External API 등록 요청에 필요한 정보를 담는다.
 */
@Data
public class ExternalApiRequestDto {
	
	private ExternalApiDto externalApi;
	private List<ExternalApiParamDto> params = new ArrayList<>();
	private ExternalApiPagingDto paging;
	
}
