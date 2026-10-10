package com.kji.scheduler.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kji.scheduler.dto.RawDataDetailDto;
import com.kji.scheduler.dto.RawDataListDto;
import com.kji.scheduler.dto.RawDataPageDto;
import com.kji.scheduler.dto.RawDataSearchDto;
import com.kji.scheduler.mapper.RawDataMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RawDataService {
	
	private final RawDataMapper mapper;
	
	/**
	 * 검색조건과 수집 기간을 기준으로 Raw Data 목록을 페이징 조회한다.
	 *
	 * @param searchDto Raw Data 검색조건 및 페이징 정보
	 * @return 페이지 정보를 포함한 Raw Data 목록
	 * @throws IllegalArgumentException 검색조건이나 페이징 값이 올바르지 않은 경우
	 */
	@Transactional(readOnly = true)
	public RawDataPageDto getRawDataList(RawDataSearchDto searchDto) {
		
		// 1. 검색조건과 페이징 값을 검증한다.
		if (searchDto == null) {
			throw new IllegalArgumentException("Raw Data 검색조건이 없습니다.");
		}
		
		if (searchDto.getPage() < 1) {
			throw new IllegalArgumentException("페이지 번호는 1 이상이어야 합니다.");
		}
		
		if (searchDto.getSize() < 1 || searchDto.getSize() > 100) {
			throw new IllegalArgumentException("페이지당 조회 건수는 1~100 사이여야 합니다.");
		}
		
		if (searchDto.getStartDate() != null && searchDto.getEndDate() != null
				&& searchDto.getStartDate().isAfter(searchDto.getEndDate())) {
			throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
		}
		
		// 2. 수집 시작일과 종료일을 조회 일시 범위로 변환한다.
		LocalDateTime startAt = searchDto.getStartDate() != null
				? searchDto.getStartDate().atStartOfDay()
				: null;

		LocalDateTime endAt = searchDto.getEndDate() != null
				? searchDto.getEndDate().plusDays(1).atStartOfDay()
				: null;
		
		// 3. 검색조건과 수집 기간으로 Raw Data 목록을 조회한다.
		List<RawDataListDto> content = mapper.findRawDataList(searchDto, startAt, endAt);
		
		// 4. 동일한 검색조건으로 전체 Raw Data 건수를 조회한다.
		long totalCount = mapper.countRawData(searchDto, startAt, endAt);
		
		// 5. 조회 결과와 페이징 정보를 반환한다.
		return new RawDataPageDto(
				content,
				totalCount,
				searchDto.getPage(),
				searchDto.getSize()
		);
		
	}
	
	/**
	 * Raw Data 식별자를 조건으로 원본 응답을 포함한 상세 정보를 조회한다.
	 *
	 * @param rawDataId 조회 대상 Raw Data 식별자
	 * @return Raw Data 상세 정보
	 * @throws IllegalArgumentException Raw Data 식별자가 올바르지 않거나 조회 대상이 없는 경우
	 */
	@Transactional(readOnly = true)
	public RawDataDetailDto getRawDataDetail(Long rawDataId) {
		
		// 1. Raw Data 식별자를 검증한다.
		if (rawDataId == null || rawDataId < 1) {
			throw new IllegalArgumentException("Raw Data 식별자가 올바르지 않습니다.");
		}
		
		// 2. Raw Data 상세 정보를 조회한다.
		RawDataDetailDto rawData = mapper.findRawDataDetail(rawDataId);
		
		// 3. 조회 대상이 존재하는지 확인한다.
		if (rawData == null) {
			throw new IllegalArgumentException("Raw Data 정보를 찾을 수 없습니다. rawDataId: " + rawDataId);
		}
		
		// 4. Raw Data 상세 정보를 반환한다.
		return rawData;
		
	}
	
}