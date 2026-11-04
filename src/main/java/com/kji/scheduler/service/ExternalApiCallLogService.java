package com.kji.scheduler.service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kji.scheduler.dto.ExternalApiCallAttemptDetailDto;
import com.kji.scheduler.dto.ExternalApiCallHistoryDto;
import com.kji.scheduler.dto.ExternalApiCallHistoryPageDto;
import com.kji.scheduler.dto.ExternalApiCallHistorySearchDto;
import com.kji.scheduler.dto.ExternalApiCallHistorySummaryDto;
import com.kji.scheduler.dto.ExternalApiCallLogDto;
import com.kji.scheduler.mapper.ExternalApiCallLogMapper;
import com.kji.scheduler.mapper.ExternalApiRequestLogMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExternalApiCallLogService {
	
	private final ExternalApiCallLogMapper mapper;
	private final ExternalApiRequestLogMapper externalApiRequestLogMapper;
	
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
	

	/**
	 * External API 호출 시도 상세 정보와 실제 요청 파라미터를 조회한다.
	 *
	 * @param apiCallLogId HTTP 호출 시도 식별자
	 * @return HTTP 호출 시도 상세 정보
	 * @throws IllegalArgumentException 호출 시도 식별자가 올바르지 않은 경우
	 * @throws IllegalStateException 호출 시도 이력이 존재하지 않는 경우
	 */
	public ExternalApiCallAttemptDetailDto getCallAttemptDetail(Long apiCallLogId) {
		
		// 1. 호출 시도 식별자를 검증한다.
		if (apiCallLogId == null || apiCallLogId < 1) {
			throw new IllegalArgumentException("External API 호출 이력 식별자가 올바르지 않습니다.");
		}
		
		// 2. HTTP 호출 시도 상세 정보를 조회한다.
		ExternalApiCallAttemptDetailDto detail = mapper.findCallAttemptDetail(apiCallLogId);
		
		// 3. 호출 시도 이력이 존재하는지 확인한다.
		if (detail == null) {
			throw new IllegalStateException("External API 호출 이력이 존재하지 않습니다.");
		}
		
		// 4. 과거 이력 등 요청 식별자가 없는 경우 빈 파라미터 목록을 설정한다.
		if (detail.getRequestId() == null) {
			detail.setParams(Collections.emptyList());
			return detail;
		}
		
		// 5. 실제 요청에 사용된 Query 및 Body 파라미터를 조회한다.
		detail.setParams(externalApiRequestLogMapper.findRequestParams(detail.getRequestId()));
		
		// 6. 호출 시도 상세 정보를 반환한다.
		return detail;
	}
	
}
