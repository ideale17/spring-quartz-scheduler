package com.kji.scheduler.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.kji.scheduler.dto.DashboardExecutionSummaryDto;
import com.kji.scheduler.dto.RecentFailedJobDto;

@Mapper
public interface DashboardMapper {
	
	// 오늘 실행 현황 조회
	DashboardExecutionSummaryDto findTodayExecutionSummary();
	
	// 최근 실패 Job 조회
	List<RecentFailedJobDto> findRecentFailedJobs();
	
}