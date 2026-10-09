package com.kji.scheduler.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kji.scheduler.dto.JobLogDto;

@Mapper
public interface JobLogMapper {
	
	/**
	 * Quartz Job 실행 시작 정보를 DB에 저장한다.
	 *
	 * @param jobLogDto 저장할 Job 실행 이력 정보
	 * @return 저장된 행 수
	 */
	int insertStart(JobLogDto jobLogDto);
	
	/**
	 * Quartz 실행 인스턴스 식별자를 조건으로 DB 실행 이력의 종료 상태, 소요 시간과 오류 메시지를 수정한다.
	 *
	 * @param fireInstanceId 수정 대상 Quartz 실행 인스턴스 식별자
	 * @param status 실행 종료 상태
	 * @param runMillis 실행 소요 시간(밀리초)
	 * @param exceptionMessage 오류 메시지
	 * @return 수정된 행 수
	 */
    int updateFinish(@Param("fireInstanceId") String fireInstanceId,
                     @Param("status") String status,
                     @Param("runMillis") Long runMillis,
                     @Param("exceptionMessage") String exceptionMessage);
    
	/**
	 * Quartz 실행 인스턴스 식별자를 조건으로 DB 실행 이력의 상태를 실행 거부로 수정한다.
	 *
	 * @param fireInstanceId 수정 대상 Quartz 실행 인스턴스 식별자
	 * @return 수정된 행 수
	 */
    int updateVetoed(@Param("fireInstanceId") String fireInstanceId);
}
