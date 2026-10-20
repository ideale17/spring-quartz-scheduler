
package com.kji.scheduler.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.kji.scheduler.dto.RawDataDetailDto;
import com.kji.scheduler.dto.TransformRuleFieldDto;
import com.kji.scheduler.dto.TransformRuleVersionDto;
import com.kji.scheduler.dto.TransformTaskDto;
import com.kji.scheduler.mapper.TransformResultMapper;
import com.kji.scheduler.mapper.TransformRuleMapper;
import com.kji.scheduler.mapper.TransformTaskMapper;

@Service
public class TransformTaskExecutionService {
	
	private final TransformTaskMapper taskMapper;
	private final TransformRuleMapper ruleMapper;
	private final TransformResultMapper resultMapper;
	private final RawDataService rawDataService;
	private final TransformEngineService transformEngineService;
	
	public TransformTaskExecutionService(
			TransformTaskMapper taskMapper,
			TransformRuleMapper ruleMapper,
			TransformResultMapper resultMapper,
			RawDataService rawDataService,
			TransformEngineService transformEngineService) {
		this.taskMapper = taskMapper;
		this.ruleMapper = ruleMapper;
		this.resultMapper = resultMapper;
		this.rawDataService = rawDataService;
		this.transformEngineService = transformEngineService;
	}
	
	/**
	 * 선점한 가공 작업을 실행하고 결과를 저장한다.
	 *
	 * @param transformTaskId 가공 작업 식별자
	 */
	@Transactional
	public void executeTask(Long transformTaskId) {
		
		// 1. 선점한 가공 작업을 조회한다.
		TransformTaskDto task = taskMapper.findTransformTask(transformTaskId);
		
		if (task == null || !"RUNNING".equals(task.getStatus())) {
			throw new IllegalStateException("실행 가능한 가공 작업을 찾을 수 없습니다.");
		}
		
		// 2. 원본 데이터를 조회한다.
		RawDataDetailDto rawData = rawDataService.getRawDataDetail(task.getRawDataId());
		
		// 3. 작업 등록 시 확정한 가공 규칙 버전을 조회한다.
		TransformRuleVersionDto version = ruleMapper.findTransformRuleVersion(task.getTransformRuleVersionId());
		
		if (version == null) {
			throw new IllegalStateException("가공 규칙 버전을 찾을 수 없습니다.");
		}
		
		// 4. 규칙 버전별 추출 필드를 조회한다.
		List<TransformRuleFieldDto> fields = ruleMapper.findTransformRuleFieldList(version.getTransformRuleVersionId());
		
		// 5. 원본 JSON을 가공한다.
		JsonNode result = transformEngineService.transformJson(rawData.getResponseBody(), version, fields);
		
		// 6. 가공 결과를 저장한다.
		if (resultMapper.insertTransformResult(transformTaskId, result.toString()) != 1) {
			throw new IllegalStateException("가공 결과 저장에 실패했습니다.");
		}
		
		// 7. 가공 작업을 성공 상태로 변경한다.
		if (taskMapper.markSuccess(transformTaskId) != 1) {
			throw new IllegalStateException("가공 작업 성공 상태 변경에 실패했습니다.");
		}
		
	}
	
}
