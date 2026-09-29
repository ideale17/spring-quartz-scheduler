package com.kji.scheduler.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * ExternalApiDto 클래스.
 *
 * @author kji
 * @since 2026. 9. 28.
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