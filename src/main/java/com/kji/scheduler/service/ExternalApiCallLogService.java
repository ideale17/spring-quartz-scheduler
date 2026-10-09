package com.kji.scheduler.service;

import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kji.scheduler.dto.ExternalApiCallHistoryDto;
import com.kji.scheduler.dto.ExternalApiCallHistoryPageDto;
import com.kji.scheduler.dto.ExternalApiCallHistorySearchDto;
import com.kji.scheduler.dto.ExternalApiCallHistorySummaryDto;
import com.kji.scheduler.dto.ExternalApiCallLogDto;
import com.kji.scheduler.mapper.ExternalApiCallLogMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExternalApiCallLogService {
	
	private final ExternalApiCallLogMapper mapper;
	
	/**
	 * External API 호출 시작 이력을 저장한다.
	 *
	 * @param externalApiCallLogDto External API 호출 이력 정보
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void insertStart(ExternalApiCallLogDto externalApiCallLogDto) {
		externalApiCallLogDto.setStatus("STARTED");
		mapper.insertStart(externalApiCallLogDto);
	}
	
	/**
	 * External API 호출을 성공 상태로 종료하고 이력을 갱신한다.
	 *
	 * @param externalApiCallLogDto External API 호출 이력 정보
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markSuccess(ExternalApiCallLogDto externalApiCallLogDto) {
		externalApiCallLogDto.setStatus("SUCCESS");
		externalApiCallLogDto.setErrorMessage(null);
		mapper.updateFinish(externalApiCallLogDto);
	}
	
	/**
	 * External API 호출을 실패 상태로 종료하고 오류 메시지와 이력을 갱신한다.
	 *
	 * @param externalApiCallLogDto External API 호출 이력 정보
	 * @param errorMessage 저장할 오류 메시지
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markFailed(ExternalApiCallLogDto externalApiCallLogDto, String errorMessage) {
		
		if (errorMessage != null && errorMessage.length() > 2000) {
			errorMessage = errorMessage.substring(0, 2000);
		}
		
		externalApiCallLogDto.setStatus("FAILED");
		externalApiCallLogDto.setErrorMessage(errorMessage);
		
		mapper.updateFinish(externalApiCallLogDto);
	}
	
	/**
	 * 검색 조건에 맞는 External API 실행 단위 호출 이력을 조회한다.
	 *
	 * @param searchDto External API 호출 이력 검색 조건
	 * @return 페이지 정보와 실행별 전체 소요 시간을 포함한 호출 이력
	 */
	public ExternalApiCallHistoryPageDto getCallHistory(ExternalApiCallHistorySearchDto searchDto) {
		
		// 1. 종료일이 있으면 조회 종료 시각을 다음 날로 계산한다.
		if (searchDto.getEndDate() != null) {
			searchDto.setEndDateExclusive(searchDto.getEndDate().plusDays(1));
		}
		
		// 2. 검색조건으로 External API 실행 단위 호출 이력을 조회한다.
		List<ExternalApiCallHistorySummaryDto> content = mapper.findCallHistorySummary(searchDto);
		
		// 3. 각 실행의 전체 소요 시간을 계산한다.
		for (ExternalApiCallHistorySummaryDto history : content) {
			
			if (history.getStartedAt() == null || history.getFinishedAt() == null) {
				continue;
			}
			
			long totalRunMillis = Duration.between(history.getStartedAt(), history.getFinishedAt()).toMillis();
			history.setTotalRunMillis(totalRunMillis);
		}
		
		// 4. 동일한 검색조건의 전체 실행 이력 건수를 조회한다.
		long totalCount = mapper.countCallHistorySummary(searchDto);
		
		// 5. 조회 결과와 페이징 정보를 반환한다.
		return new ExternalApiCallHistoryPageDto(
				content,
				totalCount,
				searchDto.getPage(),
				searchDto.getSize());
	}
	
	/**
	 * External API 실행별 호출 시도 이력을 조회한다.
	 *
	 * @param executionId External API 실행 식별자
	 * @return 해당 실행의 호출 시도 이력 목록
	 */
	public List<ExternalApiCallHistoryDto> getCallHistoryDetail(String executionId) {
		
		// 1. 실행 식별자를 검증한다.
		if (executionId == null || executionId.isBlank()) {
			throw new IllegalArgumentException("External API 실행 식별자는 필수입니다.");
		}
		
		// 2. 실행 식별자로 각 호출 시도 이력을 조회한다.
		return mapper.findCallHistoryDetail(executionId);
	}
	
}
