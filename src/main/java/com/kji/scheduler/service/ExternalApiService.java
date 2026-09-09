package com.kji.scheduler.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.kji.scheduler.dto.ExternalApiDto;
import com.kji.scheduler.dto.ExternalApiParamDto;
import com.kji.scheduler.mapper.ExternalApiMapper;

@Service
public class ExternalApiService {
	
	private final ExternalApiMapper externalApiMapper;
	
	public ExternalApiService(ExternalApiMapper externalApiMapper) {
		this.externalApiMapper = externalApiMapper;
	}
	
	// External API 목록 조회
	public List<ExternalApiDto> getExternalApiList() {
		
		// 1. 등록된 External API 목록을 조회한다.
		return externalApiMapper.findAllExternalApiList();
	}
	
	// External API 단건 조회
	public ExternalApiDto getExternalApi(Long externalApiId) {
		
		// 1. External API 기본 정보를 조회한다.
		ExternalApiDto externalApi = externalApiMapper.findExternalApi(externalApiId);
		
		// 2. 조회 결과가 없으면 예외를 발생시킨다.
		if (externalApi == null) {
			throw new IllegalArgumentException("존재하지 않는 External API입니다. externalApiId: " + externalApiId);
		}
		
		return externalApi;
	}
	
	// External API 파라미터 목록 조회
	public List<ExternalApiParamDto> getExternalApiParamList(Long externalApiId) {
		
		// 1. External API 존재 여부를 확인한다.
		getExternalApi(externalApiId);
		
		// 2. External API에 등록된 파라미터 목록을 조회한다.
		return externalApiMapper.findExternalApiParamList(externalApiId);
	}
	
}