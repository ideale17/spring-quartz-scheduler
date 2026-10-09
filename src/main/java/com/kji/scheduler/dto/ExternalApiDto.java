package com.kji.scheduler.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * External API 관리와 실행에 필요한 설정 정보를 담는다.
 */
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
    
    private String authType;
    private String authLocation;
    private String authKey;
    private String authValue;
    private String authUsername;
    private String authPassword;
    
    private Boolean authValueConfigured;
    private Boolean authPasswordConfigured;
    
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
