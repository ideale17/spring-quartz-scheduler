package com.kji.scheduler.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.kji.scheduler.dto.CollectRawDataDto;

@Mapper
public interface CollectRawDataMapper {
	
	/**
	 * External API 응답 원본 데이터를 실행 식별자와 요청 순번 정보와 함께 저장한다.
	 *
	 * @param collectRawData 저장할 수집 원본 데이터
	 * @return 저장된 행 수
	 */
	int insertCollectRawData(CollectRawDataDto collectRawData);
	
}
