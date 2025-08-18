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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insertStart(JobLogDto jobLogDto) {
        mapper.insertStart(jobLogDto);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(String fireInstanceId, long runMillis) {
        mapper.updateFinish(fireInstanceId, "SUCCESS", runMillis, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String fireInstanceId, long runMillis, String message) {
        // 메시지는 너무 길 수 있으니 2000자 내로 자르기(옵션)
        if (message != null && message.length() > 1990) {
            message = message.substring(0, 1990);
        }
        mapper.updateFinish(fireInstanceId, "FAILED", runMillis, message);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markVetoed(String fireInstanceId) {
        mapper.updateVetoed(fireInstanceId);
    }
    
}
