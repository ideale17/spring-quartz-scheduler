package com.kji.scheduler.dto;

import lombok.Data;

/**
 * External API 인증 정보 수정에 필요한 데이터를 담는다.
 */
@Data
public class ExternalApiAuthDto {
	
	private String authType;
	private String authLocation;
	private String authKey;
	private String authValue;
	private String authUsername;
	private String authPassword;
	
}
