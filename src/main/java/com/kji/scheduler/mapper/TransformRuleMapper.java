package com.kji.scheduler.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kji.scheduler.dto.TransformRuleDto;
import com.kji.scheduler.dto.TransformRuleFieldDto;
import com.kji.scheduler.dto.TransformRuleVersionDto;

@Mapper
public interface TransformRuleMapper {
	
	/**
	 * 등록된 가공 규칙 목록을 식별자 내림차순으로 조회한다.
	 *
	 * @return 가공 규칙 목록
	 */
	List<TransformRuleDto> findAllTransformRuleList();
	
	/**
	 * 가공 규칙 식별자를 조건으로 기본 정보를 조회한다.
	 *
	 * @param transformRuleId 조회 대상 가공 규칙 식별자
	 * @return 가공 규칙 정보, 없으면 null
	 */
	TransformRuleDto findTransformRule(@Param("transformRuleId") Long transformRuleId);
	
	/**
	 * 가공 규칙의 현재 버전 설정을 조회한다.
	 *
	 * @param transformRuleId 조회 대상 가공 규칙 식별자
	 * @return 현재 버전 설정, 없으면 null
	 */
	TransformRuleVersionDto findCurrentTransformRuleVersion(@Param("transformRuleId") Long transformRuleId);
	
	/**
	 * 가공 규칙 식별자를 조건으로 전체 버전 목록을 조회한다.
	 *
	 * @param transformRuleId 조회 대상 가공 규칙 식별자
	 * @return 가공 규칙 버전 목록
	 */
	List<TransformRuleVersionDto> findTransformRuleVersionList(@Param("transformRuleId") Long transformRuleId);
	
	/**
	 * 가공 규칙 버전 식별자를 조건으로 버전 정보를 조회한다.
	 *
	 * @param transformRuleVersionId 조회 대상 가공 규칙 버전 식별자
	 * @return 가공 규칙 버전 정보, 없으면 null
	 */
	TransformRuleVersionDto findTransformRuleVersion(@Param("transformRuleVersionId") Long transformRuleVersionId);
	
	/**
	 * 가공 규칙 버전에 등록된 추출 필드 목록을 조회한다.
	 *
	 * @param transformRuleVersionId 조회 대상 가공 규칙 버전 식별자
	 * @return 추출 필드 목록
	 */
	List<TransformRuleFieldDto> findTransformRuleFieldList(@Param("transformRuleVersionId") Long transformRuleVersionId);
	
	/**
	 * 가공 규칙 기본 정보를 저장한다.
	 *
	 * @param rule 저장할 가공 규칙 정보
	 * @return 저장된 행 수
	 */
	int insertTransformRule(TransformRuleDto rule);
	
	/**
	 * 가공 규칙 버전 정보를 저장한다.
	 *
	 * @param version 저장할 가공 규칙 버전 정보
	 * @return 저장된 행 수
	 */
	int insertTransformRuleVersion(TransformRuleVersionDto version);
	
	/**
	 * 가공 규칙 추출 필드 정보를 저장한다.
	 *
	 * @param field 저장할 추출 필드 정보
	 * @return 저장된 행 수
	 */
	int insertTransformRuleField(TransformRuleFieldDto field);
	
	/**
	 * 가공 규칙의 이름, 설명 및 사용 여부를 수정한다.
	 *
	 * @param rule 수정할 가공 규칙 기본 정보
	 * @return 수정된 행 수
	 */
	int updateTransformRule(TransformRuleDto rule);
	
	/**
	 * 가공 규칙의 현재 버전 번호를 변경한다.
	 *
	 * @param transformRuleId 수정 대상 가공 규칙 식별자
	 * @param currentVersion 변경할 현재 버전 번호
	 * @return 수정된 행 수
	 */
	int updateTransformRuleCurrentVersion(
			@Param("transformRuleId") Long transformRuleId,
			@Param("currentVersion") Integer currentVersion);
	
}