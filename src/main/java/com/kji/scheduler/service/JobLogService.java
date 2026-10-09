package com.kji.scheduler.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kji.scheduler.dto.JobLogDto;
import com.kji.scheduler.mapper.JobLogMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JobLogService {
	
	private final JobLogMapper mapper;

	/**
	 * Quartz Job 실행 시작 이력을 저장한다.
	 *
	 * @param jobLogDto Job 실행 이력 정보
	 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insertStart(JobLogDto jobLogDto) {
        mapper.insertStart(jobLogDto);
    }

	/**
	 * Quartz Job 실행을 성공 상태로 종료하고 이력을 갱신한다.
	 *
	 * @param fireInstanceId Quartz 실행 인스턴스 식별자
	 * @param runMillis 실행 소요 시간(밀리초)
	 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(String fireInstanceId, long runMillis) {
        mapper.updateFinish(fireInstanceId, "SUCCESS", runMillis, null);
    }

	/**
	 * Quartz Job 실행을 실패 상태로 종료하고 오류 메시지와 이력을 갱신한다.
	 *
	 * @param fireInstanceId Quartz 실행 인스턴스 식별자
	 * @param runMillis 실행 소요 시간(밀리초)
	 * @param message 저장할 오류 메시지
	 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String fireInstanceId, long runMillis, String message) {
        // 메시지는 너무 길 수 있으니 2000자 내로 자르기(옵션)
        if (message != null && message.length() > 1990) {
            message = message.substring(0, 1990);
        }
        mapper.updateFinish(fireInstanceId, "FAILED", runMillis, message);
    }

	/**
	 * Quartz Job 실행이 거부된 상태로 이력을 갱신한다.
	 *
	 * @param fireInstanceId Quartz 실행 인스턴스 식별자
	 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markVetoed(String fireInstanceId) {
        mapper.updateVetoed(fireInstanceId);
    }
    
}
