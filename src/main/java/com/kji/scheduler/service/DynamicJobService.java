package com.kji.scheduler.service;

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
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.stereotype.Service;

import com.kji.scheduler.dto.CreateJobRequest;
import com.kji.scheduler.dto.JobHistoryDto;
import com.kji.scheduler.dto.JobHistoryPageDto;
import com.kji.scheduler.dto.JobHistorySearchDto;
import com.kji.scheduler.dto.JobInfoDto;
import com.kji.scheduler.dto.ScheduleType;
import com.kji.scheduler.dto.UpdateJobRequest;
import com.kji.scheduler.mapper.DynamicJobMapper;
import com.kji.scheduler.repository.JobClassRegistry;

@Service
public class DynamicJobService {
	
	private final Scheduler scheduler;
	private final JobClassRegistry jobClassRegistry;
	private final DynamicJobMapper dynamicJobMapper;
	
	public DynamicJobService(Scheduler scheduler, JobClassRegistry jobClassRegistry, DynamicJobMapper dynamicJobMapper) {
        this.scheduler = scheduler;
        this.jobClassRegistry = jobClassRegistry;
        this.dynamicJobMapper = dynamicJobMapper;
    }
	
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
	public void addJob(CreateJobRequest request) throws SchedulerException {
		
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
        	long intervalMillis = Long.parseLong(scheduleExpr) * 1000L;
        	
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
	
	// Job 스케줄 수정
	public boolean updateSchedule(UpdateJobRequest request) throws SchedulerException {
		
		String jobName = request.getJobName();
	    String jobGroup = request.getJobGroup();
	    ScheduleType scheduleType = request.getScheduleType();
	    String scheduleExpr = request.getScheduleExpr();
	    
	    JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
	    TriggerKey triggerKey = TriggerKey.triggerKey(jobName + "Trigger", jobGroup);
	    
	    // 1. Job 존재 여부 확인
	    if (!scheduler.checkExists(jobKey)) {
	        throw new SchedulerException("수정할 Job이 존재하지 않습니다: " + jobName + "/" + jobGroup);
	    }
	    
	    // 2. Trigger 존재 여부 확인
	    Trigger oldTrigger = scheduler.getTrigger(triggerKey);
	    
	    if (oldTrigger == null) {
	        throw new SchedulerException("수정할 Trigger가 존재하지 않습니다: " + triggerKey);
	    }
	    
	    // 3. 새 Trigger 생성
	    Trigger newTrigger;
	    
	    if (scheduleType == ScheduleType.CRON) {
	    	
	        if (!CronExpression.isValidExpression(scheduleExpr)) {
	            throw new IllegalArgumentException("유효하지 않은 Cron 표현식입니다: " + scheduleExpr);
	        }
	        
	        newTrigger = TriggerBuilder.newTrigger()
	                .withIdentity(triggerKey)
	                .forJob(jobKey)
	                .withSchedule(CronScheduleBuilder.cronSchedule(scheduleExpr))
	                .build();
	        
	    } else if (scheduleType == ScheduleType.SIMPLE) {
	    	
	        long intervalSeconds;
	        
	        try {
	            intervalSeconds = Long.parseLong(scheduleExpr);
	        } catch (NumberFormatException e) {
	            throw new IllegalArgumentException("SIMPLE 스케줄 값은 숫자여야 합니다: " + scheduleExpr);
	        }
	        
	        if (intervalSeconds <= 0) {
	            throw new IllegalArgumentException("SIMPLE 스케줄 값은 0보다 커야 합니다.");
	        }
	        
	        newTrigger = TriggerBuilder.newTrigger()
	                .withIdentity(triggerKey)
	                .forJob(jobKey)
	                .withSchedule(
	                        SimpleScheduleBuilder.simpleSchedule()
	                                .withIntervalInMilliseconds(intervalSeconds * 1000L)
	                                .repeatForever()
	                )
	                .build();
	        
	    } else {
	        throw new IllegalArgumentException(
	                "지원하지 않는 스케줄 타입입니다: " + scheduleType
	        );
	    }
	    
	    // 4. 기존 Trigger를 새 Trigger로 교체
	    return scheduler.rescheduleJob(triggerKey, newTrigger) != null;
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
    public List<JobInfoDto> getAllScheduledJobs() throws SchedulerException {
    	
    	// 1. DB에 저장된 Job 및 Trigger 정보를 조회한다.
        List<JobInfoDto> jobList = dynamicJobMapper.findAllJobList();
        
        // 2. Quartz Scheduler API 기준 Trigger 상태를 추가한다.
        for (JobInfoDto jobInfo : jobList) {
            setSchedulerTriggerState(jobInfo);
        }
        
        return jobList;
        
    }
    
    // 등록 가능한 Job 클래스 목록 조회
    public List<String> getAvailableJobTypes() {
    	
        // 1. JobClassRegistry에 등록된 Job 클래스 목록을 조회한다.
        return jobClassRegistry.getAvailableJobTypes()
                .stream()
                .sorted()
                .toList();
        
    }
    
    // Job 단건 조회
    public JobInfoDto getScheduledJob(String jobName, String jobGroup) {

        // 1. Job 이름과 그룹으로 등록된 Job을 조회한다.
        JobInfoDto jobInfo = dynamicJobMapper.findJob(jobName, jobGroup);

        // 2. 조회 결과가 없으면 예외를 발생시킨다.
        if (jobInfo == null) {
            throw new IllegalArgumentException(
                    "조회할 Job이 존재하지 않습니다: " + jobName + "/" + jobGroup
            );
        }

        return jobInfo;
    }
    
    // Quartz Scheduler API 기준 Trigger 상태 설정
    private void setSchedulerTriggerState(JobInfoDto jobInfo) throws SchedulerException {
    	
		// 1. Trigger가 없는 Job은 NONE 상태로 처리한다.
		if (jobInfo.getTriggerName() == null || jobInfo.getTriggerGroup() == null) {
			jobInfo.setSchedulerTriggerState(Trigger.TriggerState.NONE.name());
			return;
		}
		
		// 2. TriggerKey로 Quartz Scheduler의 논리적인 Trigger 상태를 조회한다.
		TriggerKey triggerKey = TriggerKey.triggerKey(jobInfo.getTriggerName(), jobInfo.getTriggerGroup());
		
		jobInfo.setSchedulerTriggerState(scheduler.getTriggerState(triggerKey).name());
    }
    
    // Job 중지
    public void pauseJob(String jobName, String jobGroup) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        if (scheduler.checkExists(jobKey)) {
            scheduler.pauseJob(jobKey);
        }else {
        	throw new SchedulerException("Job이 존재하지 않습니다.");
        }
    }
    
    // Job 재시작
    public void resumeJob(String jobName, String jobGroup) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);

        if (scheduler.checkExists(jobKey)) {
            scheduler.resumeJob(jobKey);
        } else {
            throw new SchedulerException("재시작할 Job이 존재하지 않습니다: " + jobName + "/" + jobGroup);
        }
    }
    
    // Job 이력 목록 조회
    public JobHistoryPageDto getJobHistory(JobHistorySearchDto searchDto) {
    	
    	// 1. 검색 조건에 맞는 실행 이력 목록을 조회한다.
        List<JobHistoryDto> content = dynamicJobMapper.findJobHistory(searchDto);

        // 2. 검색 조건에 맞는 전체 실행 이력 건수를 조회한다.
        long totalCount = dynamicJobMapper.countJobHistory(searchDto);

        // 3. 실행 이력 목록과 페이징 정보를 반환한다.
        return new JobHistoryPageDto(
                content,
                totalCount,
                searchDto.getPage(),
                searchDto.getSize()
        );
        
    }
        
}