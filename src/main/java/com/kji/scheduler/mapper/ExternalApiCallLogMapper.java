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
	
	int insertStart(ExternalApiCallLogDto externalApiCallLogDto);
	
	int updateFinish(ExternalApiCallLogDto externalApiCallLogDto);
	
	List<ExternalApiCallHistoryDto> findCallHistory(ExternalApiCallHistorySearchDto searchDto);

	long countCallHistory(ExternalApiCallHistorySearchDto searchDto);
	
	// External API 실행 단위 호출 이력 조회
	List<ExternalApiCallHistorySummaryDto> findCallHistorySummary(ExternalApiCallHistorySearchDto searchDto);

	// External API 실행 단위 호출 이력 전체 건수 조회
	long countCallHistorySummary(ExternalApiCallHistorySearchDto searchDto);
	
	// External API 실행별 호출 시도 이력 조회
	List<ExternalApiCallHistoryDto> findCallHistoryDetail(@Param("executionId") String executionId);
	
}