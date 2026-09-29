package com.kji.scheduler.service;

import org.springframework.stereotype.Service;

import com.kji.scheduler.dto.CollectRawDataDto;
import com.kji.scheduler.mapper.CollectRawDataMapper;

/**
 * External API 수집 원본 데이터 저장을 처리한다.
 *
 * @author kji
 * @since 2026. 9. 29.
 */
@Service
public class CollectRawDataService {
	
	private final CollectRawDataMapper collectRawDataMapper;
	
	public CollectRawDataService(CollectRawDataMapper collectRawDataMapper) {
		this.collectRawDataMapper = collectRawDataMapper;
	}
	
	/**
	 * External API 수집 원본 데이터를 저장한다.
	 *
	 * @param collectRawData 수집 원본 데이터
	 */
	public void insertCollectRawData(CollectRawDataDto collectRawData) {
		
		// 1. 수집 원본 데이터를 저장한다.
		collectRawDataMapper.insertCollectRawData(collectRawData);
		
	}
	
}