package com.kji.scheduler.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.kji.scheduler.dto.ExternalApiCallLogDto;

@Mapper
public interface ExternalApiCallLogMapper {
	
	int insertStart(ExternalApiCallLogDto externalApiCallLogDto);
	
	int updateFinish(ExternalApiCallLogDto externalApiCallLogDto);
	
}