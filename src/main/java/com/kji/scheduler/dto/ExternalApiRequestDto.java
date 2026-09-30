package com.kji.scheduler.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ExternalApiRequestDto {
	
	private ExternalApiDto externalApi;
	private List<ExternalApiParamDto> params = new ArrayList<>();
	private ExternalApiPagingDto paging;
	
}