package com.kji.scheduler.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kji.scheduler.dto.ExternalApiCallLogDto;
import com.kji.scheduler.mapper.ExternalApiCallLogMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExternalApiCallLogService {
	
	private final ExternalApiCallLogMapper mapper;
	
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void insertStart(ExternalApiCallLogDto externalApiCallLogDto) {
		externalApiCallLogDto.setStatus("STARTED");
		mapper.insertStart(externalApiCallLogDto);
	}
	
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markSuccess(ExternalApiCallLogDto externalApiCallLogDto) {
		externalApiCallLogDto.setStatus("SUCCESS");
		externalApiCallLogDto.setErrorMessage(null);
		mapper.updateFinish(externalApiCallLogDto);
	}
	
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markFailed(ExternalApiCallLogDto externalApiCallLogDto, String errorMessage) {
		
		if (errorMessage != null && errorMessage.length() > 2000) {
			errorMessage = errorMessage.substring(0, 2000);
		}
		
		externalApiCallLogDto.setStatus("FAILED");
		externalApiCallLogDto.setErrorMessage(errorMessage);
		
		mapper.updateFinish(externalApiCallLogDto);
	}
	
}