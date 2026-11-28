package com.kji.scheduler.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kji.scheduler.dto.AnalysisLoadListDto;
import com.kji.scheduler.dto.AnalysisLoadPageDto;
import com.kji.scheduler.dto.AnalysisLoadRequestDto;
import com.kji.scheduler.dto.AnalysisLoadSearchDto;
import com.kji.scheduler.dto.AnalysisModelDto;
import com.kji.scheduler.dto.AnalysisModelVersionDto;
import com.kji.scheduler.mapper.AnalysisLoadMapper;
import com.kji.scheduler.mapper.AnalysisModelMapper;

@Service
public class AnalysisLoadService {

	private static final Set<String> LOAD_SEARCH_STATUSES =
			Set.of("ALL", "UNREGISTERED", "PENDING", "RUNNING", "SUCCESS", "FAILED");
	
	private final AnalysisLoadMapper analysisLoadMapper;
	private final AnalysisModelMapper analysisModelMapper;
	
	public AnalysisLoadService(
			AnalysisLoadMapper analysisLoadMapper,
			AnalysisModelMapper analysisModelMapper) {
		this.analysisLoadMapper = analysisLoadMapper;
		this.analysisModelMapper = analysisModelMapper;
	}
	
	/**
	 * 선택한 적재 모델의 정보와 가공 결과, 적재 상태를 함께 조회한다.
	 *
	 * @param searchDto 적재 모델 식별자, 기간, 적재 상태 및 페이징 정보
	 * @return 적재 모델 정보와 적재 대상 목록
	 */
	@Transactional(readOnly = true)
	public AnalysisLoadPageDto getAnalysisLoadList(AnalysisLoadSearchDto searchDto) {
		
		// 1. 검색조건을 검증한다.
		if (searchDto == null || searchDto.getAnalysisModelId() == null
				|| searchDto.getAnalysisModelId() < 1) {
			throw new IllegalArgumentException("데이터 적재 모델을 선택해주세요.");
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

		String loadStatus = searchDto.getLoadStatus();
		if (loadStatus != null && !LOAD_SEARCH_STATUSES.contains(loadStatus)) {
			throw new IllegalArgumentException("지원하지 않는 적재 상태 조회 조건입니다.");
		}
		
		// 2. 적재 모델 기본 정보를 조회한다.
		AnalysisModelDto model = analysisModelMapper.findAnalysisModel(searchDto.getAnalysisModelId());
		
		if (model == null) {
			throw new IllegalArgumentException("데이터 적재 모델을 찾을 수 없습니다.");
		}
		
		if (!"Y".equals(model.getEnabled())) {
			throw new IllegalArgumentException("사용 중지된 데이터 적재 모델입니다.");
		}
		
		// 3. 현재 적재 모델 버전을 확인한다.
		AnalysisModelVersionDto version = analysisModelMapper.findCurrentAnalysisModelVersion(model.getAnalysisModelId());
		
		if (version == null) {
			throw new IllegalStateException("현재 데이터 적재 모델 버전을 찾을 수 없습니다.");
		}
		
		// 4. 연결된 가공 규칙 버전 번호를 조회한다.
		Integer transformVersionNo = analysisLoadMapper.findTransformRuleVersionNo(version.getTransformRuleVersionId());
		
		if (transformVersionNo == null) {
			throw new IllegalStateException("연결된 가공 규칙 버전 정보를 찾을 수 없습니다.");
		}
		
		// 5. 검색 기간을 조회 일시 범위로 변환한다.
		LocalDateTime startAt = searchDto.getStartDate() != null
				? searchDto.getStartDate().atStartOfDay()
				: null;
		
		LocalDateTime endAt = searchDto.getEndDate() != null
				? searchDto.getEndDate().plusDays(1).atStartOfDay()
				: null;
		
		// 6. 가공 결과와 적재 작업 상태를 조회한다.
		List<AnalysisLoadListDto> content = analysisLoadMapper.findAnalysisLoadList(
				searchDto,
				version.getTransformRuleId(),
				version.getAnalysisModelVersionId(),
				version.getTransformRuleVersionId(),
				startAt,
				endAt);
		
		// 7. 동일한 검색조건으로 전체 건수를 조회한다.
		long totalCount = analysisLoadMapper.countAnalysisLoadList(
				searchDto,
				version.getTransformRuleId(),
				version.getAnalysisModelVersionId(),
				version.getTransformRuleVersionId(),
				startAt,
				endAt);
		
		// 8. 모델 정보와 적재 대상 목록을 하나의 응답으로 반환한다.
		return new AnalysisLoadPageDto(
				model,
				version,
				transformVersionNo,
				content,
				totalCount,
				searchDto.getPage(),
				searchDto.getSize());
	}
	
	/**
	 * 선택한 가공 결과에 대해 데이터 적재 작업을 등록한다.
	 *
	 * @param request 적재 모델과 가공 결과 식별자 목록
	 * @return 등록된 적재 작업 건수
	 */
	@Transactional
	public int createAnalysisLoadSources(AnalysisLoadRequestDto request) {
		
		// 1. 필수 요청 정보를 검증한다.
		if (request == null || request.getAnalysisModelId() == null || request.getAnalysisModelId() < 1) {
			throw new IllegalArgumentException("데이터 적재 모델을 선택해주세요.");
		}
		
		List<Long> resultIds = request.getTransformResultIds();
		
		if (resultIds == null || resultIds.isEmpty()) {
			throw new IllegalArgumentException("적재할 가공 결과를 선택해주세요.");
		}
		
		if (resultIds.size() > 100) {
			throw new IllegalArgumentException("적재 작업은 한 번에 최대 100건까지 등록할 수 있습니다.");
		}
		
		// 2. 중복된 가공 결과 식별자가 있는지 확인한다.
		Set<Long> resultIdSet = new HashSet<>();
		
		for (Long resultId : resultIds) {
			if (resultId == null || resultId < 1) {
				throw new IllegalArgumentException("가공 결과 식별자가 올바르지 않습니다.");
			}
			
			if (!resultIdSet.add(resultId)) {
				throw new IllegalArgumentException("중복된 가공 결과가 선택되었습니다. transformResultId: " + resultId);
			}
		}
		
		// 3. 적재 모델의 존재 여부와 사용 상태를 확인한다.
		AnalysisModelDto model = analysisModelMapper.findAnalysisModel(request.getAnalysisModelId());
		
		if (model == null) {
			throw new IllegalArgumentException("데이터 적재 모델을 찾을 수 없습니다.");
		}
		
		if (!"Y".equals(model.getEnabled())) {
			throw new IllegalArgumentException("사용 중지된 데이터 적재 모델은 실행할 수 없습니다.");
		}
		
		// 4. 작업 등록 시점의 모델 버전을 확정한다.
		AnalysisModelVersionDto version = analysisModelMapper.findCurrentAnalysisModelVersion(model.getAnalysisModelId());
		
		if (version == null) {
			throw new IllegalStateException("현재 데이터 적재 모델 버전을 찾을 수 없습니다.");
		}
		
		// 5. 가공 결과별로 대기 작업을 등록한다.
		int registeredCount = 0;
		
		for (Long resultId : resultIds) {
			int inserted = analysisLoadMapper.insertAnalysisLoadSource(
					version.getAnalysisModelVersionId(),
					version.getTransformRuleVersionId(),
					resultId);
			
			if (inserted != 1) {
				throw new IllegalArgumentException("적재 모델과 일치하는 가공 완료 결과가 아닙니다. transformResultId: " + resultId);
			}
			
			registeredCount++;
		}
		
		// 6. 등록된 적재 작업 건수를 반환한다.
		return registeredCount;
	}
	
}
