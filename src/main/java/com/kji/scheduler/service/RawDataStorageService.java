
package com.kji.scheduler.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kji.scheduler.dto.CollectRawDataDto;
import com.kji.scheduler.mapper.CollectRawDataMapper;

@Service
public class RawDataStorageService {
	
	private final CollectRawDataMapper collectRawDataMapper;
	
	public RawDataStorageService(CollectRawDataMapper collectRawDataMapper) {
		this.collectRawDataMapper = collectRawDataMapper;
	}
	
	/**
	 * 수집 원본 데이터를 별도 트랜잭션에서 저장한다.
	 *
	 * @param collectRawData 수집 원본 데이터
	 * @throws IllegalArgumentException 수집 정보가 올바르지 않은 경우
	 * @throws IllegalStateException 저장에 실패한 경우
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void saveRawData(CollectRawDataDto collectRawData) {
		
		// 1. 수집 원본 데이터 정보를 검증한다.
		if (collectRawData == null || collectRawData.getExternalApiId() == null
				|| collectRawData.getExternalApiId() < 1) {
			throw new IllegalArgumentException("수집 데이터의 External API 식별자가 올바르지 않습니다.");
		}
		
		// 2. Raw Data를 저장한다.
		if (collectRawDataMapper.insertCollectRawData(collectRawData) != 1
				|| collectRawData.getRawDataId() == null) {
			throw new IllegalStateException("Raw Data 저장에 실패했습니다.");
		}
	}
	
}
