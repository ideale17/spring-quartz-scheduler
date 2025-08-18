package com.kji.scheduler.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kji.scheduler.dto.JobLogDto;

@Mapper
public interface JobLogMapper {
	int insertStart(JobLogDto jobLogDto);
    int updateFinish(@Param("fireInstanceId") String fireInstanceId,
                     @Param("status") String status,
                     @Param("runMillis") Long runMillis,
                     @Param("exceptionMessage") String exceptionMessage);
    int updateVetoed(@Param("fireInstanceId") String fireInstanceId);
}
