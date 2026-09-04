package com.kji.scheduler.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kji.scheduler.dto.JobHistoryDto;
import com.kji.scheduler.dto.JobHistorySearchDto;
import com.kji.scheduler.dto.JobInfoDto;

@Mapper
public interface DynamicJobMapper {
	
	// 스케줄러 목록 조회
	List<JobInfoDto> findAllJobList();
	
	// 스케줄러 이력 조회
	List<JobHistoryDto> findJobHistory(JobHistorySearchDto searchDto);
	
	// 스케줄러 단건 조회
	JobInfoDto findJob(
	        @Param("jobName") String jobName,
	        @Param("jobGroup") String jobGroup
	);
	
	long countJobHistory(JobHistorySearchDto searchDto);
	
}
