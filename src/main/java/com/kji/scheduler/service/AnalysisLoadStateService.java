package com.kji.scheduler.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kji.scheduler.dto.AnalysisLoadSourceDto;
import com.kji.scheduler.mapper.AnalysisLoadMapper;

@Service
public class AnalysisLoadStateService {
	
	private final AnalysisLoadMapper mapper;

	public AnalysisLoadStateService(AnalysisLoadMapper mapper) {
		this.mapper = mapper;
	}
	
	/**
	 * 대기 중인 적재 작업을 최대 지정 건수만큼 원자적으로 선점한다.
	 * 선점 상태는 별도 트랜잭션에서 확정하고 실제 적재는 이후에 수행한다.
	 *
	 * @param batchSize 최대 선점 건수
	 * @return 선점한 적재 작업 목록
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public List<AnalysisLoadSourceDto> claimPendingLoads(int batchSize) {
		
		// 1. 선점 건수를 검증한다.
		if (batchSize < 1 || batchSize > 100) {
			throw new IllegalArgumentException("배치 선점 건수는 1~100 사이여야 합니다.");
		}
		
		// 2. PENDING 적재 작업을 원자적으로 선점한다.
		return mapper.claimPendingLoads(batchSize);
		
	}
	
	/**
	 * 실패한 적재 작업의 상태와 오류를 기록한다.
	 *
	 * @param analysisLoadSourceId 적재 작업 식별자
	 * @param errorMessage 실패 원인
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markFailed(Long analysisLoadSourceId, String errorMessage) {
		
		// 1. 오류 메시지를 정리한다.
		String message = errorMessage == null || errorMessage.isBlank() ? "알 수 없는 데이터 적재 오류" : errorMessage;
		
		if (message.length() > 2000) {
			message = message.substring(0, 2000);
		}
		
		// 2. 실패 상태와 오류 메시지를 저장한다.
		if (mapper.markLoadFailed(analysisLoadSourceId, message) != 1) {
			throw new IllegalStateException("적재 작업 실패 상태 변경에 실패했습니다.");
		}
		
	}
	
}