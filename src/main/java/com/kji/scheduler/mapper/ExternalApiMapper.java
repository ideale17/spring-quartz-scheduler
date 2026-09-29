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
	
	// External API 화면용 단건 조회
	ExternalApiDto findExternalApiDetail(@Param("externalApiId") Long externalApiId);
	
	// External API 파라미터 목록 조회
	List<ExternalApiParamDto> findExternalApiParamList(@Param("externalApiId") Long externalApiId);
	
	// External API 저장
	int insertExternalApi(ExternalApiDto externalApi);
	
	// External API 파라미터 저장
	int insertExternalApiParam(ExternalApiParamDto param);
	
	// External API 기본 정보 수정
	int updateExternalApi(ExternalApiDto externalApi);
	
	// External API 파라미터 전체 삭제
	int deleteExternalApiParams(@Param("externalApiId") Long externalApiId);
	
	// External API 삭제
	int deleteExternalApi(@Param("externalApiId") Long externalApiId);
	
}