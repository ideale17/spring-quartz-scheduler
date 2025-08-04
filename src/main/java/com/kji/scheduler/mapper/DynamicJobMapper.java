package com.kji.scheduler.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.kji.scheduler.dto.JobInfoDto;

@Mapper
public interface DynamicJobMapper {
	
	List<JobInfoDto> findAllJobList();
	
}
