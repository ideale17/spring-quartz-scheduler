
package com.kji.scheduler.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kji.scheduler.dto.TransformAutoConfigDto;

@Mapper
public interface TransformAutoConfigMapper {
	
	/**
	 * External API에 연결된 자동 가공 규칙 목록을 조회한다.
	 *
	 * @param externalApiId External API 식별자
	 * @return 자동 가공 규칙 연결 목록
	 */
	List<TransformAutoConfigDto> findTransformAutoConfigList(@Param("externalApiId") Long externalApiId);
	
	/**
	 * External API에 연결된 자동 가공 규칙을 모두 삭제한다.
	 *
	 * @param externalApiId External API 식별자
	 * @return 삭제된 행 수
	 */
	int deleteTransformAutoConfigs(@Param("externalApiId") Long externalApiId);
	
	/**
	 * External API 자동 가공 규칙 연결을 등록한다.
	 *
	 * @param config 자동 가공 규칙 연결 정보
	 * @return 등록된 행 수
	 */
	int insertTransformAutoConfig(TransformAutoConfigDto config);
	
	/**
	 * External API의 활성 자동 가공 규칙과 현재 버전 목록을 조회한다.
	 *
	 * @param externalApiId External API 식별자
	 * @return 활성 자동 가공 규칙의 현재 버전 목록
	 */
	List<TransformAutoConfigDto> findEnabledAutoTransformRules(@Param("externalApiId") Long externalApiId);
	
}
