package com.example.demo.controller;

import java.util.List;
import java.util.Map;

import org.quartz.SchedulerException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.JobInfoDto;
import com.example.demo.dto.ScheduleRequest;
import com.example.demo.dto.ScheduleType;
import com.example.demo.service.DynamicJobService;


@RestController
@RequestMapping("/jobs")
public class DynamicJobController {
	
	private final DynamicJobService jobService;

    public DynamicJobController(DynamicJobService jobService) {
        this.jobService = jobService;
    }

    // Job 추가 및 실행 요청
//    @PostMapping("/addJob")
//    public String addJob(@RequestParam(name = "jobClassName") String jobClassName,
//    	                 @RequestParam(name = "jobName") String jobName,
//                         @RequestParam(name = "jobGroup", defaultValue = "default") String jobGroup,
//                         @RequestParam(name = "scheduleType") ScheduleType scheduleType,
//                         @RequestParam(name = "scheduleExpr") String scheduleExpr) throws SchedulerException {
//        jobService.addJob(jobClassName, jobName, jobGroup, scheduleType, scheduleExpr);
//        return "Job 추가됨: " + jobName;
//    }
    
    @PostMapping("/addJob")
    public String addJob(@RequestBody ScheduleRequest request) throws SchedulerException {
        jobService.addJob(request);
        return "Job 추가됨: " + request.getJobName();
    }
    
    // Job 추가 실행X 요청
    @PostMapping("/addJobOnly")
    public String addJobOnly(@RequestParam(name = "jobClassName") String jobClassName,
    	                 @RequestParam(name = "jobName") String jobName,
                         @RequestParam(name = "jobGroup", defaultValue = "default") String jobGroup) throws SchedulerException {
        jobService.addJobOnly(jobClassName, jobName, jobGroup);
        return "Job 추가됨: " + jobName;
    }
    
    // 트리거 등록 및 실행 요청
    @PostMapping("/addTriggerToExistingJob")
    public String addTriggerToExistingJob(@RequestParam(name = "jobName") String jobName,
                         @RequestParam(name = "jobGroup", defaultValue = "default") String jobGroup,
                         @RequestParam(name = "scheduleType") ScheduleType scheduleType,
                         @RequestParam(name = "scheduleExpr") String scheduleExpr) throws SchedulerException {
        jobService.addTriggerToExistingJob(jobName, jobGroup, scheduleType, scheduleExpr);
        return "트리거 추가됨: " + jobName;
    }
    
    // Job 삭제 요청
    @DeleteMapping("/deleteJob")
    public String deleteJob(@RequestParam(name = "jobName") String jobName,
                            @RequestParam(name = "jobGroup", defaultValue = "default") String jobGroup) throws SchedulerException {
        boolean result = jobService.deleteJob(jobName, jobGroup);
        return result ? "Job 삭제됨: " + jobName : "삭제 실패 (Job 없음)";
    }
    
    // Job 조회
    @GetMapping("/listJobs")
    public List<JobInfoDto> listAllJobs() throws SchedulerException {
        return jobService.getAllScheduledJobs();
    }
    
}
