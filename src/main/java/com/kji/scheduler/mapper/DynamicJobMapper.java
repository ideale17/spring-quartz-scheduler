package com.kji.scheduler.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kji.scheduler.dto.JobHistoryDto;
import com.kji.scheduler.dto.JobHistorySearchDto;
import com.kji.scheduler.dto.JobInfoDto;

@Mapper
public interface DynamicJobMapper {
	
	/**
	 * 등록된 Job과 트리거 정보를 Job 그룹과 이름순으로 조회한다.
	 *
	 * @return 트리거 정보를 포함한 Job 목록
	 */
	List<JobInfoDto> findAllJobList();
	
	/**
	 * 검색 조건과 페이징 정보로 Job 실행 이력을 조회한다.
	 *
	 * @param searchDto Job 실행 이력 검색 조건과 페이징 정보
	 * @return Job 실행 이력 목록
	 */
	List<JobHistoryDto> findJobHistory(JobHistorySearchDto searchDto);
	
	/**
	 * Job 이름과 그룹을 조건으로 Job과 트리거 정보를 조회한다.
	 *
	 * @param jobName 조회 대상 Job 이름
	 * @param jobGroup 조회 대상 Job 그룹
	 * @return 트리거 정보를 포함한 Job 정보, 없으면 null
	 */
	JobInfoDto findJob(
	        @Param("jobName") String jobName,
	        @Param("jobGroup") String jobGroup
	);
	
	/**
	 * 검색 조건에 맞는 Job 실행 이력의 전체 건수를 조회한다.
	 *
	 * @param searchDto Job 실행 이력 검색 조건
	 * @return 검색 조건에 맞는 전체 실행 이력 건수
	 */
	long countJobHistory(JobHistorySearchDto searchDto);
	
}
