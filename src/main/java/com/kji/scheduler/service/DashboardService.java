package com.kji.scheduler.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.springframework.stereotype.Service;

import com.kji.scheduler.dto.DashboardDto;
import com.kji.scheduler.dto.DashboardExecutionSummaryDto;
import com.kji.scheduler.dto.JobInfoDto;
import com.kji.scheduler.dto.RecentFailedJobDto;
import com.kji.scheduler.mapper.DashboardMapper;
import com.kji.scheduler.mapper.DynamicJobMapper;

@Service
public class DashboardService {
	
	private final Scheduler scheduler;
	private final DynamicJobMapper dynamicJobMapper;
	private final DashboardMapper dashboardMapper;
	
	public DashboardService(Scheduler scheduler, DynamicJobMapper dynamicJobMapper, DashboardMapper dashboardMapper) {
		this.scheduler = scheduler;
		this.dynamicJobMapper = dynamicJobMapper;
		this.dashboardMapper = dashboardMapper;
	}
	
	/**
	 * Job 상태별 건수, 오늘 실행 현황과 최근 실패 Job 목록을 조회한다.
	 *
	 * @return 대시보드 현황 정보
	 * @throws SchedulerException Job 트리거 상태 조회 중 Quartz 오류가 발생한 경우
	 */
	public DashboardDto getDashboard() throws SchedulerException {
		
		// 1. DB에 저장된 Job 및 Trigger 정보를 조회한다.
		List<JobInfoDto> jobList = dynamicJobMapper.findAllJobList();
		
		// 2. 전체 Job 수와 Quartz Scheduler 기준 상태별 Job 수를 계산한다.
		int totalJobCount = jobList.size();
		int normalJobCount = 0;
		int pausedJobCount = 0;
		
		for (JobInfoDto jobInfo : jobList) {
			
			Trigger.TriggerState triggerState = getSchedulerTriggerState(jobInfo);
			
			if (triggerState == Trigger.TriggerState.NORMAL) {
				normalJobCount++;
			} else if (triggerState == Trigger.TriggerState.PAUSED) {
				pausedJobCount++;
			}
		}
		
		// 3. 오늘 실행 현황 조회 범위를 계산한다.
		LocalDateTime todayStart = LocalDate.now().atStartOfDay();
		LocalDateTime tomorrowStart = todayStart.plusDays(1);
		
		// 4. 오늘 실행 현황을 조회한다.
		DashboardExecutionSummaryDto executionSummary = dashboardMapper.findTodayExecutionSummary(todayStart, tomorrowStart);
		
		// 5. 최근 실패 Job 목록을 조회한다.
		List<RecentFailedJobDto> recentFailedJobs = dashboardMapper.findRecentFailedJobs();
		
		// 6. 조회한 정보를 Dashboard 응답 DTO에 설정한다.
		DashboardDto dashboard = new DashboardDto();
		
		dashboard.setTotalJobCount(totalJobCount);
		dashboard.setNormalJobCount(normalJobCount);
		dashboard.setPausedJobCount(pausedJobCount);
		
		dashboard.setTodayExecutionCount(executionSummary.getTotalCount());
		dashboard.setTodaySuccessCount(executionSummary.getSuccessCount());
		dashboard.setTodayFailedCount(executionSummary.getFailedCount());
		
		dashboard.setRecentFailedJobs(recentFailedJobs);
		
		return dashboard;
	}
	
	/**
	 * Quartz Scheduler 기준으로 Job의 트리거 상태를 조회한다.
	 *
	 * @param jobInfo 트리거 상태를 조회할 Job 정보
	 * @return 트리거 상태, 트리거가 없으면 NONE
	 * @throws SchedulerException 트리거 상태 조회 중 Quartz 오류가 발생한 경우
	 */
	private Trigger.TriggerState getSchedulerTriggerState(JobInfoDto jobInfo) throws SchedulerException {
		
		// 1. Trigger가 없는 Job은 NONE 상태로 처리한다.
		if (jobInfo.getTriggerName() == null || jobInfo.getTriggerGroup() == null) {
			return Trigger.TriggerState.NONE;
		}
		
		// 2. TriggerKey로 Quartz Scheduler의 논리적인 Trigger 상태를 조회한다.
		TriggerKey triggerKey = TriggerKey.triggerKey(jobInfo.getTriggerName(), jobInfo.getTriggerGroup());
		
		return scheduler.getTriggerState(triggerKey);
	}
	
}
