package com.kji.scheduler.service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kji.scheduler.dto.TransformRuleDto;
import com.kji.scheduler.dto.TransformRuleFieldDto;
import com.kji.scheduler.dto.TransformRuleRequestDto;
import com.kji.scheduler.dto.TransformRuleSearchDto;
import com.kji.scheduler.dto.TransformRuleVersionDto;
import com.kji.scheduler.mapper.TransformRuleMapper;

@Service
public class TransformRuleService {
	
	private final TransformRuleMapper mapper;
	
	public TransformRuleService(TransformRuleMapper mapper) {
		this.mapper = mapper;
	}
	
	/**
	 * 검색조건에 해당하는 가공 규칙 목록을 조회한다.
	 *
	 * @param searchDto 가공 규칙 검색조건
	 * @return 가공 규칙 목록
	 * @throws IllegalArgumentException 검색조건이 올바르지 않은 경우
	 */
	@Transactional(readOnly = true)
	public List<TransformRuleDto> getTransformRuleList(TransformRuleSearchDto searchDto) {
		
		// 1. 규칙명 검색조건을 정리하고 검증한다.
		String ruleName = searchDto.getRuleName();
		
		if (ruleName != null) {
			ruleName = ruleName.trim();
			
			if (ruleName.length() > 100) {
				throw new IllegalArgumentException("규칙명 검색조건은 100자 이하여야 합니다.");
			}
			
			searchDto.setRuleName(ruleName);
		}
		
		// 2. 사용 여부 검색조건을 검증한다.
		String enabled = searchDto.getEnabled();
		
		if (enabled != null && !enabled.isEmpty()
				&& !"Y".equals(enabled) && !"N".equals(enabled)) {
			throw new IllegalArgumentException("올바르지 않은 사용 여부 검색조건입니다.");
		}
		
		// 3. 검색조건에 해당하는 가공 규칙 목록을 조회한다.
		return mapper.findTransformRuleList(searchDto);
	}
	
	/**
	 * 가공 규칙의 현재 버전과 추출 필드를 조회한다.
	 *
	 * @param transformRuleId 가공 규칙 식별자
	 * @return 가공 규칙 상세 정보
	 */
	@Transactional(readOnly = true)
	public TransformRuleRequestDto getTransformRuleDetail(Long transformRuleId) {
		
		// 1. 가공 규칙 기본 정보를 조회한다.
		TransformRuleDto rule = getTransformRule(transformRuleId);
		
		// 2. 현재 버전 정보를 조회한다.
		TransformRuleVersionDto version = mapper.findCurrentTransformRuleVersion(transformRuleId);
		
		if (version == null) {
			throw new IllegalStateException("현재 가공 규칙 버전을 찾을 수 없습니다.");
		}
		
		// 3. 현재 버전의 추출 필드를 조회한다.
		List<TransformRuleFieldDto> fields = mapper.findTransformRuleFieldList(version.getTransformRuleVersionId());
		
		// 4. 상세 응답을 구성한다.
		TransformRuleRequestDto result = new TransformRuleRequestDto();
		result.setRule(rule);
		result.setVersion(version);
		result.setFields(fields);
		
		return result;
	}

	/**
	 * 가공 규칙의 버전 목록을 조회한다.
	 *
	 * @param transformRuleId 가공 규칙 식별자
	 * @return 가공 규칙 버전 목록
	 */
	@Transactional(readOnly = true)
	public List<TransformRuleVersionDto> getTransformRuleVersionList(Long transformRuleId) {
		
		// 1. 가공 규칙 존재 여부를 확인한다.
		getTransformRule(transformRuleId);
		
		// 2. 등록된 버전 목록을 조회한다.
		return mapper.findTransformRuleVersionList(transformRuleId);
	}
	
	/**
	 * 특정 가공 규칙 버전의 설정을 조회한다.
	 *
	 * @param transformRuleId 가공 규칙 식별자
	 * @param transformRuleVersionId 가공 규칙 버전 식별자
	 * @return 가공 규칙 버전별 상세 정보
	 */
	@Transactional(readOnly = true)
	public TransformRuleRequestDto getTransformRuleVersionDetail(Long transformRuleId, Long transformRuleVersionId) {
		
		// 1. 가공 규칙 기본 정보를 조회한다.
		TransformRuleDto rule = getTransformRule(transformRuleId);
		
		// 2. 요청한 버전이 해당 규칙에 속하는지 확인한다.
		TransformRuleVersionDto version = mapper.findTransformRuleVersion(transformRuleVersionId);
		
		if (version == null || !transformRuleId.equals(version.getTransformRuleId())) {
			throw new IllegalArgumentException("가공 규칙 버전 정보를 찾을 수 없습니다.");
		}
		
		// 3. 해당 버전의 추출 필드를 조회한다.
		List<TransformRuleFieldDto> fields = mapper.findTransformRuleFieldList(transformRuleVersionId);
		
		// 4. 버전별 상세 응답을 구성한다.
		TransformRuleRequestDto result = new TransformRuleRequestDto();
		result.setRule(rule);
		result.setVersion(version);
		result.setFields(fields);
		
		return result;
	}

	/**
	 * 가공 규칙과 최초 버전 및 추출 필드를 등록한다.
	 *
	 * @param request 가공 규칙 등록 정보
	 * @return 생성된 가공 규칙 식별자
	 */
	@Transactional
	public Long createTransformRule(TransformRuleRequestDto request) {
		
		// 1. 등록 정보를 검증하고 기본값을 설정한다.
		validateRequest(request);
		
		TransformRuleDto rule = request.getRule();
		TransformRuleVersionDto version = request.getVersion();
		
		// 2. 가공 규칙 기본 정보를 등록한다.
		rule.setTransformRuleId(null);
		rule.setCurrentVersion(1);
		
		if (mapper.insertTransformRule(rule) != 1 || rule.getTransformRuleId() == null) {
			throw new IllegalStateException("가공 규칙 기본 정보 등록에 실패했습니다.");
		}
		
		// 3. 최초 버전을 등록한다.
		version.setTransformRuleVersionId(null);
		version.setTransformRuleId(rule.getTransformRuleId());
		version.setVersionNo(1);
		
		if (mapper.insertTransformRuleVersion(version) != 1 || version.getTransformRuleVersionId() == null) {
			throw new IllegalStateException("가공 규칙 버전 등록에 실패했습니다.");
		}
		
		// 4. 최초 버전의 추출 필드를 등록한다.
		insertFields(version.getTransformRuleVersionId(), request.getFields());
		
		// 5. 생성된 가공 규칙 식별자를 반환한다.
		return rule.getTransformRuleId();
	}

	/**
	 * 가공 규칙을 수정하고 설정 변경 시 새 버전을 생성한다.
	 *
	 * @param transformRuleId 수정 대상 가공 규칙 식별자
	 * @param request 가공 규칙 수정 정보
	 */
	@Transactional
	public void updateTransformRule(Long transformRuleId, TransformRuleRequestDto request) {
		
		// 1. 수정 요청 정보를 검증한다.
		validateRequest(request);
		
		if (transformRuleId == null || transformRuleId < 1) {
			throw new IllegalArgumentException("가공 규칙 식별자가 올바르지 않습니다.");
		}
		
		// 2. 기존 가공 규칙 정보를 조회한다.
		TransformRuleDto savedRule = getTransformRule(transformRuleId);
		
		// 3. 수정 화면에서 조회한 버전과 현재 버전을 비교한다.
		Integer expectedVersion = request.getRule().getCurrentVersion();
		
		if (expectedVersion == null || !expectedVersion.equals(savedRule.getCurrentVersion())) {
			throw new IllegalStateException("가공 규칙이 변경되었거나 버전 정보가 없습니다. 다시 조회해 주세요.");
		}
		
		// 4. 현재 가공 규칙 버전과 추출 필드를 조회한다.
		TransformRuleVersionDto savedVersion = mapper.findCurrentTransformRuleVersion(transformRuleId);
		
		if (savedVersion == null) {
			throw new IllegalStateException("현재 가공 규칙 버전을 찾을 수 없습니다.");
		}
		
		List<TransformRuleFieldDto> savedFields = mapper.findTransformRuleFieldList(savedVersion.getTransformRuleVersionId());
		
		// 5. 기본 정보 변경 여부를 확인한다.
		TransformRuleDto rule = request.getRule();
		rule.setTransformRuleId(transformRuleId);
		
		boolean basicChanged =
				!Objects.equals(savedRule.getRuleName(), rule.getRuleName())
				|| !Objects.equals(savedRule.getDescription(), rule.getDescription())
				|| !Objects.equals(savedRule.getEnabled(), rule.getEnabled());
		
		if (basicChanged && mapper.updateTransformRule(rule) != 1) {
			throw new IllegalStateException("가공 규칙 기본 정보 수정에 실패했습니다.");
		}
		
		// 6. 가공 설정 변경 여부를 확인한다.
		if (!isTransformChanged(savedVersion, savedFields, request.getVersion(), request.getFields())) {
			return;
		}
		
		// 7. 새 버전 번호를 생성한다.
		int nextVersion = savedRule.getCurrentVersion() + 1;
		
		TransformRuleVersionDto version = request.getVersion();
		version.setTransformRuleVersionId(null);
		version.setTransformRuleId(transformRuleId);
		version.setVersionNo(nextVersion);
		
		// 8. 새로운 가공 규칙 버전을 등록한다.
		if (mapper.insertTransformRuleVersion(version) != 1
				|| version.getTransformRuleVersionId() == null) {
			throw new IllegalStateException("새 가공 규칙 버전 등록에 실패했습니다.");
		}
		
		// 9. 새로운 버전의 추출 필드를 등록한다.
		insertFields(version.getTransformRuleVersionId(), request.getFields());
		
		// 10. 현재 버전을 갱신한다.
		if (mapper.updateTransformRuleCurrentVersion(transformRuleId, nextVersion) != 1) {
			throw new IllegalStateException("가공 규칙 현재 버전 변경에 실패했습니다.");
		}
	}

	/**
	 * 가공 규칙 기본 정보를 조회한다.
	 *
	 * @param transformRuleId 조회 대상 가공 규칙 식별자
	 * @return 가공 규칙 기본 정보
	 */
	private TransformRuleDto getTransformRule(Long transformRuleId) {
		
		if (transformRuleId == null || transformRuleId < 1) {
			throw new IllegalArgumentException("가공 규칙 식별자가 올바르지 않습니다.");
		}
		
		TransformRuleDto rule = mapper.findTransformRule(transformRuleId);
		
		if (rule == null) {
			throw new IllegalArgumentException("가공 규칙을 찾을 수 없습니다. transformRuleId: " + transformRuleId);
		}
		
		return rule;
	}

	/**
	 * 가공 규칙과 추출 필드 정보를 검증한다.
	 *
	 * @param request 검증 대상 가공 규칙 정보
	 */
	private void validateRequest(TransformRuleRequestDto request) {
		
		// 1. 필수 요청 객체를 확인한다.
		if (request == null || request.getRule() == null || request.getVersion() == null) {
			throw new IllegalArgumentException("가공 규칙 등록 및 수정 정보가 없습니다.");
		}
		
		TransformRuleDto rule = request.getRule();
		TransformRuleVersionDto version = request.getVersion();
		
		// 2. 규칙 기본 정보를 검증한다.
		if (rule.getRuleName() == null || rule.getRuleName().isBlank()) {
			throw new IllegalArgumentException("가공 규칙명은 필수입니다.");
		}
		
		rule.setRuleName(rule.getRuleName().trim());
		
		if (rule.getRuleName().length() > 100) {
			throw new IllegalArgumentException("가공 규칙명은 100자 이하여야 합니다.");
		}
		
		if (rule.getDescription() != null && rule.getDescription().length() > 500) {
			throw new IllegalArgumentException("가공 규칙 설명은 500자 이하여야 합니다.");
		}
		
		if (rule.getEnabled() == null || rule.getEnabled().isBlank()) {
			rule.setEnabled("Y");
		}
		
		if (!"Y".equals(rule.getEnabled()) && !"N".equals(rule.getEnabled())) {
			throw new IllegalArgumentException("가공 규칙 사용 여부가 올바르지 않습니다.");
		}
		
		// 3. 데이터 형식과 반복 경로를 검증한다.
		if (!"JSON".equals(version.getDataFormat()) && !"XML".equals(version.getDataFormat())) {
			throw new IllegalArgumentException("데이터 형식은 JSON 또는 XML이어야 합니다.");
		}
		
		version.setRepeatPath(normalize(version.getRepeatPath()));
		
		if (version.getRepeatPath() != null && version.getRepeatPath().length() > 1000) {
			throw new IllegalArgumentException("반복 대상 경로는 1000자 이하여야 합니다.");
		}
		
		// 4. 추출 필드 목록을 검증한다.
		if (request.getFields() == null || request.getFields().isEmpty()) {
			throw new IllegalArgumentException("추출 필드는 최소 1개 이상 등록해야 합니다.");
		}
		
		Set<String> targetNames = new HashSet<>();
		
		for (int i = 0; i < request.getFields().size(); i++) {
			TransformRuleFieldDto field = request.getFields().get(i);
			
			if (field == null) {
				throw new IllegalArgumentException("추출 필드 정보가 올바르지 않습니다.");
			}
			
			field.setSourcePath(normalize(field.getSourcePath()));
			field.setTargetName(normalize(field.getTargetName()));
			
			if (field.getSourcePath() == null || field.getTargetName() == null) {
				throw new IllegalArgumentException("원본 경로와 출력 필드명은 필수입니다.");
			}
			
			if (field.getSourcePath().length() > 1000 || field.getTargetName().length() > 100) {
				throw new IllegalArgumentException("추출 필드 경로 또는 출력 필드명 길이가 제한을 초과했습니다.");
			}
			
			if (!targetNames.add(field.getTargetName())) {
				throw new IllegalArgumentException("중복된 출력 필드명입니다. targetName: " + field.getTargetName());
			}
			
			if (!Set.of("STRING", "INTEGER", "DECIMAL", "BOOLEAN", "DATETIME").contains(field.getDataType())) {
				throw new IllegalArgumentException("지원하지 않는 추출 필드 데이터 타입입니다.");
			}
			
			field.setValueFormat(normalize(field.getValueFormat()));
			
			if (field.getValueFormat() != null && field.getValueFormat().length() > 100) {
				throw new IllegalArgumentException("데이터 변환 형식은 100자 이하여야 합니다.");
			}
			
			if (field.getRequiredYn() == null || field.getRequiredYn().isBlank()) {
				field.setRequiredYn("N");
			}
			
			if (!"Y".equals(field.getRequiredYn()) && !"N".equals(field.getRequiredYn())) {
				throw new IllegalArgumentException("추출 필드 필수 여부가 올바르지 않습니다.");
			}
			
			// 화면 배열 순서를 출력 순서로 사용한다.
			field.setSortOrder(i);
		}
	}

	/**
	 * 가공 설정 변경 여부를 확인한다.
	 *
	 * @param savedVersion 현재 가공 규칙 버전
	 * @param savedFields 현재 추출 필드 목록
	 * @param version 수정 요청 버전 설정
	 * @param fields 수정 요청 필드 목록
	 * @return 가공 설정 변경 여부
	 */
	private boolean isTransformChanged(
			TransformRuleVersionDto savedVersion,
			List<TransformRuleFieldDto> savedFields,
			TransformRuleVersionDto version,
			List<TransformRuleFieldDto> fields) {

		// 1. 데이터 형식과 반복 경로를 비교한다.
		if (!Objects.equals(savedVersion.getDataFormat(), version.getDataFormat())
				|| !Objects.equals(normalize(savedVersion.getRepeatPath()), version.getRepeatPath())) {
			return true;
		}

		// 2. 필드 개수를 비교한다.
		if (savedFields.size() != fields.size()) {
			return true;
		}

		// 3. 필드 설정을 비교한다.
		for (int i = 0; i < fields.size(); i++) {
			TransformRuleFieldDto saved = savedFields.get(i);
			TransformRuleFieldDto changed = fields.get(i);

			if (!Objects.equals(saved.getSourcePath(), changed.getSourcePath())
					|| !Objects.equals(saved.getTargetName(), changed.getTargetName())
					|| !Objects.equals(saved.getDataType(), changed.getDataType())
					|| !Objects.equals(normalize(saved.getValueFormat()), changed.getValueFormat())
					|| !Objects.equals(saved.getRequiredYn(), changed.getRequiredYn())
					|| !Objects.equals(saved.getSortOrder(), changed.getSortOrder())) {
				return true;
			}
		}

		return false;
	}

	/**
	 * 특정 버전에 추출 필드를 등록한다.
	 *
	 * @param versionId 가공 규칙 버전 식별자
	 * @param fields 저장할 추출 필드 목록
	 */
	private void insertFields(Long versionId, List<TransformRuleFieldDto> fields) {

		// 1. 추출 필드를 순서대로 등록한다.
		for (TransformRuleFieldDto field : fields) {
			field.setTransformFieldId(null);
			field.setTransformRuleVersionId(versionId);

			if (mapper.insertTransformRuleField(field) != 1) {
				throw new IllegalStateException("가공 규칙 추출 필드 등록에 실패했습니다. targetName: " + field.getTargetName());
			}
		}
	}

	/**
	 * 공백 문자열을 null로 변환한다.
	 *
	 * @param value 변환 대상 문자열
	 * @return 정규화된 문자열
	 */
	private String normalize(String value) {
		
		if (value == null || value.isBlank()) {
			return null;
		}
		
		return value.trim();
	}
	
}