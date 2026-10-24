
package com.kji.scheduler.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kji.scheduler.dto.TransformAutoConfigDto;
import com.kji.scheduler.dto.TransformTaskDto;
import com.kji.scheduler.mapper.TransformAutoConfigMapper;
import com.kji.scheduler.mapper.TransformTaskMapper;

@Service
public class AutoTransformTaskRegistrationService {
	
	private final TransformAutoConfigMapper transformAutoConfigMapper;
	private final TransformTaskMapper transformTaskMapper;
	
	public AutoTransformTaskRegistrationService(
			TransformAutoConfigMapper transformAutoConfigMapper,
			TransformTaskMapper transformTaskMapper) {
		this.transformAutoConfigMapper = transformAutoConfigMapper;
		this.transformTaskMapper = transformTaskMapper;
	}
	
	/**
	 * 저장된 Raw Data에 대한 자동 가공 작업을 별도 트랜잭션에서 등록한다.
	 *
	 * @param rawDataId 수집 원본 데이터 식별자
	 * @param externalApiId External API 식별자
	 * @return 등록된 자동 가공 작업 건수
	 * @throws IllegalArgumentException 식별자가 올바르지 않은 경우
	 * @throws IllegalStateException 자동 가공 작업 등록에 실패한 경우
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public int registerAutoTransformTasks(Long rawDataId, Long externalApiId) {
		
		// 1. 요청 식별자를 검증한다.
		if (rawDataId == null || rawDataId < 1 || externalApiId == null || externalApiId < 1) {
			throw new IllegalArgumentException("자동 가공 작업 등록 식별자가 올바르지 않습니다.");
		}
		
		// 2. 활성 자동 가공 규칙의 현재 버전을 조회한다.
		List<TransformAutoConfigDto> configs =
				transformAutoConfigMapper.findEnabledAutoTransformRules(externalApiId);
		
		// 3. 가공 규칙별로 대기 작업을 등록한다.
		for (TransformAutoConfigDto config : configs) {
			
			TransformTaskDto task = new TransformTaskDto();
			task.setRawDataId(rawDataId);
			task.setTransformRuleVersionId(config.getTransformRuleVersionId());
			task.setRequestType("SCHEDULED");
			task.setStatus("PENDING");
			
			if (transformTaskMapper.insertTransformTask(task) != 1 || task.getTransformTaskId() == null) {
				throw new IllegalStateException(
						"자동 가공 작업 등록에 실패했습니다. transformRuleId: " + config.getTransformRuleId());
			}
		}
		
		// 4. 등록된 자동 가공 작업 건수를 반환한다.
		return configs.size();
	}
	
}
