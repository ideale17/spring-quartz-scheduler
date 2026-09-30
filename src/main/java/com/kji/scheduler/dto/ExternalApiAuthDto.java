package com.kji.scheduler.dto;

import lombok.Data;

/**
 * External API 인증 정보 수정 DTO.
 *
 * @author kji
 * @since 2026. 9. 30.
 */
@Data
public class ExternalApiAuthDto {
	
	private String authType;		// 인증 방식(NONE, API_KEY, BEARER, BASIC)
	private String authLocation;	// API Key 전달 위치(HEADER, QUERY)
	private String authKey;			// API Key 이름
	private String authValue;		// API Key 값 또는 Bearer Token
	private String authUsername;	// Basic Authentication 사용자명
	private String authPassword;	// Basic Authentication 비밀번호
	
}