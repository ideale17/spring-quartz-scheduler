
package com.kji.scheduler.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

/**
 * RESTART 실행에 필요한 원본 설정을 담는다.
 */
@Data
public class ExternalApiRestartConfigDto {
	
	private ExternalApiDto externalApi;
	private List<ExternalApiParamDto> params;
	private ExternalApiPagingDto paging;
	private LocalDateTime executionBaseTime;
	
}
