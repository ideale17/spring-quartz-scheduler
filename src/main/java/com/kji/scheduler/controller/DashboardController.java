package com.kji.scheduler.controller;

import org.quartz.SchedulerException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kji.scheduler.service.DashboardService;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {
	
	private final DashboardService dashboardService;
	
	public DashboardController(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}
	
	/**
	 * Dashboard 운영 정보를 조회한다.
	 *
	 * @return Dashboard 운영 정보 조회 결과
	 */
	@GetMapping("/info")
	public ResponseEntity<?> getDashboardInfo() {
		
		try {
			return ResponseEntity.ok(dashboardService.getDashboard());
			
		} catch (SchedulerException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Dashboard 정보 조회 실패 (Scheduler 예외): " + e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Dashboard 정보 조회 실패: " + e.getMessage());
		}
		
	}
	
}