package com.kji.scheduler.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kji.scheduler.dto.TransformTaskDto;
import com.kji.scheduler.dto.TransformTaskHistoryDto;

@Mapper
public interface TransformTaskMapper {
	
	/**
	 * Raw Data 가공 작업을 등록한다.
	 *
	 * @param task 등록할 가공 작업 정보
	 * @return 등록된 행 수
	 */
	int insertTransformTask(TransformTaskDto task);
	
	/**
	 * 대기 중인 가공 작업 한 건을 선점한다.
	 *
	 * @return 선점된 가공 작업 ID, 없으면 null
	 */
	List<TransformTaskDto> claimPendingTasks(int batchSize);
	
	/**
	 * 가공 작업 정보를 조회한다.
	 *
	 * @param transformTaskId 가공 작업 식별자
	 * @return 가공 작업 정보
	 */
	TransformTaskDto findTransformTask(Long transformTaskId);
	
	/**
	 * Raw Data 식별자로 전체 가공 작업 이력을 조회한다.
	 *
	 * @param rawDataId Raw Data 식별자
	 * @return 가공 작업 이력 목록
	 */
	List<TransformTaskHistoryDto> findTransformTaskHistory(@Param("rawDataId") Long rawDataId);
	
	/**
	 * 동일한 Raw Data와 규칙 버전의 중복 작업 건수를 조회한다.
	 *
	 * @param rawDataId Raw Data 식별자
	 * @param transformRuleVersionId 가공 규칙 버전 식별자
	 * @return 대기, 실행 중 또는 성공한 작업 건수
	 */
	long countDuplicateTransformTask(
			@Param("rawDataId") Long rawDataId,
			@Param("transformRuleVersionId") Long transformRuleVersionId);
	
	/**
	 * 가공 작업을 성공 상태로 변경한다.
	 *
	 * @param transformTaskId 가공 작업 식별자
	 * @return 변경된 행 수
	 */
	int markSuccess(Long transformTaskId);
	
	/**
	 * 가공 작업을 실패 상태로 변경한다.
	 *
	 * @param transformTaskId 가공 작업 식별자
	 * @param errorMessage 실패 원인
	 * @return 변경된 행 수
	 */
	int markFailed(@Param("transformTaskId") Long transformTaskId, @Param("errorMessage") String errorMessage);
	
}