
package com.kji.scheduler.dto;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * External API 실행 당시 논리적 HTTP 요청 정보를 담는다.
 */
@Data
public class ExternalApiRequestLogDto {
	
	private Long requestId;
	private String executionId;
	private Integer requestSequence;
	private Integer pageNo;
	private String requestMethod;
	private String requestUrl;
	private LocalDateTime createdAt;
	
}
