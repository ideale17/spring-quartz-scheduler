package com.kji.scheduler.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kji.scheduler.dto.TransformResultListDto;
import com.kji.scheduler.dto.TransformResultPageDto;
import com.kji.scheduler.dto.TransformResultSearchDto;
import com.kji.scheduler.mapper.TransformResultMapper;

@Service
public class TransformResultService {

	private final TransformResultMapper mapper;

	public TransformResultService(TransformResultMapper mapper) {
		this.mapper = mapper;
	}

	/**
	 * 검색조건과 가공 기간을 기준으로 가공 결과 목록을 페이징 조회한다.
	 *
	 * @param searchDto 가공 결과 검색조건 및 페이징 정보
	 * @return 페이지 정보를 포함한 가공 결과 목록
	 * @throws IllegalArgumentException 검색조건이나 페이징 값이 올바르지 않은 경우
	 */
	@Transactional(readOnly = true)
	public TransformResultPageDto getTransformResultList(TransformResultSearchDto searchDto) {

		// 1. 검색조건과 페이징 값을 검증한다.
		if (searchDto == null) {
			throw new IllegalArgumentException("가공 결과 검색조건이 없습니다.");
		}

		if (searchDto.getPage() < 1) {
			throw new IllegalArgumentException("페이지 번호는 1 이상이어야 합니다.");
		}

		if (searchDto.getSize() < 1 || searchDto.getSize() > 100) {
			throw new IllegalArgumentException("페이지당 조회 건수는 1~100 사이여야 합니다.");
		}

		if (searchDto.getExternalApiId() != null && searchDto.getExternalApiId() < 1) {
			throw new IllegalArgumentException("External API 식별자가 올바르지 않습니다.");
		}

		if (searchDto.getTransformRuleId() != null && searchDto.getTransformRuleId() < 1) {
			throw new IllegalArgumentException("가공 규칙 식별자가 올바르지 않습니다.");
		}

		if (searchDto.getStartDate() != null && searchDto.getEndDate() != null
				&& searchDto.getStartDate().isAfter(searchDto.getEndDate())) {
			throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
		}

		// 2. 가공 시작일과 종료일을 조회 일시 범위로 변환한다.
		LocalDateTime startAt = searchDto.getStartDate() != null
				? searchDto.getStartDate().atStartOfDay()
				: null;

		LocalDateTime endAt = searchDto.getEndDate() != null
				? searchDto.getEndDate().plusDays(1).atStartOfDay()
				: null;

		// 3. 검색조건과 가공 기간으로 가공 결과 목록을 조회한다.
		List<TransformResultListDto> content = mapper.findTransformResultList(searchDto, startAt, endAt);

		// 4. 동일한 검색조건으로 전체 가공 결과 건수를 조회한다.
		long totalCount = mapper.countTransformResult(searchDto, startAt, endAt);

		// 5. 조회 결과와 페이징 정보를 반환한다.
		return new TransformResultPageDto(
				content,
				totalCount,
				searchDto.getPage(),
				searchDto.getSize()
		);

	}

}