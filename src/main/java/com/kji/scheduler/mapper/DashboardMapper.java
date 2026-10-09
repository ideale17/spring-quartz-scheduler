package com.kji.scheduler.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kji.scheduler.dto.DashboardExecutionSummaryDto;
import com.kji.scheduler.dto.RecentFailedJobDto;

@Mapper
public interface DashboardMapper {
	
	/**
	 * 지정한 실행 시간 범위의 전체 실행 건수와 성공·실패 건수를 조회한다.
	 *
	 * @param startDateTime 조회 시작 시각(포함)
	 * @param endDateTime 조회 종료 시각(미포함)
	 * @return 전체 실행 건수와 성공·실패 건수
	 */
	DashboardExecutionSummaryDto findTodayExecutionSummary(
			@Param("startDateTime") LocalDateTime startDateTime,
			@Param("endDateTime") LocalDateTime endDateTime);
	
	/**
	 * 최근 실패한 Job 실행 이력을 실행 시각과 이력 식별자 내림차순으로 최대 5건 조회한다.
	 *
	 * @return 최근 실패한 Job 실행 이력 목록
	 */
	List<RecentFailedJobDto> findRecentFailedJobs();
	
}
