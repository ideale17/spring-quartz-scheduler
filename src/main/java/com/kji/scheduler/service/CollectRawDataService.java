
package com.kji.scheduler.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.kji.scheduler.dto.CollectRawDataDto;

@Service
public class CollectRawDataService {
	
	private static final Logger log = LoggerFactory.getLogger(CollectRawDataService.class);
	
	private final RawDataStorageService rawDataStorageService;
	private final AutoTransformTaskRegistrationService autoTransformTaskRegistrationService;
	
	public CollectRawDataService(
			RawDataStorageService rawDataStorageService,
			AutoTransformTaskRegistrationService autoTransformTaskRegistrationService) {
		this.rawDataStorageService = rawDataStorageService;
		this.autoTransformTaskRegistrationService = autoTransformTaskRegistrationService;
	}
	
	/**
	 * 수집 원본 데이터를 저장한 후 자동 가공 작업 등록을 시도한다.
	 * 자동 가공 작업 등록에 실패하더라도 이미 저장된 원본 데이터는 유지한다.
	 *
	 * @param collectRawData 수집 원본 데이터
	 */
	public void insertCollectRawData(CollectRawDataDto collectRawData) {
		
		// 1. 수집 원본 데이터를 별도 트랜잭션에서 저장한다.
		rawDataStorageService.saveRawData(collectRawData);
		
		// 2. 원본 데이터 저장이 완료되면 자동 가공 작업 등록을 시도한다.
		try {
			
			int registeredCount = autoTransformTaskRegistrationService.registerAutoTransformTasks(
					collectRawData.getRawDataId(), collectRawData.getExternalApiId());
			
			// 3. 자동 가공 작업 등록 결과를 기록한다.
			log.info("자동 가공 작업 등록 완료. rawDataId: {}, externalApiId: {}, registeredCount: {}",
					collectRawData.getRawDataId(), collectRawData.getExternalApiId(), registeredCount);
			
		} catch (Exception e) {
			
			// 4. 등록 실패는 기록하되 원본 데이터 수집 성공에는 영향을 주지 않는다.
			log.error("자동 가공 작업 등록 실패. rawDataId: {}, externalApiId: {}",
					collectRawData.getRawDataId(), collectRawData.getExternalApiId(), e);
		}
		
	}
	
}
