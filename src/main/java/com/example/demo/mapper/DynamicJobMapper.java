package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.dto.JobInfoDto;

@Mapper
public interface DynamicJobMapper {
	
	List<JobInfoDto> findAllJobList();
	
}
