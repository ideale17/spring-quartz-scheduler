package com.kji.scheduler.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kji.scheduler.dto.RawDataDetailDto;
import com.kji.scheduler.dto.RawDataListDto;
import com.kji.scheduler.dto.RawDataSearchDto;

@Mapper
public interface RawDataMapper {
	
	/**
	 * 검색조건과 수집 기간을 기준으로 Raw Data 목록을 수집 일시 및 식별자 내림차순으로 조회한다.
	 *
	 * @param searchDto Raw Data 검색조건 및 페이징 정보
	 * @param startAt 조회 시작 일시
	 * @param endAt 조회 종료 일시(미포함)
	 * @return Raw Data 목록
	 */
	List<RawDataListDto> findRawDataList(
			@Param("searchDto") RawDataSearchDto searchDto,
			@Param("startAt") LocalDateTime startAt,
			@Param("endAt") LocalDateTime endAt);
	
	/**
	 * 검색조건과 수집 기간을 기준으로 Raw Data 전체 건수를 조회한다.
	 *
	 * @param searchDto Raw Data 검색조건
	 * @param startAt 조회 시작 일시
	 * @param endAt 조회 종료 일시(미포함)
	 * @return Raw Data 전체 건수
	 */
	long countRawData(
			@Param("searchDto") RawDataSearchDto searchDto,
			@Param("startAt") LocalDateTime startAt,
			@Param("endAt") LocalDateTime endAt);
	
	/**
	 * Raw Data 식별자를 조건으로 원본 응답을 포함한 상세 정보를 조회한다.
	 *
	 * @param rawDataId 조회 대상 Raw Data 식별자
	 * @return Raw Data 상세 정보, 없으면 null
	 */
	RawDataDetailDto findRawDataDetail(@Param("rawDataId") Long rawDataId);
	
}