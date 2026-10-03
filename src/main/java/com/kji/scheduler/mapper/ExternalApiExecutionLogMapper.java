package com.kji.scheduler.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.kji.scheduler.dto.ExternalApiCallHistorySearchDto;
import com.kji.scheduler.dto.ExternalApiCallHistorySummaryDto;
import com.kji.scheduler.dto.ExternalApiExecutionLogDto;

@Mapper
public interface ExternalApiExecutionLogMapper {
	
	// External API 실행 시작 이력을 저장한다.
	void insertStart(ExternalApiExecutionLogDto externalApiExecutionLogDto);
	
	// External API 실행 종료 결과를 갱신한다.
	void updateFinish(ExternalApiExecutionLogDto externalApiExecutionLogDto);
	
	// External API 실행 이력을 조회한다.
	List<ExternalApiCallHistorySummaryDto> findExecutionHistory(ExternalApiCallHistorySearchDto searchDto);

	// External API 실행 이력 전체 건수를 조회한다.
	long countExecutionHistory(ExternalApiCallHistorySearchDto searchDto);
	
}