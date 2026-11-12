
package com.kji.scheduler.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kji.scheduler.dto.ExternalApiExecutionSnapshotDto;

/**
 * External API 실행 설정 스냅샷을 저장한다.
 */
@Mapper
public interface ExternalApiExecutionSnapshotMapper {
	
	/**
	 * 실행 설정 스냅샷을 저장한다.
	 *
	 * @param snapshotDto 실행 설정 스냅샷
	 * @return 저장된 행 수
	 */
	int insertSnapshot(ExternalApiExecutionSnapshotDto snapshotDto);
	
	/**
	 * 최초 실행 식별자로 설정 스냅샷을 조회한다.
	 *
	 * @param executionId 최초 실행 식별자
	 * @return 실행 설정 스냅샷
	 */
	ExternalApiExecutionSnapshotDto findSnapshotByExecutionId(@Param("executionId") String executionId);
	
}
