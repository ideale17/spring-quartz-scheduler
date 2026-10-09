package com.kji.scheduler.dto;

import lombok.Data;

/**
 * 회원가입 요청 정보를 담는다.
 */
@Data
public class SignupRequest {
	
    private String username;
    private String password;
    private String passwordConfirm;
    
}