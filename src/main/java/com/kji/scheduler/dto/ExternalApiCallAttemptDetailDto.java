
package com.kji.scheduler.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

/**
 * External API 호출 시도 상세 정보와 실제 요청 정보를 담는다.
 */
@Data
public class ExternalApiCallAttemptDetailDto {
	
	private Long apiCallLogId;
	private Long requestId;
	private String executionId;
	
	private Integer requestSequence;
	private Integer pageNo;
	private Integer attemptNo;
	
	private String requestMethod;
	private String requestUrl;
	
	private Integer httpStatus;
	private String status;
	private LocalDateTime startedAt;
	private LocalDateTime finishedAt;
	private Long runMillis;
	private String errorMessage;
	
	private Long rawDataId;
	
	private List<ExternalApiRequestParamLogDto> params;
	
}
