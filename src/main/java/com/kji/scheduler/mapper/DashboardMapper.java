package com.kji.scheduler.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kji.scheduler.dto.DashboardExecutionSummaryDto;
import com.kji.scheduler.dto.RecentFailedJobDto;

@Mapper
public interface DashboardMapper {
	
	// 오늘 실행 현황 조회
	DashboardExecutionSummaryDto findTodayExecutionSummary(
			@Param("startDateTime") LocalDateTime startDateTime,
			@Param("endDateTime") LocalDateTime endDateTime);
	
	// 최근 실패 Job 조회
	List<RecentFailedJobDto> findRecentFailedJobs();
	
}