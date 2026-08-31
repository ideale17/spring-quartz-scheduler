package com.kji.scheduler.controller;

import java.util.List;

import org.quartz.SchedulerException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kji.scheduler.dto.JobHistoryDto;
import com.kji.scheduler.dto.JobInfoDto;
import com.kji.scheduler.dto.ScheduleRequest;
import com.kji.scheduler.dto.ScheduleType;
import com.kji.scheduler.service.DynamicJobService;

import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class DynamicJobController {
	
	private final DynamicJobService dynamicJobService;
	    
    @PostMapping("/addJob")
    public ResponseEntity<String> addJob(@RequestBody ScheduleRequest request) {
    	
    	try {
    		dynamicJobService.addJob(request);
    		return ResponseEntity.ok("Job 추가됨: " + request.getJobName());
    		
		} catch (SchedulerException e) {
        	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Job 추가 실패 (Scheduler 예외): " + e.getMessage());
        	
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body("잘못된 요청: " + e.getMessage());
			
        } catch (Exception e) {
        	return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 내부 오류: " + e.getMessage());
        	
        }
    	
    }
    
    // Job 추가 실행X 요청
    @PostMapping("/addJobOnly")
    public String addJobOnly(@RequestParam(name = "jobClassName") String jobClassName,
    	                 @RequestParam(name = "jobName") String jobName,
                         @RequestParam(name = "jobGroup", defaultValue = "default") String jobGroup) throws SchedulerException {
    	dynamicJobService.addJobOnly(jobClassName, jobName, jobGroup);
        return "Job 추가됨: " + jobName;
    }
    
    // 트리거 등록 및 실행 요청
    @PostMapping("/addTriggerToExistingJob")
    public String addTriggerToExistingJob(@RequestParam(name = "jobName") String jobName,
                         @RequestParam(name = "jobGroup", defaultValue = "default") String jobGroup,
                         @RequestParam(name = "scheduleType") ScheduleType scheduleType,
                         @RequestParam(name = "scheduleExpr") String scheduleExpr) throws SchedulerException {
    	dynamicJobService.addTriggerToExistingJob(jobName, jobGroup, scheduleType, scheduleExpr);
        return "트리거 추가됨: " + jobName;
    }
    
    // Job 스케줄 수정
    @PutMapping("/updateSchedule")
    public ResponseEntity<String> updateSchedule(
            @RequestParam(name = "jobName") String jobName,
            @RequestParam(name = "jobGroup", defaultValue = "default") String jobGroup,
            @RequestParam(name = "scheduleType") ScheduleType scheduleType,
            @RequestParam(name = "scheduleExpr") String scheduleExpr) {
    	
        try {
            boolean result = dynamicJobService.updateSchedule(jobName, jobGroup, scheduleType, scheduleExpr);
            
            if (result) {
                return ResponseEntity.ok("Job 스케줄 수정됨: " + jobName);
            }
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Job 스케줄 수정 실패: " + jobName);
            
        } catch (SchedulerException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Job 스케줄 수정 실패 (Scheduler 예외): " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("잘못된 요청: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 내부 오류: " + e.getMessage());
        }
        
    }
    
    
    // Job 삭제 요청
    @DeleteMapping("/deleteJob")
    public String deleteJob(@RequestParam(name = "jobName") String jobName,
                            @RequestParam(name = "jobGroup", defaultValue = "default") String jobGroup) throws SchedulerException {
        boolean result = dynamicJobService.deleteJob(jobName, jobGroup);
        return result ? "Job 삭제됨: " + jobName : "삭제 실패 (Job 없음)";
    }
    
    // Job 조회
    @GetMapping("/listJobs")
    public List<JobInfoDto> listAllJobs() throws SchedulerException {
        return dynamicJobService.getAllScheduledJobs();
    }
    
    // 등록 가능한 Job 클래스 목록 조회
    @GetMapping("/jobClasses")
    public List<String> getAvailableJobTypes() {
        return dynamicJobService.getAvailableJobTypes();
    }
    
    // Job 중지
    @PostMapping("/pauseJob")
    public ResponseEntity<String> pauseJob(@RequestParam(name = "jobName") String jobName,
    									@RequestParam(name = "jobGroup", defaultValue = "default") String jobGroup) {
    	
    	try {
    		dynamicJobService.pauseJob(jobName, jobGroup);
    		return ResponseEntity.ok("Job 중지됨: " + jobName);
    		
        } catch (SchedulerException e) {
        	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Job 중지 실패 (Scheduler 예외): " + e.getMessage());
        	
        } catch (Exception e) {
        	return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 내부 오류: " + e.getMessage());
        	
        }
    	
    }
    
    // Job 재시작
    @PostMapping("/resumeJob")
    public ResponseEntity<String> resumeJob(@RequestParam(name = "jobName") String jobName,
    										@RequestParam(name = "jobGroup", defaultValue = "default") String jobGroup) {
    	
    	try {
    		dynamicJobService.resumeJob(jobName, jobGroup);
    		return ResponseEntity.ok("Job 재시작됨: " + jobName);
    		
        } catch (SchedulerException e) {
        	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Job 재시작 실패 (Scheduler 예외): " + e.getMessage());
        	
        } catch (Exception e) {
        	return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 내부 오류: " + e.getMessage());
        	
        }
    	
    }
    
    
    @GetMapping("/historyJobs")
    public List<JobHistoryDto> historyJobs() throws SchedulerException {
        return dynamicJobService.getJobHistory();
    }
    
}
