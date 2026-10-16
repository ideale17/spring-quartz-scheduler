package com.kji.scheduler.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kji.scheduler.dto.TransformTaskDto;

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
	Long claimPendingTask();
	
	/**
	 * 가공 작업 정보를 조회한다.
	 *
	 * @param transformTaskId 가공 작업 식별자
	 * @return 가공 작업 정보
	 */
	TransformTaskDto findTransformTask(Long transformTaskId);
	
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