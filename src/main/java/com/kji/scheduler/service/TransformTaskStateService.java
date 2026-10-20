package com.kji.scheduler.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kji.scheduler.dto.TransformTaskDto;
import com.kji.scheduler.mapper.TransformTaskMapper;

@Service
public class TransformTaskStateService {
	
	private final TransformTaskMapper mapper;
	
	public TransformTaskStateService(TransformTaskMapper mapper) {
		this.mapper = mapper;
	}
	
	/**
	 * 대기 중인 가공 작업을 최대 지정 건수만큼 원자적으로 선점한다.
	 * 선점 상태는 별도 트랜잭션에서 확정하고 실제 가공은 이후에 수행한다.
	 *
	 * @param batchSize 최대 선점 건수
	 * @return 선점한 가공 작업 목록
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public List<TransformTaskDto> claimPendingTasks(int batchSize) {

	    if (batchSize < 1 || batchSize > 100) {
	        throw new IllegalArgumentException("배치 선점 건수는 1~100 사이여야 합니다.");
	    }

	    return mapper.claimPendingTasks(batchSize);
	}
	
	/**
	 * 실패한 가공 작업의 상태와 오류를 기록한다.
	 *
	 * @param transformTaskId 가공 작업 식별자
	 * @param errorMessage 오류 메시지
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markFailed(Long transformTaskId, String errorMessage) {
		
		String message = errorMessage == null ? "알 수 없는 가공 오류" : errorMessage;
		
		if (message.length() > 2000) {
			message = message.substring(0, 2000);
		}
		
		if (mapper.markFailed(transformTaskId, message) != 1) {
			throw new IllegalStateException("가공 작업 실패 상태 변경에 실패했습니다.");
		}
	}
	
}