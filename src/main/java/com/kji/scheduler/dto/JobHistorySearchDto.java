package com.kji.scheduler.dto;

import java.time.LocalDate;

import lombok.Data;

/**
 * Job 실행 이력 조회를 위한 검색 조건과 페이징 정보를 담는다.
 */
@Data
public class JobHistorySearchDto {

    private String jobName;
    private String jobGroup;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate endDateExclusive;
    
    private int page = 1;
    private int size = 10;

    public int getOffset() {
        return (page - 1) * size;
    }
    
}