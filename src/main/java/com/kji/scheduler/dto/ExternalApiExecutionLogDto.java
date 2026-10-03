package com.kji.scheduler.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 외부 API 1회 실행 단위의 전체 실행 이력을 관리하는 DTO.
 *
 * @author kji
 * @since 2026. 10. 3.
 */
@Data
public class ExternalApiExecutionLogDto {

	private String executionId;				// External API 1회 실행 단위 식별자
	private Long externalApiId;				// External API 식별자
	private String fireInstanceId;			// Quartz 실행 인스턴스 식별자
	private LocalDateTime startedAt;		// External API 실행 시작 일시
	private LocalDateTime finishedAt;		// External API 실행 종료 일시
	private Long runMillis;					// External API 전체 실행 소요 시간(ms)
	private String status;					// External API 실행 상태(RUNNING, SUCCESS, FAILED)
	private String errorMessage;			// External API 실행 실패 시 오류 메시지
	private LocalDateTime createdAt;		// 로그 생성 일시
	
}