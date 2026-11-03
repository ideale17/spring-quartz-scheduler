
package com.kji.scheduler.dto;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * External API 실행 당시 실제 요청 파라미터 정보를 담는다.
 */
@Data
public class ExternalApiRequestParamLogDto {
	
	private Long requestParamId;
	private Long requestId;
	private String paramLocation;
	private String paramName;
	private String paramValue;
	private LocalDateTime createdAt;
	
}
