
package com.kji.scheduler.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * External API 최초 실행 당시 설정 스냅샷을 담는다.
 */
@Data
public class ExternalApiExecutionSnapshotDto {
	
	private String executionId;
	private Integer snapshotVersion;
	private LocalDateTime executionBaseTime;
	private String snapshotData;
	private LocalDateTime createdAt;
	
}
