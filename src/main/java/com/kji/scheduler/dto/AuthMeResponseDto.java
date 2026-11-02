package com.kji.scheduler.dto;

import java.util.List;
import lombok.Data;

/**
 * 현재 로그인한 사용자의 이름과 권한 목록을 담는다.
 */
@Data
public class AuthMeResponseDto {
	
    private String username;
    private List<String> roles;
    
}
