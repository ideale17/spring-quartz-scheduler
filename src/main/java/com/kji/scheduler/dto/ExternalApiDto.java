package com.kji.scheduler.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ExternalApiDto {

    private Long externalApiId;
    private String apiName;
    private String apiUrl;
    private String httpMethod;
    private String enabled;
    
    private String retryEnabled;
    private Integer maxRetryCount;
    private Integer retryIntervalSec;
    
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}