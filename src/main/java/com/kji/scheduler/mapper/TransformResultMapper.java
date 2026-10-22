package com.kji.scheduler.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kji.scheduler.dto.TransformResultDetailDto;
import com.kji.scheduler.dto.TransformResultListDto;
import com.kji.scheduler.dto.TransformResultSearchDto;

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
	
	/**
	 * 검색조건과 가공 기간을 기준으로 가공 결과 목록을 조회한다.
	 *
	 * @param searchDto 가공 결과 검색조건 및 페이징 정보
	 * @param startAt 조회 시작 일시
	 * @param endAt 조회 종료 일시(미포함)
	 * @return 가공 결과 목록
	 */
	List<TransformResultListDto> findTransformResultList(
			@Param("searchDto") TransformResultSearchDto searchDto,
			@Param("startAt") LocalDateTime startAt,
			@Param("endAt") LocalDateTime endAt);
	
	/**
	 * 검색조건과 가공 기간을 기준으로 전체 가공 결과 건수를 조회한다.
	 *
	 * @param searchDto 가공 결과 검색조건
	 * @param startAt 조회 시작 일시
	 * @param endAt 조회 종료 일시(미포함)
	 * @return 전체 가공 결과 건수
	 */
	long countTransformResult(
			@Param("searchDto") TransformResultSearchDto searchDto,
			@Param("startAt") LocalDateTime startAt,
			@Param("endAt") LocalDateTime endAt);
	
	/**
	 * 가공 결과 식별자로 상세 정보를 조회한다.
	 *
	 * @param transformResultId 가공 결과 식별자
	 * @return 가공 결과 상세 정보, 없으면 null
	 */
	TransformResultDetailDto findTransformResultDetail(@Param("transformResultId") Long transformResultId);
	
}