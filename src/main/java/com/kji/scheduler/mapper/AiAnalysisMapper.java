package com.kji.scheduler.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.kji.scheduler.dto.AiAnalysisDataDto;
import com.kji.scheduler.dto.AiAnalysisSearchDto;

@Mapper
public interface AiAnalysisMapper {
	
	/**
	 * 검색조건에 해당하는 AI 분석 대상 가공 결과를 조회한다.
	 *
	 * @param searchDto AI 분석 대상 검색조건
	 * @return AI 분석 대상 가공 결과 목록
	 */
	List<AiAnalysisDataDto> findAnalysisData(AiAnalysisSearchDto searchDto);
	
	/**
	 * AI 분석 조건에 해당하는 전체 가공 결과 건수를 조회한다.
	 *
	 * @param searchDto AI 분석 대상 검색조건
	 * @return 전체 가공 결과 건수
	 */
	long countAnalysisData(AiAnalysisSearchDto searchDto);
	
}