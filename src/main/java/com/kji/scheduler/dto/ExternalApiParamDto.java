package com.kji.scheduler.dto;

import lombok.Data;

/**
 * External API 요청 파라미터의 설정 정보를 담는다.
 */
@Data
public class ExternalApiParamDto {

    private Long paramId;
    private Long externalApiId;
    private String paramLocation;
    private String paramName;
    private String valueType;
    private String paramValue;
    private String valueFormat;
    private String requiredYn;
    private Integer sortOrder;
    private String description;

}
