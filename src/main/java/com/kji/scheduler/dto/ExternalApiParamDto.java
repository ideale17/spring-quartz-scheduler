package com.kji.scheduler.dto;

import lombok.Data;

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