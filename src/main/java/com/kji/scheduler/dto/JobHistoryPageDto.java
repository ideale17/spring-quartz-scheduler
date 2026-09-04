package com.kji.scheduler.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JobHistoryPageDto {

    private List<JobHistoryDto> content;
    private long totalCount;
    private int page;
    private int size;
    
}