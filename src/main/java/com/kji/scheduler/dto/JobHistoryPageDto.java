package com.kji.scheduler.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Job 실행 이력의 페이징 조회 결과를 담는다.
 */
@Data
@AllArgsConstructor
public class JobHistoryPageDto {

    private List<JobHistoryDto> content;
    private long totalCount;
    private int page;
    private int size;
    
}