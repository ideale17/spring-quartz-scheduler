package com.kji.scheduler.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kji.scheduler.dto.ExternalApiCallHistoryPageDto;
import com.kji.scheduler.dto.ExternalApiCallHistorySearchDto;
import com.kji.scheduler.dto.ExternalApiCallHistorySummaryDto;
import com.kji.scheduler.dto.ExternalApiExecutionLogDto;
import com.kji.scheduler.mapper.ExternalApiExecutionLogMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExternalApiExecutionLogService {
	
	private final ExternalApiExecutionLogMapper mapper;
	
	// External API 실행 시작 이력을 저장한다.
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void insertStart(ExternalApiExecutionLogDto executionLogDto) {
		
		// 1. 실행 시작 시간과 상태를 설정한다.
		executionLogDto.setStartedAt(LocalDateTime.now());
		executionLogDto.setStatus("RUNNING");
		
		// 2. External API 실행 시작 이력을 저장한다.
		mapper.insertStart(executionLogDto);
	}
	
	// External API 실행을 성공 상태로 종료한다.
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markSuccess(ExternalApiExecutionLogDto executionLogDto) {
		
		// 1. 실행 종료 정보를 설정한다.
		setFinishInfo(executionLogDto);
		executionLogDto.setStatus("SUCCESS");
		executionLogDto.setErrorMessage(null);
		
		// 2. External API 실행 결과를 갱신한다.
		mapper.updateFinish(executionLogDto);
	}
	
	// External API 실행을 실패 상태로 종료한다.
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markFailed(ExternalApiExecutionLogDto executionLogDto, String errorMessage) {
		
		// 1. 오류 메시지가 저장 가능한 길이를 초과하면 자른다.
		if (errorMessage != null && errorMessage.length() > 2000) {
			errorMessage = errorMessage.substring(0, 2000);
		}
		
		// 2. 실행 종료 정보를 설정한다.
		setFinishInfo(executionLogDto);
		executionLogDto.setStatus("FAILED");
		executionLogDto.setErrorMessage(errorMessage);
		
		// 3. External API 실행 결과를 갱신한다.
		mapper.updateFinish(executionLogDto);
	}
	
	// External API 실행 종료 시간과 전체 소요 시간을 설정한다.
	private void setFinishInfo(ExternalApiExecutionLogDto executionLogDto) {
		
		// 1. 실행 종료 시간을 설정한다.
		LocalDateTime finishedAt = LocalDateTime.now();
		executionLogDto.setFinishedAt(finishedAt);
		
		// 2. 실행 시작 시간이 있으면 전체 소요 시간을 계산한다.
		if (executionLogDto.getStartedAt() != null) {
			long runMillis = Duration.between(executionLogDto.getStartedAt(), finishedAt).toMillis();
			executionLogDto.setRunMillis(runMillis);
		}
	}
	
	// External API 실행 이력을 조회한다.
	public ExternalApiCallHistoryPageDto getExecutionHistory(ExternalApiCallHistorySearchDto searchDto) {
		
		// 1. 종료일이 있으면 조회 종료 시각을 다음 날로 계산한다.
		if (searchDto.getEndDate() != null) {
			searchDto.setEndDateExclusive(searchDto.getEndDate().plusDays(1));
		}
		
		// 2. 검색조건으로 External API 실행 이력을 조회한다.
		List<ExternalApiCallHistorySummaryDto> content = mapper.findExecutionHistory(searchDto);
		
		// 3. 동일한 검색조건의 전체 실행 이력 건수를 조회한다.
		long totalCount = mapper.countExecutionHistory(searchDto);
		
		// 4. 조회 결과와 페이징 정보를 반환한다.
		return new ExternalApiCallHistoryPageDto(
				content,
				totalCount,
				searchDto.getPage(),
				searchDto.getSize()
		);
		
	}
	
}