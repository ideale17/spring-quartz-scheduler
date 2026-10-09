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

import com.kji.scheduler.dto.CreateJobRequest;
import com.kji.scheduler.dto.JobHistoryPageDto;
import com.kji.scheduler.dto.JobHistorySearchDto;
import com.kji.scheduler.dto.JobInfoDto;
import com.kji.scheduler.dto.JobBatchRequest;
import com.kji.scheduler.dto.JobBatchResponse;
import com.kji.scheduler.dto.ScheduleType;
import com.kji.scheduler.dto.UpdateJobRequest;
import com.kji.scheduler.service.DynamicJobService;

import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class DynamicJobController {
	
	private final DynamicJobService dynamicJobService;
	    
    /**
     * Quartz Job을 등록한다.
     *
     * @param request Job 등록 요청 정보
     * @return Job 등록 처리 결과
     */
    @PostMapping("/add")
    public ResponseEntity<String> addJob(@RequestBody CreateJobRequest request) {
    	
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
    
    /**
     * Quartz Job을 트리거 없이 등록한다.
     *
     * @param jobClassName 등록할 Job 클래스 이름
     * @param jobName Job 이름
     * @param jobGroup Job 그룹
     * @return Job 등록 처리 결과
     * @throws SchedulerException Job 등록 중 Quartz 오류가 발생한 경우
     */
    @PostMapping("/add-only")
    public String addJobOnly(@RequestParam(name = "jobClassName") String jobClassName,
    	                 @RequestParam(name = "jobName") String jobName,
                         @RequestParam(name = "jobGroup", defaultValue = "default") String jobGroup) throws SchedulerException {
    	dynamicJobService.addJobOnly(jobClassName, jobName, jobGroup);
        return "Job 추가됨: " + jobName;
    }
    
	/**
	 * 등록된 Quartz Job에 트리거를 추가한다.
	 *
	 * @param jobName Job 이름
	 * @param jobGroup Job 그룹
	 * @param scheduleType 스케줄 유형
	 * @param scheduleExpr 스케줄 표현식
	 * @return 트리거 등록 처리 결과
	 * @throws SchedulerException 트리거 등록 중 Quartz 오류가 발생한 경우
	 */
    @PostMapping("/add-trigger")
    public String addTriggerToExistingJob(@RequestParam(name = "jobName") String jobName,
                         @RequestParam(name = "jobGroup", defaultValue = "default") String jobGroup,
                         @RequestParam(name = "scheduleType") ScheduleType scheduleType,
                         @RequestParam(name = "scheduleExpr") String scheduleExpr) throws SchedulerException {
    	dynamicJobService.addTriggerToExistingJob(jobName, jobGroup, scheduleType, scheduleExpr);
        return "트리거 추가됨: " + jobName;
    }
    
    /**
     * 등록된 Quartz Job의 스케줄을 수정한다.
     *
     * @param request Job 스케줄 수정 요청 정보
     * @return Job 스케줄 수정 처리 결과
     */
    @PutMapping("/update")
    public ResponseEntity<String> updateJob(@RequestBody UpdateJobRequest request) {

        try {
            boolean result = dynamicJobService.updateSchedule(request);

            if (result) {
                return ResponseEntity.ok("Job 스케줄 수정됨: " + request.getJobName());
            }

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Job 스케줄 수정 실패: " + request.getJobName());

        } catch (SchedulerException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Job 스케줄 수정 실패 (Scheduler 예외): " + e.getMessage());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("잘못된 요청: " + e.getMessage());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 내부 오류: " + e.getMessage());
        }
    }
    
    
	/**
	 * 등록된 Quartz Job을 삭제한다.
	 *
	 * @param jobName Job 이름
	 * @param jobGroup Job 그룹
	 * @return Job 삭제 처리 결과
	 * @throws SchedulerException Job 삭제 중 Quartz 오류가 발생한 경우
	 */
    @DeleteMapping("/delete")
    public String deleteJob(@RequestParam(name = "jobName") String jobName,
                            @RequestParam(name = "jobGroup", defaultValue = "default") String jobGroup) throws SchedulerException {
        boolean result = dynamicJobService.deleteJob(jobName, jobGroup);
        return result ? "Job 삭제됨: " + jobName : "삭제 실패 (Job 없음)";
    }
    
	/**
	 * 등록된 Quartz Job 목록을 조회한다.
	 *
	 * @return 등록된 Job 목록
	 * @throws SchedulerException Job 목록 조회 중 Quartz 오류가 발생한 경우
	 */
    @GetMapping("/list")
    public List<JobInfoDto> listAllJobs() throws SchedulerException {
        return dynamicJobService.getAllScheduledJobs();
    }
    
    
	/**
	 * 등록된 Quartz Job의 상세 정보를 조회한다.
	 *
	 * @param jobName Job 이름
	 * @param jobGroup Job 그룹
	 * @return Job 상세 정보 조회 결과
	 */
    @GetMapping("/detail")
    public ResponseEntity<?> getJob(
            @RequestParam(name = "jobName") String jobName,
            @RequestParam(name = "jobGroup", defaultValue = "default") String jobGroup) {

        try {
            return ResponseEntity.ok(dynamicJobService.getScheduledJob(jobName, jobGroup));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 내부 오류: " + e.getMessage());
        }
    }
    
	/**
	 * 등록 가능한 Quartz Job 클래스 목록을 조회한다.
	 *
	 * @return 등록 가능한 Job 클래스 이름 목록
	 */
    @GetMapping("/job-classes")
    public List<String> getAvailableJobTypes() {
        return dynamicJobService.getAvailableJobTypes();
    }
    
	/**
	 * Quartz Job의 즉시 실행을 요청한다.
	 *
	 * @param jobName Job 이름
	 * @param jobGroup Job 그룹
	 * @return Job 즉시 실행 요청 처리 결과
	 */
    @PostMapping("/run")
    public ResponseEntity<String> runJob(@RequestParam(name = "jobName") String jobName,
                                        @RequestParam(name = "jobGroup", defaultValue = "default") String jobGroup) {
    	
        try {
            dynamicJobService.runJob(jobName, jobGroup);
            return ResponseEntity.ok("Job 즉시 실행 요청됨: " + jobName);
            
        } catch (SchedulerException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Job 즉시 실행 실패 (Scheduler 예외): " + e.getMessage());
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 내부 오류: " + e.getMessage());
        }
    }
    
	/**
	 * 여러 Quartz Job의 즉시 실행을 일괄 요청한다.
	 *
	 * @param request Job 일괄 즉시 실행 요청 정보
	 * @return Job 일괄 즉시 실행 요청 처리 결과
	 */
    @PostMapping("/run-batch")
    public ResponseEntity<?> runJobs(@RequestBody JobBatchRequest request) {
    	
    	try {
    		JobBatchResponse response = dynamicJobService.runJobs(request);
    		return ResponseEntity.ok(response);
    		
    	} catch (IllegalArgumentException e) {
    		return ResponseEntity.badRequest().body("잘못된 요청: " + e.getMessage());
    		
    	} catch (Exception e) {
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 내부 오류: " + e.getMessage());
    	}
    }
    
	/**
	 * 중지된 Quartz Job의 스케줄을 재개한다.
	 *
	 * @param jobName Job 이름
	 * @param jobGroup Job 그룹
	 * @return Job 스케줄 재개 처리 결과
	 */
    @PostMapping("/resume")
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
    
	/**
	 * 여러 Quartz Job의 스케줄을 일괄 재개한다.
	 *
	 * @param request Job 스케줄 일괄 재개 요청 정보
	 * @return Job 스케줄 일괄 재개 처리 결과
	 */
    @PostMapping("/resume-batch")
    public ResponseEntity<?> resumeJobs(@RequestBody JobBatchRequest request) {
    	
    	try {
    		JobBatchResponse response = dynamicJobService.resumeJobs(request);
    		return ResponseEntity.ok(response);
    		
    	} catch (IllegalArgumentException e) {
    		return ResponseEntity.badRequest().body("잘못된 요청: " + e.getMessage());
    		
    	} catch (Exception e) {
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
    				.body("서버 내부 오류: " + e.getMessage());
    	}
    }
    
	/**
	 * Quartz Job의 스케줄을 중지한다.
	 *
	 * @param jobName Job 이름
	 * @param jobGroup Job 그룹
	 * @return Job 스케줄 중지 처리 결과
	 */
    @PostMapping("/pause")
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
    
	/**
	 * 여러 Quartz Job의 스케줄을 일괄 중지한다.
	 *
	 * @param request Job 스케줄 일괄 중지 요청 정보
	 * @return Job 스케줄 일괄 중지 처리 결과
	 */
    @PostMapping("/pause-batch")
    public ResponseEntity<?> pauseJobs(@RequestBody JobBatchRequest request) {
    	
    	try {
    		JobBatchResponse response = dynamicJobService.pauseJobs(request);
    		return ResponseEntity.ok(response);
    		
    	} catch (IllegalArgumentException e) {
    		return ResponseEntity.badRequest().body("잘못된 요청: " + e.getMessage());
    		
    	} catch (Exception e) {
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
    				.body("서버 내부 오류: " + e.getMessage());
    	}
    }
    
	/**
	 * Quartz Job의 실행 이력을 조회한다.
	 *
	 * @param searchDto Job 실행 이력 검색 조건
	 * @return 페이지 정보를 포함한 Job 실행 이력
	 * @throws SchedulerException Job 실행 이력 조회 중 Quartz 오류가 발생한 경우
	 */
    @GetMapping("/history")
    public JobHistoryPageDto historyJobs(JobHistorySearchDto searchDto) throws SchedulerException {
        return dynamicJobService.getJobHistory(searchDto);
    }
    
}
