
package com.kji.scheduler.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kji.scheduler.dto.RawDataDetailDto;
import com.kji.scheduler.dto.TransformRuleRequestDto;
import com.kji.scheduler.dto.TransformTaskDto;
import com.kji.scheduler.dto.TransformTaskHistoryDto;
import com.kji.scheduler.dto.TransformTaskRequestDto;
import com.kji.scheduler.mapper.TransformTaskMapper;

@Service
public class TransformTaskService {
	
	private final TransformTaskMapper mapper;
	private final RawDataService rawDataService;
	private final TransformRuleService transformRuleService;
	
	public TransformTaskService(TransformTaskMapper mapper, RawDataService rawDataService, TransformRuleService transformRuleService) {
		this.mapper = mapper;
		this.rawDataService = rawDataService;
		this.transformRuleService = transformRuleService;
	}
	
	/**
	 * 선택한 Raw Data의 수동 가공 작업을 대기 상태로 일괄 등록한다.
	 *
	 * @param request 가공 대상 목록 및 규칙 정보
	 * @return 등록된 가공 작업 식별자 목록
	 * @throws IllegalArgumentException 요청 정보나 가공 대상이 올바르지 않은 경우
	 * @throws IllegalStateException 가공 작업 등록에 실패한 경우
	 */
	@Transactional
	public List<Long> createManualTransformTasks(TransformTaskRequestDto request) {
		
		// 1. 요청 정보를 검증한다.
		if (request == null || request.getTransformRuleId() == null || request.getTransformRuleId() < 1) {
			throw new IllegalArgumentException("가공 규칙 식별자가 올바르지 않습니다.");
		}
		
		if (request.getRawDataIds() == null || request.getRawDataIds().isEmpty()) {
			throw new IllegalArgumentException("가공 대상 Raw Data를 선택해야 합니다.");
		}
		
		if (request.getRawDataIds().size() > 100) {
			throw new IllegalArgumentException("가공 작업은 한 번에 최대 100건까지 등록할 수 있습니다.");
		}
		
		// 2. 가공 대상 식별자와 중복 여부를 검증한다.
		Set<Long> rawDataIdSet = new HashSet<>();
		
		for (Long rawDataId : request.getRawDataIds()) {
			
			if (rawDataId == null || rawDataId < 1) {
				throw new IllegalArgumentException("Raw Data 식별자가 올바르지 않습니다.");
			}
			
			if (!rawDataIdSet.add(rawDataId)) {
				throw new IllegalArgumentException("중복된 Raw Data가 선택되었습니다. rawDataId: " + rawDataId);
			}
		}
		
		// 3. 가공 규칙의 현재 버전과 사용 여부를 확인한다.
		TransformRuleRequestDto ruleDetail = transformRuleService.getTransformRuleDetail(request.getTransformRuleId());
		
		if (!"Y".equals(ruleDetail.getRule().getEnabled())) {
			throw new IllegalArgumentException("사용 중지된 가공 규칙은 실행할 수 없습니다.");
		}
		
		String dataFormat = ruleDetail.getVersion().getDataFormat();

		if (!"JSON".equals(dataFormat) && !"XML".equals(dataFormat)) {
		    throw new IllegalArgumentException("지원하지 않는 가공 데이터 형식입니다: " + dataFormat);
		}
		
		Long transformRuleVersionId = ruleDetail.getVersion().getTransformRuleVersionId();
		
		// 4. 선택한 Raw Data의 존재 여부와 원본 응답을 검증한다.
		Long externalApiId = null;
		
		for (Long rawDataId : request.getRawDataIds()) {
			
			RawDataDetailDto rawData = rawDataService.getRawDataDetail(rawDataId);
			
			if (rawData.getResponseBody() == null || rawData.getResponseBody().isBlank()) {
				throw new IllegalArgumentException("가공할 Raw Data 원본 응답이 없습니다. rawDataId: " + rawDataId);
			}
			
			if (externalApiId == null) {
				externalApiId = rawData.getExternalApiId();
			} else if (!externalApiId.equals(rawData.getExternalApiId())) {
				throw new IllegalArgumentException("서로 다른 External API의 Raw Data는 함께 등록할 수 없습니다.");
			}
		}
		
		// 5. 동일 Raw Data와 현재 규칙 버전의 중복 가공 여부를 확인한다.
		for (Long rawDataId : request.getRawDataIds()) {
			
			long duplicateCount = mapper.countDuplicateTransformTask(rawDataId, transformRuleVersionId);
			
			if (duplicateCount > 0) {
				throw new IllegalArgumentException(
						"현재 가공 규칙 버전으로 이미 등록되었거나 가공 완료된 데이터입니다. rawDataId: " + rawDataId);
			}
		}
		
		// 6. 선택한 Raw Data별로 수동 가공 작업을 등록한다.
		List<Long> transformTaskIds = new ArrayList<>();
		
		for (Long rawDataId : request.getRawDataIds()) {
			
			TransformTaskDto task = new TransformTaskDto();
			task.setRawDataId(rawDataId);
			task.setTransformRuleVersionId(transformRuleVersionId);
			task.setRequestType("MANUAL");
			task.setStatus("PENDING");
			
			if (mapper.insertTransformTask(task) != 1 || task.getTransformTaskId() == null) {
				throw new IllegalStateException("가공 작업 등록에 실패했습니다. rawDataId: " + rawDataId);
			}
			
			transformTaskIds.add(task.getTransformTaskId());
		}
		
		// 7. 등록된 작업 식별자 목록을 반환한다.
		return transformTaskIds;
	}
	
	/**
	 * Raw Data의 전체 가공 작업 이력을 조회한다.
	 *
	 * @param rawDataId Raw Data 식별자
	 * @return 가공 작업 이력 목록
	 * @throws IllegalArgumentException Raw Data 식별자가 올바르지 않은 경우
	 */
	@Transactional(readOnly = true)
	public List<TransformTaskHistoryDto> getTransformTaskHistory(Long rawDataId) {
		
		// 1. Raw Data 식별자를 검증한다.
		if (rawDataId == null || rawDataId < 1) {
			throw new IllegalArgumentException("Raw Data 식별자가 올바르지 않습니다.");
		}
		
		// 2. Raw Data의 가공 작업 이력을 조회한다.
		return mapper.findTransformTaskHistory(rawDataId);
	}
	
}
