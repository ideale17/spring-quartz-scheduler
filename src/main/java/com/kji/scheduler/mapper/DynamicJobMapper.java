package com.kji.scheduler.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.kji.scheduler.dto.JobHistoryDto;
import com.kji.scheduler.dto.JobInfoDto;

@Mapper
public interface DynamicJobMapper {
	
	// 스케줄러 목록 조회
	List<JobInfoDto> findAllJobList();
	
	// 스케줄러 이력 조회
	List<JobHistoryDto> findJobHistory();
	
}
