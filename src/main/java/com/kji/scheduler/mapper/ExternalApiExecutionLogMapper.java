package com.kji.scheduler.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.kji.scheduler.dto.ExternalApiCallHistorySearchDto;
import com.kji.scheduler.dto.ExternalApiCallHistorySummaryDto;
import com.kji.scheduler.dto.ExternalApiExecutionLogDto;

@Mapper
public interface ExternalApiExecutionLogMapper {
	
	/**
	 * External API 실행 시작 정보를 저장한다.
	 *
	 * @param externalApiExecutionLogDto 저장할 External API 실행 이력 정보
	 */
	void insertStart(ExternalApiExecutionLogDto externalApiExecutionLogDto);
	
	/**
	 * 실행 식별자를 조건으로 종료 시간, 소요 시간, 실행 상태와 오류 메시지를 수정한다.
	 *
	 * @param externalApiExecutionLogDto 실행 식별자를 포함한 실행 종료 정보
	 */
	void updateFinish(ExternalApiExecutionLogDto externalApiExecutionLogDto);
	
	/**
	 * 검색 조건과 페이징 정보로 External API 실행 이력과 호출 집계 정보를 조회한다.
	 *
	 * @param searchDto External API 실행 이력 검색 조건과 페이징 정보
	 * @return 호출 집계 정보를 포함한 실행 이력 목록
	 */
	List<ExternalApiCallHistorySummaryDto> findExecutionHistory(ExternalApiCallHistorySearchDto searchDto);

	/**
	 * 검색 조건에 맞는 External API 실행 이력의 전체 건수를 조회한다.
	 *
	 * @param searchDto External API 실행 이력 검색 조건
	 * @return 검색 조건에 맞는 전체 실행 이력 건수
	 */
	long countExecutionHistory(ExternalApiCallHistorySearchDto searchDto);
	
}
