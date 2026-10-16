
package com.kji.scheduler.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kji.scheduler.dto.TransformTaskRequestDto;
import com.kji.scheduler.service.TransformTaskService;

@RestController
@RequestMapping("/transform-task")
public class TransformTaskController {
	
	private final TransformTaskService transformTaskService;
	
	public TransformTaskController(TransformTaskService transformTaskService) {
		this.transformTaskService = transformTaskService;
	}
	
	/**
	 * 선택한 Raw Data의 수동 가공 작업 일괄 등록 요청을 처리한다.
	 *
	 * @param request 가공 대상 목록 및 규칙 정보
	 * @return 생성된 가공 작업 식별자 목록 또는 오류 메시지를 담은 HTTP 응답
	 */
	@PostMapping("/add")
	public ResponseEntity<?> createManualTransformTasks(@RequestBody TransformTaskRequestDto request) {
		
		try {
			
			// 1. 선택한 Raw Data의 수동 가공 작업을 대기 상태로 등록한다.
			List<Long> transformTaskIds = transformTaskService.createManualTransformTasks(request);
			
			// 2. 생성된 작업 식별자 목록을 반환한다.
			return ResponseEntity.ok(transformTaskIds);
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("가공 작업 등록 중 오류가 발생했습니다.");
		}
	}
	
}
