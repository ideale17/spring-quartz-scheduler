package com.kji.scheduler.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TransformResultMapper {
	
	/**
	 * 가공 결과 JSON을 저장한다.
	 *
	 * @param transformTaskId 가공 작업 식별자
	 * @param resultData 가공 결과 JSON 문자열
	 * @return 저장된 행 수
	 */
	int insertTransformResult(@Param("transformTaskId") Long transformTaskId, @Param("resultData") String resultData);
	
}