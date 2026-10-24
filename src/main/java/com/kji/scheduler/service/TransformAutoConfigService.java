
package com.kji.scheduler.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kji.scheduler.dto.TransformAutoConfigDto;
import com.kji.scheduler.dto.TransformAutoConfigRequestDto;
import com.kji.scheduler.mapper.ExternalApiMapper;
import com.kji.scheduler.mapper.TransformAutoConfigMapper;
import com.kji.scheduler.mapper.TransformRuleMapper;

@Service
public class TransformAutoConfigService {
	
	private final TransformAutoConfigMapper mapper;
	private final ExternalApiMapper externalApiMapper;
	private final TransformRuleMapper transformRuleMapper;
	
	public TransformAutoConfigService(
			TransformAutoConfigMapper mapper,
			ExternalApiMapper externalApiMapper,
			TransformRuleMapper transformRuleMapper) {
		this.mapper = mapper;
		this.externalApiMapper = externalApiMapper;
		this.transformRuleMapper = transformRuleMapper;
	}
	
	/**
	 * External API에 연결된 자동 가공 규칙 목록을 조회한다.
	 *
	 * @param externalApiId External API 식별자
	 * @return 자동 가공 규칙 연결 목록
	 * @throws IllegalArgumentException External API가 올바르지 않은 경우
	 */
	@Transactional(readOnly = true)
	public List<TransformAutoConfigDto> getTransformAutoConfigList(Long externalApiId) {
		
		// 1. External API 존재 여부를 확인한다.
		validateExternalApi(externalApiId);
		
		// 2. 자동 가공 규칙 연결 목록을 조회한다.
		return mapper.findTransformAutoConfigList(externalApiId);
	}
	
	/**
	 * External API의 자동 가공 규칙 연결 설정을 일괄 저장한다.
	 *
	 * @param externalApiId External API 식별자
	 * @param request 자동 가공 규칙 연결 목록
	 * @throws IllegalArgumentException 연결 정보가 올바르지 않은 경우
	 * @throws IllegalStateException 저장에 실패한 경우
	 */
	@Transactional
	public void updateTransformAutoConfigs(Long externalApiId, TransformAutoConfigRequestDto request) {
		
		// 1. External API 존재 여부를 확인한다.
		validateExternalApi(externalApiId);
		
		// 2. 저장할 연결 목록을 검증한다.
		if (request == null || request.getConfigs() == null) {
			throw new IllegalArgumentException("자동 가공 규칙 연결 정보가 없습니다.");
		}
		
		List<TransformAutoConfigDto> configs = request.getConfigs();
		Set<Long> transformRuleIds = new HashSet<>();
		
		for (TransformAutoConfigDto config : configs) {
			
			if (config == null || config.getTransformRuleId() == null || config.getTransformRuleId() < 1) {
				throw new IllegalArgumentException("가공 규칙 식별자가 올바르지 않습니다.");
			}
			
			if (!transformRuleIds.add(config.getTransformRuleId())) {
				throw new IllegalArgumentException("중복된 가공 규칙입니다. transformRuleId: " + config.getTransformRuleId());
			}
			
			if (!"Y".equals(config.getEnabled()) && !"N".equals(config.getEnabled())) {
				throw new IllegalArgumentException("자동 가공 사용 여부가 올바르지 않습니다.");
			}
			
			if (transformRuleMapper.findTransformRule(config.getTransformRuleId()) == null) {
				throw new IllegalArgumentException("가공 규칙을 찾을 수 없습니다. transformRuleId: " + config.getTransformRuleId());
			}
		}
		
		// 3. 기존 연결 설정을 삭제한다.
		mapper.deleteTransformAutoConfigs(externalApiId);
		
		// 4. 요청한 가공 규칙 연결을 새로 등록한다.
		for (TransformAutoConfigDto config : configs) {
			
			config.setExternalApiId(externalApiId);
			
			if (mapper.insertTransformAutoConfig(config) != 1) {
				throw new IllegalStateException("자동 가공 규칙 연결 저장에 실패했습니다. transformRuleId: " + config.getTransformRuleId());
			}
		}
	}
	
	/**
	 * External API 식별자와 존재 여부를 검증한다.
	 *
	 * @param externalApiId External API 식별자
	 * @throws IllegalArgumentException External API가 올바르지 않은 경우
	 */
	private void validateExternalApi(Long externalApiId) {
		
		// 1. External API 식별자를 검증한다.
		if (externalApiId == null || externalApiId < 1) {
			throw new IllegalArgumentException("External API 식별자가 올바르지 않습니다.");
		}
		
		// 2. External API 존재 여부를 확인한다.
		if (externalApiMapper.findExternalApi(externalApiId) == null) {
			throw new IllegalArgumentException("External API를 찾을 수 없습니다. externalApiId: " + externalApiId);
		}
	}
	
}
