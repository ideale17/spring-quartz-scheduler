package com.kji.scheduler.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.kji.scheduler.dto.CollectRawDataDto;

@Mapper
public interface CollectRawDataMapper {
	
	// 수집 원본 데이터 저장
	int insertCollectRawData(CollectRawDataDto collectRawData);
	
}