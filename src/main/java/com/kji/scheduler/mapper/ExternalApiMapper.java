package com.kji.scheduler.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kji.scheduler.dto.ExternalApiDto;
import com.kji.scheduler.dto.ExternalApiParamDto;

@Mapper
public interface ExternalApiMapper {

	// External API 목록 조회
	List<ExternalApiDto> findAllExternalApiList();

	// External API 단건 조회
	ExternalApiDto findExternalApi(@Param("externalApiId") Long externalApiId);

	// External API 파라미터 목록 조회
	List<ExternalApiParamDto> findExternalApiParamList(@Param("externalApiId") Long externalApiId);

}