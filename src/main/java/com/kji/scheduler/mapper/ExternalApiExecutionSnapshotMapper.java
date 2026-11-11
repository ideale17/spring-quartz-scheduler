
package com.kji.scheduler.mapper;

import org.apache.ibatis.annotations.Mapper;

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
	
}
