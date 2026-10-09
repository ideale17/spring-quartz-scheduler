package com.kji.scheduler.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kji.scheduler.dto.ExternalApiCallHistoryDto;
import com.kji.scheduler.dto.ExternalApiCallHistorySearchDto;
import com.kji.scheduler.dto.ExternalApiCallHistorySummaryDto;
import com.kji.scheduler.dto.ExternalApiCallLogDto;

@Mapper
public interface ExternalApiCallLogMapper {
	
	/**
	 * External API 호출 시작 정보를 저장한다.
	 *
	 * @param externalApiCallLogDto 저장할 External API 호출 이력 정보
	 * @return 저장된 행 수
	 */
	int insertStart(ExternalApiCallLogDto externalApiCallLogDto);
	
	/**
	 * 호출 이력 식별자를 조건으로 HTTP 상태, 종료 시간, 소요 시간, 호출 상태와 오류 메시지를 수정한다.
	 *
	 * @param externalApiCallLogDto 호출 이력 식별자를 포함한 호출 종료 정보
	 * @return 수정된 행 수
	 */
	int updateFinish(ExternalApiCallLogDto externalApiCallLogDto);
	
	/**
	 * 검색 조건과 페이징 정보로 External API 호출 이력을 조회한다.
	 *
	 * @param searchDto External API 호출 이력 검색 조건과 페이징 정보
	 * @return 호출 이력 목록
	 */
	List<ExternalApiCallHistoryDto> findCallHistory(ExternalApiCallHistorySearchDto searchDto);

	/**
	 * 검색 조건에 맞는 External API 호출 이력의 전체 건수를 조회한다.
	 *
	 * @param searchDto External API 호출 이력 검색 조건
	 * @return 검색 조건에 맞는 전체 호출 이력 건수
	 */
	long countCallHistory(ExternalApiCallHistorySearchDto searchDto);
	
	/**
	 * 검색 조건과 페이징 정보로 실행별 호출 집계와 마지막 호출 상태를 조회한다.
	 *
	 * @param searchDto External API 호출 이력 검색 조건과 페이징 정보
	 * @return 실행별 호출 집계와 마지막 호출 상태 목록
	 */
	List<ExternalApiCallHistorySummaryDto> findCallHistorySummary(ExternalApiCallHistorySearchDto searchDto);

	/**
	 * 검색 조건에 맞는 External API 호출 이력의 실행 단위 전체 건수를 조회한다.
	 *
	 * @param searchDto External API 호출 이력 검색 조건
	 * @return 검색 조건에 맞는 전체 실행 건수
	 */
	long countCallHistorySummary(ExternalApiCallHistorySearchDto searchDto);
	
	/**
	 * 실행 식별자를 조건으로 호출 시도 이력을 요청 순번과 시도 횟수순으로 조회한다.
	 *
	 * @param executionId 조회 대상 External API 실행 식별자
	 * @return 해당 실행의 호출 시도 이력 목록
	 */
	List<ExternalApiCallHistoryDto> findCallHistoryDetail(@Param("executionId") String executionId);
	
}
