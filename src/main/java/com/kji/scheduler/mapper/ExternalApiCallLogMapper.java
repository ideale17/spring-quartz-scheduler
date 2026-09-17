package com.kji.scheduler.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.kji.scheduler.dto.ExternalApiCallHistoryDto;
import com.kji.scheduler.dto.ExternalApiCallHistorySearchDto;
import com.kji.scheduler.dto.ExternalApiCallLogDto;

@Mapper
public interface ExternalApiCallLogMapper {
	
	int insertStart(ExternalApiCallLogDto externalApiCallLogDto);
	
	int updateFinish(ExternalApiCallLogDto externalApiCallLogDto);
	
	List<ExternalApiCallHistoryDto> findCallHistory(ExternalApiCallHistorySearchDto searchDto);

	long countCallHistory(ExternalApiCallHistorySearchDto searchDto);
	
}