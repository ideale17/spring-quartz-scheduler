package com.kji.scheduler.controller;

import org.quartz.SchedulerException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kji.scheduler.service.SchedulerInfoService;

@RestController
@RequestMapping("/scheduler")
public class SchedulerInfoController {
	
	private final SchedulerInfoService schedulerInfoService;
	
	public SchedulerInfoController(SchedulerInfoService schedulerInfoService) {
        this.schedulerInfoService = schedulerInfoService;
    }
	
	/**
	 * Quartz Scheduler 운영 정보 조회 요청을 처리한다.
	 *
	 * @return Quartz Scheduler 운영 정보 또는 오류 메시지를 담은 HTTP 응답
	 */
	@GetMapping("/info")
	public ResponseEntity<?> getSchedulerInfo() {
		
		try {
            return ResponseEntity.ok(schedulerInfoService.getSchedulerInfo());
        } catch (SchedulerException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Scheduler 정보 조회 실패 (Scheduler 예외): " + e.getMessage());
        }
		
	}
	
}
