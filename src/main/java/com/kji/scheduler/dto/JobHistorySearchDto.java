package com.kji.scheduler.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class JobHistorySearchDto {

    private String jobName;
    private String jobGroup;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    
}