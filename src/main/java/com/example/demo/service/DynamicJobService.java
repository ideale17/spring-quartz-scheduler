package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;

import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.example.demo.dto.JobInfoDto;
import com.example.demo.dto.ScheduleRequest;
import com.example.demo.dto.ScheduleType;
import com.example.demo.mapper.DynamicJobMapper;
import com.example.demo.repository.JobClassRegistry;

@Service
public class DynamicJobService {
	
	private final Scheduler scheduler;
	private final JobClassRegistry jobClassRegistry;
	
	public DynamicJobService(@Qualifier("scheduler")Scheduler scheduler, @Qualifier("jobClassRegistry")JobClassRegistry jobClassRegistry) {
        this.scheduler = scheduler;
        this.jobClassRegistry = jobClassRegistry;
    }
	
	@Autowired
	private DynamicJobMapper dynamicJobMapper;
	
	/**
	 * Quartz 스케줄러에 새 Job 및 Trigger를 등록한다.
	 *
	 * 이미 동일한 JobKey(name + group)가 존재하면 {@link SchedulerException}을
	 * 발생시켜 중복 등록을 방지한다.
	 *
	 * @param jobClassName   등록할 Job 클래스의 단순 이름(예: "EmailJob")
	 * @param jobName     Quartz JobKey 의 name
	 * @param jobGroup   Quartz JobKey 의 group
	 * @param scheduleType   스케줄 방식(CRON, SIMPLE)
	 * @param scheduleExpr   스케줄 식
	 *                       ‑ CRON  →  크론 표현식(예: "0 0/10 * * * ?")
	 *                       ‑ SIMPLE → 반복 간격(초, 예: "5")
	 * @throws SchedulerException  Job 중복,등록 실패 등 Quartz 예외
	 * @throws IllegalArgumentException 잘못된 Cron 또는 스케줄 타입
	 */
	//public void addJob(String jobClassName, String jobName, String jobGroup, ScheduleType scheduleType, String scheduleExpr) throws SchedulerException {
	public void addJob(ScheduleRequest request) throws SchedulerException {
		
		JobDataMap dataMap = new JobDataMap(request.getParams());
		
		String jobClassName = request.getJobClassName();
		String jobName = request.getJobName();
		String jobGroup = request.getJobGroup();
		ScheduleType scheduleType = request.getScheduleType();
		String scheduleExpr = request.getScheduleExpr();
		
		
		// 1. Job 클래스 조회(미등록 시 예외)
        Class<? extends Job> jobClass = jobClassRegistry.getJobClass(jobClassName);
		
        // 2. JobDetail 생성
        JobDetail jobDetail = JobBuilder.newJob(jobClass)
                .withIdentity(jobName, jobGroup)
                .usingJobData(dataMap)
                .build();
        
        
        // 3. Trigger 생성
        Trigger trigger;
        
        if (scheduleType == ScheduleType.CRON) {
        	
        	// Cron 표현식 유효성 검사
            if (!CronExpression.isValidExpression(scheduleExpr)) {
                throw new IllegalArgumentException("유효하지 않은 Cron 표현식입니다: " + scheduleExpr);
            }

            trigger = TriggerBuilder.newTrigger()
                .withIdentity(jobName + "Trigger", jobGroup)
                .withSchedule(CronScheduleBuilder.cronSchedule(scheduleExpr))
                .forJob(jobDetail)
                .build();
            
        }else if(scheduleType == ScheduleType.SIMPLE) {
        	
        	//초 -> 밀리초 변경
        	long intervalMillis = Integer.parseInt(scheduleExpr) * 1000L;
        	
        	trigger = TriggerBuilder.newTrigger()
                    .withIdentity(jobName + "Trigger", jobGroup)
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                    .withIntervalInMilliseconds(intervalMillis)
                    .repeatForever())
                    .forJob(jobDetail)
                    .build();
        	
        } else {
            throw new IllegalArgumentException("지원하지 않는 스케줄 형식입니다: " + scheduleType);
        }
        
        if (scheduler.checkExists(jobDetail.getKey())) {
            throw new SchedulerException("동일한 이름의 Job이 이미 존재합니다: " + jobDetail.getKey());
        }
        
        scheduler.scheduleJob(jobDetail, trigger);
        
    }
	
	public void addJobOnly(String jobClassName, String jobName, String jobGroup) throws SchedulerException {

		// 1. Job 클래스 찾기 (등록되어 있어야 함)
		Class<? extends Job> jobClass = jobClassRegistry.getJobClass(jobClassName);

		// 2. JobDetail 생성 (Durably로 설정: Trigger 없이도 등록 가능)
		JobDetail jobDetail = JobBuilder.newJob(jobClass)
				.withIdentity(jobName, jobGroup)
				.storeDurably(true)  // Trigger 없이 저장하려면 필수
				.build();

		// 3. 중복 여부 확인
		if (scheduler.checkExists(jobDetail.getKey())) {
			throw new SchedulerException("동일한 이름의 Job이 이미 존재합니다: " + jobDetail.getKey());
		}

		// 4. Trigger 없이 Job만 DB에 등록
		scheduler.addJob(jobDetail, false);
	}
	
	public void addTriggerToExistingJob(String jobName, String jobGroup, ScheduleType scheduleType, String scheduleExpr) throws SchedulerException {
		
		JobKey jobKey = new JobKey(jobName, jobGroup);
		
		if (!scheduler.checkExists(jobKey)) {
			throw new SchedulerException("존재하지 않는 Job입니다: " + jobKey);
		}
		
		Trigger trigger;
		
		if (scheduleType == ScheduleType.CRON) {
			
			if (!CronExpression.isValidExpression(scheduleExpr)) {
				throw new IllegalArgumentException("잘못된 Cron 식입니다.");
			}
			
			trigger = TriggerBuilder.newTrigger()
					.withIdentity(jobName + "Trigger", jobGroup)
					.forJob(jobKey)
					.withSchedule(CronScheduleBuilder.cronSchedule(scheduleExpr))
					.build();
			
			} else if (scheduleType == ScheduleType.SIMPLE) {
				
				long intervalMillis = Long.parseLong(scheduleExpr) * 1000L;
				
				trigger = TriggerBuilder.newTrigger()
						.withIdentity(jobName + "Trigger", jobGroup)
						.forJob(jobKey)
						.withSchedule(SimpleScheduleBuilder.simpleSchedule()
									.withIntervalInMilliseconds(intervalMillis)
									.repeatForever())
						.build();
				
			} else {
				throw new IllegalArgumentException("지원하지 않는 스케줄 타입입니다.");
			}
		
			scheduler.scheduleJob(trigger);
	}

	
	// Job 삭제
    public boolean deleteJob(String jobName, String jobGroup) throws SchedulerException {
        JobKey jobKey = new JobKey(jobName, jobGroup);
        return scheduler.deleteJob(jobKey);
    }
    
    // Job 조회
    public List<JobInfoDto> getAllScheduledJobs_bak() throws SchedulerException {
    	
        List<JobInfoDto> jobList = new ArrayList<>();
        
        for (String jobGroup : scheduler.getJobGroupNames()) {
            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(jobGroup))) {
            	
                JobInfoDto JobInfoDto = new JobInfoDto();
                JobInfoDto.setJobName(jobKey.getName());
                JobInfoDto.setJobGroup(jobKey.getGroup());
                
                List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
                
                if (!triggers.isEmpty()) {
                    Trigger trigger = triggers.get(0);
                    
                    JobInfoDto.setTriggerType(trigger.getClass().getSimpleName());
                    //JobInfoDto.setNextFireTime(trigger.getNextFireTime());
                    //JobInfoDto.setPrevFireTime(trigger.getPreviousFireTime());
                    JobInfoDto.setState(scheduler.getTriggerState(trigger.getKey()).name());
                    
                }

                jobList.add(JobInfoDto);
            }
        }
        return jobList;
    }
    
    // Job 목록 조회
    public List<JobInfoDto> getAllScheduledJobs() {
    	return dynamicJobMapper.findAllJobList();
    }
    
}
