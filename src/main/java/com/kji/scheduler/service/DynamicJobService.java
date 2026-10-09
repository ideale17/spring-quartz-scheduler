package com.kji.scheduler.service;

import java.util.ArrayList;
import java.util.HashMap;
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
import com.kji.scheduler.dto.MisfirePolicy;
import com.kji.scheduler.dto.JobBatchResultDto;
import com.kji.scheduler.dto.JobTargetDto;
import com.kji.scheduler.dto.JobBatchRequest;
import com.kji.scheduler.dto.JobBatchResponse;
import com.kji.scheduler.dto.ScheduleType;
import com.kji.scheduler.dto.UpdateJobRequest;
import com.kji.scheduler.job.ExternalApiCallJob;
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
	 * Quartz Job과 트리거를 등록한다.
	 *
	 * @param request Job 등록 요청 정보
	 * @throws SchedulerException Job 등록 중 Quartz 오류가 발생한 경우
	 */
	public void addJob(CreateJobRequest request) throws SchedulerException {
		
		JobDataMap dataMap = new JobDataMap(request.getParams());
		
		String jobClassName = request.getJobClassName();
		String jobName = request.getJobName();
		String jobGroup = request.getJobGroup();
		ScheduleType scheduleType = request.getScheduleType();
		String scheduleExpr = request.getScheduleExpr();
		
		MisfirePolicy misfirePolicy = request.getMisfirePolicy();
		
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
        	
        	// 3-1. Cron 표현식 유효성 검사
            if (!CronExpression.isValidExpression(scheduleExpr)) {
                throw new IllegalArgumentException("유효하지 않은 Cron 표현식입니다: " + scheduleExpr);
            }
            
            // 3-2. Cron 스케줄 기본 설정
            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(scheduleExpr);
            
            // 3-3. Misfire 정책 적용
        	// SMART_POLICY는 Quartz 기본 정책을 사용하므로 별도 설정하지 않는다.
            if (misfirePolicy == MisfirePolicy.DO_NOTHING) {
            	// 놓친 실행은 건너뛰고 다음 정상 실행 시간부터 다시 실행한다.
            	scheduleBuilder = scheduleBuilder.withMisfireHandlingInstructionDoNothing();
            } else if (misfirePolicy == MisfirePolicy.FIRE_AND_PROCEED) {
            	// 놓친 실행이 있으면 즉시 한 번 실행한 후 다음 정상 실행 일정으로 복귀한다.
            	scheduleBuilder = scheduleBuilder.withMisfireHandlingInstructionFireAndProceed();
            } else if (misfirePolicy != MisfirePolicy.SMART_POLICY) {
            	throw new IllegalArgumentException("CRON에서 지원하지 않는 Misfire 정책입니다: " + misfirePolicy);
            }
            
            // 3-4. Cron Trigger 생성
            trigger = TriggerBuilder.newTrigger()
                .withIdentity(jobName + "Trigger", jobGroup)
                .withSchedule(scheduleBuilder)
                .forJob(jobDetail)
                .build();
            
        }else if(scheduleType == ScheduleType.SIMPLE) {
        	
        	// 3-1. SIMPLE 스케줄 값 유효성 검사
        	long intervalSeconds;
        	
        	try {
        		intervalSeconds = Long.parseLong(scheduleExpr);
        	} catch (NumberFormatException e) {
        		throw new IllegalArgumentException("SIMPLE 스케줄 값은 숫자여야 합니다: " + scheduleExpr);
        	}
        	
        	if (intervalSeconds <= 0) {
        		throw new IllegalArgumentException("SIMPLE 스케줄 값은 0보다 커야 합니다.");
        	}
        	
        	// 3-2. Simple 스케줄 기본 설정
        	SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
        			.withIntervalInMilliseconds(intervalSeconds * 1000L)
        			.repeatForever();
        	
        	// 3-3. Misfire 정책 적용
        	// SMART_POLICY는 Quartz 기본 정책을 사용하므로 별도 설정하지 않는다.
        	if (misfirePolicy == MisfirePolicy.FIRE_NOW) {
        		// 놓친 실행이 있으면 가능한 시점에 즉시 한 번 실행한다.
        		scheduleBuilder = scheduleBuilder.withMisfireHandlingInstructionFireNow();
        	} else if (misfirePolicy == MisfirePolicy.NOW_WITH_EXISTING_COUNT) {
        		// 즉시 실행하고 기존 전체 반복 횟수 기준을 유지한다.
        		scheduleBuilder = scheduleBuilder.withMisfireHandlingInstructionNowWithExistingCount();
        	} else if (misfirePolicy == MisfirePolicy.NOW_WITH_REMAINING_COUNT) {
        		// 즉시 실행하고 남아 있는 반복 횟수를 기준으로 이후 실행을 이어간다.
        		scheduleBuilder = scheduleBuilder.withMisfireHandlingInstructionNowWithRemainingCount();
        	} else if (misfirePolicy == MisfirePolicy.NEXT_WITH_EXISTING_COUNT) {
        		// 놓친 실행은 건너뛰고 다음 실행 시점부터 기존 전체 반복 횟수 기준으로 이어간다.
        		scheduleBuilder = scheduleBuilder.withMisfireHandlingInstructionNextWithExistingCount();
        	} else if (misfirePolicy == MisfirePolicy.NEXT_WITH_REMAINING_COUNT) {
        		// 놓친 실행은 건너뛰고 다음 실행 시점부터 남아 있는 반복 횟수 기준으로 이어간다.
        		scheduleBuilder = scheduleBuilder.withMisfireHandlingInstructionNextWithRemainingCount();
        	} else if (misfirePolicy != MisfirePolicy.SMART_POLICY) {
        		throw new IllegalArgumentException("SIMPLE에서 지원하지 않는 Misfire 정책입니다: " + misfirePolicy);
        	}
        	
        	// 3-4. Simple Trigger 생성
        	trigger = TriggerBuilder.newTrigger()
                    .withIdentity(jobName + "Trigger", jobGroup)
                    .withSchedule(scheduleBuilder)
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
	
	/**
	 * Quartz Job을 트리거 없이 등록한다.
	 *
	 * @param jobClassName 등록할 Job 클래스 이름
	 * @param jobName Job 이름
	 * @param jobGroup Job 그룹
	 * @throws SchedulerException Job 등록 중 Quartz 오류가 발생한 경우
	 */
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
	
	/**
	 * 등록된 Quartz Job에 트리거를 추가한다.
	 *
	 * @param jobName Job 이름
	 * @param jobGroup Job 그룹
	 * @param scheduleType 스케줄 유형
	 * @param scheduleExpr 스케줄 표현식
	 * @throws SchedulerException 트리거 등록 중 Quartz 오류가 발생한 경우
	 */
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
	
	/**
	 * Quartz Job의 스케줄과 전달된 파라미터를 수정한다.
	 *
	 * @param request Job 스케줄 수정 요청 정보
	 * @return 스케줄 수정 성공 여부
	 * @throws SchedulerException Job 스케줄 수정 중 Quartz 오류가 발생한 경우
	 */
	public boolean updateSchedule(UpdateJobRequest request) throws SchedulerException {
		
		String jobName = request.getJobName();
	    String jobGroup = request.getJobGroup();
	    ScheduleType scheduleType = request.getScheduleType();
	    String scheduleExpr = request.getScheduleExpr();
	    
	    MisfirePolicy misfirePolicy = request.getMisfirePolicy();
	    
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
	    
	    // 3. 기존 Trigger 상태 저장
	    Trigger.TriggerState oldTriggerState = scheduler.getTriggerState(triggerKey);
	    
	    // 4. 새 Trigger 생성
	    Trigger newTrigger;
	    
	    if (scheduleType == ScheduleType.CRON) {
	    	
	    	// 4-1. Cron 표현식 유효성 검사
	        if (!CronExpression.isValidExpression(scheduleExpr)) {
	            throw new IllegalArgumentException("유효하지 않은 Cron 표현식입니다: " + scheduleExpr);
	        }
	        
	        // 4-2. Cron 스케줄 기본 설정
	    	CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(scheduleExpr);
	    	
	    	// 4-3. Misfire 정책 적용
	    	// SMART_POLICY는 Quartz 기본 정책을 사용하므로 별도 설정하지 않는다.
	    	if (misfirePolicy == MisfirePolicy.DO_NOTHING) {
	    		// 놓친 실행은 건너뛰고 다음 정상 실행 시간부터 다시 실행한다.
	    		scheduleBuilder = scheduleBuilder.withMisfireHandlingInstructionDoNothing();
	    		
	    	} else if (misfirePolicy == MisfirePolicy.FIRE_AND_PROCEED) {
	    		// 놓친 실행이 있으면 즉시 한 번 실행한 후 다음 정상 실행 일정으로 복귀한다.
	    		scheduleBuilder = scheduleBuilder.withMisfireHandlingInstructionFireAndProceed();
	    		
	    	} else if (misfirePolicy != MisfirePolicy.SMART_POLICY) {
	    		throw new IllegalArgumentException("CRON에서 지원하지 않는 Misfire 정책입니다: " + misfirePolicy);
	    	}
	    	
	        newTrigger = TriggerBuilder.newTrigger()
	                .withIdentity(triggerKey)
	                .forJob(jobKey)
	                .withSchedule(scheduleBuilder)
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
	        
	        // 3-2. Simple 스케줄 기본 설정
        	SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
        			.withIntervalInMilliseconds(intervalSeconds * 1000L)
        			.repeatForever();
        	
	        // 3-3. Misfire 정책 적용
        	// SMART_POLICY는 Quartz 기본 정책을 사용하므로 별도 설정하지 않는다.
        	if (misfirePolicy == MisfirePolicy.FIRE_NOW) {
        		// 놓친 실행이 있으면 가능한 시점에 즉시 한 번 실행한다.
        		scheduleBuilder = scheduleBuilder.withMisfireHandlingInstructionFireNow();
        	} else if (misfirePolicy == MisfirePolicy.NOW_WITH_EXISTING_COUNT) {
        		// 즉시 실행하고 기존 전체 반복 횟수 기준을 유지한다.
        		scheduleBuilder = scheduleBuilder.withMisfireHandlingInstructionNowWithExistingCount();
        	} else if (misfirePolicy == MisfirePolicy.NOW_WITH_REMAINING_COUNT) {
        		// 즉시 실행하고 남아 있는 반복 횟수를 기준으로 이후 실행을 이어간다.
        		scheduleBuilder = scheduleBuilder.withMisfireHandlingInstructionNowWithRemainingCount();
        	} else if (misfirePolicy == MisfirePolicy.NEXT_WITH_EXISTING_COUNT) {
        		// 놓친 실행은 건너뛰고 다음 실행 시점부터 기존 전체 반복 횟수 기준으로 이어간다.
        		scheduleBuilder = scheduleBuilder.withMisfireHandlingInstructionNextWithExistingCount();
        	} else if (misfirePolicy == MisfirePolicy.NEXT_WITH_REMAINING_COUNT) {
        		// 놓친 실행은 건너뛰고 다음 실행 시점부터 남아 있는 반복 횟수 기준으로 이어간다.
        		scheduleBuilder = scheduleBuilder.withMisfireHandlingInstructionNextWithRemainingCount();
        	} else if (misfirePolicy != MisfirePolicy.SMART_POLICY) {
        		throw new IllegalArgumentException("SIMPLE에서 지원하지 않는 Misfire 정책입니다: " + misfirePolicy);
        	}
        	
	        newTrigger = TriggerBuilder.newTrigger()
	                .withIdentity(triggerKey)
	                .forJob(jobKey)
	                .withSchedule(scheduleBuilder)
	                .build();
	        
	    } else {
	        throw new IllegalArgumentException("지원하지 않는 스케줄 타입입니다: " + scheduleType);
	    }
	    
	    // 5. Job 파라미터가 전달된 경우 JobDataMap을 수정
	    if (request.getParams() != null) {
	    	
	        JobDetail jobDetail = scheduler.getJobDetail(jobKey);
	        
	        if (jobDetail == null) {
	            throw new SchedulerException("수정할 Job 정보를 조회할 수 없습니다: " + jobName + "/" + jobGroup);
	        }
	        
	        JobDataMap dataMap = new JobDataMap(request.getParams());
	        
	        JobDetail updatedJobDetail = jobDetail.getJobBuilder()
	        		.setJobData(dataMap)
	        		.build();
	        
	        scheduler.addJob(updatedJobDetail, true, true);
	    }
	    
	    // 6. 기존 Trigger를 새 Trigger로 교체한다.
	    boolean updated = scheduler.rescheduleJob(triggerKey, newTrigger) != null;
	    
	    // 7. 수정 전 중지 상태였다면 다시 중지 상태로 복원한다.
	    if (updated && oldTriggerState == Trigger.TriggerState.PAUSED) {
	    	scheduler.pauseJob(jobKey);
	    }
	    
	    return updated;
	}
	
	/**
	 * 등록된 Quartz Job을 삭제한다.
	 *
	 * @param jobName Job 이름
	 * @param jobGroup Job 그룹
	 * @return Job 삭제 성공 여부
	 * @throws SchedulerException Job 삭제 중 Quartz 오류가 발생한 경우
	 */
    public boolean deleteJob(String jobName, String jobGroup) throws SchedulerException {
        JobKey jobKey = new JobKey(jobName, jobGroup);
        return scheduler.deleteJob(jobKey);
    }
        
	/**
	 * DB에서 Job 목록을 조회한 후 Quartz Scheduler API 기준 Trigger 상태를 조회한다.
	 *
	 * @return 트리거 상태를 포함한 Job 목록
	 * @throws SchedulerException Job 목록 조회 중 Quartz 오류가 발생한 경우
	 */
    public List<JobInfoDto> getAllScheduledJobs() throws SchedulerException {
    	
    	// 1. DB에 저장된 Job 및 Trigger 정보를 조회한다.
        List<JobInfoDto> jobList = dynamicJobMapper.findAllJobList();
        
        // 2. Quartz Scheduler API 기준 Trigger 상태를 추가한다.
        for (JobInfoDto jobInfo : jobList) {
            setSchedulerTriggerState(jobInfo);
        }
        
        return jobList;
        
    }
    
	/**
	 * 등록 가능한 Job 클래스 목록을 이름순으로 조회한다.
	 *
	 * @return 등록 가능한 Job 클래스 이름 목록
	 */
    public List<String> getAvailableJobTypes() {
    	
        // 1. JobClassRegistry에 등록된 Job 클래스 목록을 조회한다.
        return jobClassRegistry.getAvailableJobTypes()
                .stream()
                .sorted()
                .toList();
        
    }
    
	/**
	 * Job 상세 정보와 파라미터, Misfire 정책을 조회한다.
	 *
	 * @param jobName Job 이름
	 * @param jobGroup Job 그룹
	 * @return Job 상세 정보
	 * @throws SchedulerException Job 상세 정보 조회 중 Quartz 오류가 발생한 경우
	 */
    public JobInfoDto getScheduledJob(String jobName, String jobGroup) throws SchedulerException {

        // 1. DB에서 Job 기본 정보를 조회한다.
        JobInfoDto jobInfo = dynamicJobMapper.findJob(jobName, jobGroup);

        // 2. 조회 결과가 없으면 예외를 발생시킨다.
        if (jobInfo == null) {
            throw new IllegalArgumentException("조회할 Job이 존재하지 않습니다: " + jobName + "/" + jobGroup);
        }
        
        // 2. Quartz에서 현재 JobDetail을 조회한다.
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        JobDetail jobDetail = scheduler.getJobDetail(jobKey);
        
        // 3. JobDetail이 존재하면 JobDataMap을 화면용 params로 변환한다.
        if (jobDetail != null) {
            jobInfo.setParams(new HashMap<>(jobDetail.getJobDataMap()));
        }
        
        // 4. Quartz 내부 Misfire 값을 화면에서 사용할 정책값으로 변환한다.
        setMisfirePolicy(jobInfo);
        
        return jobInfo;
    }
    
	/**
	 * Job 정보에 Quartz Scheduler의 트리거 상태를 설정한다.
	 *
	 * @param jobInfo 트리거 상태를 설정할 Job 정보
	 * @throws SchedulerException 트리거 상태 조회 중 Quartz 오류가 발생한 경우
	 */
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
    
	/**
	 * Quartz Job의 즉시 실행을 요청한다.
	 *
	 * @param jobName Job 이름
	 * @param jobGroup Job 그룹
	 * @throws SchedulerException Job 즉시 실행 요청 중 Quartz 오류가 발생한 경우
	 */
    public void runJob(String jobName, String jobGroup) throws SchedulerException {
    	
        // 1. Job 이름과 그룹으로 JobKey를 생성한다.
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        
        // 2. 등록된 Job이 존재하는지 확인한다.
        if (!scheduler.checkExists(jobKey)) {
            throw new SchedulerException("즉시 실행할 Job이 존재하지 않습니다: " + jobName + "/" + jobGroup);
        }
        
        // 3. 기존 스케줄과 별개로 Job을 즉시 한 번 실행한다.
        scheduler.triggerJob(jobKey);
    }
    
	/**
	 * 여러 Quartz Job의 즉시 실행을 일괄 요청한다.
	 *
	 * @param request Job 일괄 즉시 실행 요청 정보
	 * @return Job별 실행 요청 결과와 성공·실패 건수
	 */
    public JobBatchResponse runJobs(JobBatchRequest request) {
    	
    	// 1. 실행할 Job 목록이 존재하는지 확인한다.
    	if (request == null || request.getJobs() == null || request.getJobs().isEmpty()) {
    		throw new IllegalArgumentException("즉시 실행할 Job을 하나 이상 선택해야 합니다.");
    	}
    	
    	// 2. 각 Job의 실행 요청 결과를 저장할 목록을 생성한다.
    	List<JobBatchResultDto> results = new ArrayList<>();
    	int successCount = 0;
    	int failCount = 0;
    	
    	// 3. 선택한 Job을 하나씩 즉시 실행한다.
    	for (JobTargetDto target : request.getJobs()) {
    		
    		JobBatchResultDto result = new JobBatchResultDto();
    		
    		if (target == null) {
    			result.setSuccess(false);
    			result.setMessage("Job 정보가 없습니다.");
    			
    			results.add(result);
    			failCount++;
    			continue;
    		}
    		
    		result.setJobName(target.getJobName());
    		result.setJobGroup(target.getJobGroup());
    		
    		try {
    			runJob(target.getJobName(), target.getJobGroup());
    			
    			result.setSuccess(true);
    			result.setMessage("실행 요청 성공");
    			successCount++;
    			
    		} catch (Exception e) {
    			result.setSuccess(false);
    			result.setMessage(e.getMessage());
    			failCount++;
    		}
    		
    		results.add(result);
    	}
    	
    	// 4. 전체 실행 요청 결과를 생성한다.
    	JobBatchResponse response = new JobBatchResponse();
    	response.setTotalCount(request.getJobs().size());
    	response.setSuccessCount(successCount);
    	response.setFailCount(failCount);
    	response.setResults(results);
    	
    	return response;
    }
    
	/**
	 * 중지된 Quartz Job의 스케줄을 재개한다.
	 *
	 * @param jobName Job 이름
	 * @param jobGroup Job 그룹
	 * @throws SchedulerException Job 스케줄 재개 중 Quartz 오류가 발생한 경우
	 */
    public void resumeJob(String jobName, String jobGroup) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);

        if (scheduler.checkExists(jobKey)) {
            scheduler.resumeJob(jobKey);
        } else {
            throw new SchedulerException("재시작할 Job이 존재하지 않습니다: " + jobName + "/" + jobGroup);
        }
    }
    
	/**
	 * 여러 Quartz Job의 스케줄을 일괄 재개한다.
	 *
	 * @param request Job 스케줄 일괄 재개 요청 정보
	 * @return Job별 재개 결과와 성공·실패 건수
	 */
    public JobBatchResponse resumeJobs(JobBatchRequest request) {
    	
    	// 1. 재시작할 Job 목록이 존재하는지 확인한다.
    	if (request == null || request.getJobs() == null || request.getJobs().isEmpty()) {
    		throw new IllegalArgumentException("재시작할 Job을 하나 이상 선택해야 합니다.");
    	}
    	
    	// 2. 각 Job의 재시작 결과를 저장할 목록을 생성한다.
    	List<JobBatchResultDto> results = new ArrayList<>();
    	int successCount = 0;
    	int failCount = 0;
    	
    	// 3. 선택한 Job을 하나씩 재시작한다.
    	for (JobTargetDto target : request.getJobs()) {
    		
    		JobBatchResultDto result = new JobBatchResultDto();
    		
    		if (target == null) {
    			result.setSuccess(false);
    			result.setMessage("Job 정보가 없습니다.");
    			
    			results.add(result);
    			failCount++;
    			continue;
    		}
    		
    		result.setJobName(target.getJobName());
    		result.setJobGroup(target.getJobGroup());
    		
    		try {
    			resumeJob(target.getJobName(), target.getJobGroup());
    			
    			result.setSuccess(true);
    			result.setMessage("재시작 성공");
    			successCount++;
    			
    		} catch (Exception e) {
    			result.setSuccess(false);
    			result.setMessage(e.getMessage());
    			failCount++;
    		}
    		
    		results.add(result);
    	}
    	
    	// 4. 전체 재시작 결과를 생성한다.
    	JobBatchResponse response = new JobBatchResponse();
    	response.setTotalCount(request.getJobs().size());
    	response.setSuccessCount(successCount);
    	response.setFailCount(failCount);
    	response.setResults(results);
    	
    	return response;
    }
    
	/**
	 * Quartz Job의 스케줄을 중지한다.
	 *
	 * @param jobName Job 이름
	 * @param jobGroup Job 그룹
	 * @throws SchedulerException Job 스케줄 중지 중 Quartz 오류가 발생한 경우
	 */
    public void pauseJob(String jobName, String jobGroup) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        if (scheduler.checkExists(jobKey)) {
            scheduler.pauseJob(jobKey);
        }else {
        	throw new SchedulerException("Job이 존재하지 않습니다.");
        }
    }
    
	/**
	 * 여러 Quartz Job의 스케줄을 일괄 중지한다.
	 *
	 * @param request Job 스케줄 일괄 중지 요청 정보
	 * @return Job별 중지 결과와 성공·실패 건수
	 */
    public JobBatchResponse pauseJobs(JobBatchRequest request) {
    	
    	// 1. 중지할 Job 목록이 존재하는지 확인한다.
    	if (request == null || request.getJobs() == null || request.getJobs().isEmpty()) {
    		throw new IllegalArgumentException("중지할 Job을 하나 이상 선택해야 합니다.");
    	}
    	
    	// 2. 각 Job의 중지 결과를 저장할 목록을 생성한다.
    	List<JobBatchResultDto> results = new ArrayList<>();
    	int successCount = 0;
    	int failCount = 0;
    	
    	// 3. 선택한 Job을 하나씩 중지한다.
    	for (JobTargetDto target : request.getJobs()) {
    		
    		JobBatchResultDto result = new JobBatchResultDto();
    		
    		if (target == null) {
    			result.setSuccess(false);
    			result.setMessage("Job 정보가 없습니다.");
    			
    			results.add(result);
    			failCount++;
    			continue;
    		}
    		
    		result.setJobName(target.getJobName());
    		result.setJobGroup(target.getJobGroup());
    		
    		try {
    			pauseJob(target.getJobName(), target.getJobGroup());
    			
    			result.setSuccess(true);
    			result.setMessage("중지 성공");
    			successCount++;
    			
    		} catch (Exception e) {
    			result.setSuccess(false);
    			result.setMessage(e.getMessage());
    			failCount++;
    		}
    		
    		results.add(result);
    	}
    	
    	// 4. 전체 중지 결과를 생성한다.
    	JobBatchResponse response = new JobBatchResponse();
    	response.setTotalCount(request.getJobs().size());
    	response.setSuccessCount(successCount);
    	response.setFailCount(failCount);
    	response.setResults(results);
    	
    	return response;
    }
    
	/**
	 * 검색 조건에 맞는 Quartz Job 실행 이력을 조회한다.
	 *
	 * @param searchDto Job 실행 이력 검색 조건
	 * @return 페이지 정보를 포함한 Job 실행 이력
	 */
    public JobHistoryPageDto getJobHistory(JobHistorySearchDto searchDto) {
    	
    	// 1. 종료일이 있으면 조회 종료 시각을 다음 날로 계산한다.
    	if (searchDto.getEndDate() != null) {
    		searchDto.setEndDateExclusive(searchDto.getEndDate().plusDays(1));
    	}
    	
    	// 2. 검색 조건에 맞는 실행 이력 목록을 조회한다.
        List<JobHistoryDto> content = dynamicJobMapper.findJobHistory(searchDto);

        // 3. 검색 조건에 맞는 전체 실행 이력 건수를 조회한다.
        long totalCount = dynamicJobMapper.countJobHistory(searchDto);

        // 4. 실행 이력 목록과 페이징 정보를 반환한다.
        return new JobHistoryPageDto(
                content,
                totalCount,
                searchDto.getPage(),
                searchDto.getSize()
        );
        
    }
    
	/**
	 * 지정한 External API를 사용하는 Job 목록을 조회한다.
	 *
	 * @param externalApiId External API 식별자
	 * @return External API를 참조하는 Job 목록
	 * @throws SchedulerException External API를 사용하는 Job 목록 조회 중 Quartz 오류가 발생한 경우
	 */
    public List<JobInfoDto> getJobsUsingExternalApi(Long externalApiId) throws SchedulerException {
    	
    	// 1. External API 식별자를 검증한다.
    	if (externalApiId == null) {
    		throw new IllegalArgumentException("External API 식별자는 필수입니다.");
    	}
    	
    	List<JobInfoDto> jobList = new ArrayList<>();
    	
    	// 2. Quartz에 등록된 전체 Job을 조회한다.
    	for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.anyJobGroup())) {
    		
    		JobDetail jobDetail = scheduler.getJobDetail(jobKey);
    		
    		if (jobDetail == null) {
    			continue;
    		}
    		
    		// 3. ExternalApiCallJob이 아닌 Job은 제외한다.
    		if (!ExternalApiCallJob.class.equals(jobDetail.getJobClass())) {
    			continue;
    		}
    		
    		// 4. Job에 등록된 externalApiId를 조회한다.
    		Object jobExternalApiId = jobDetail.getJobDataMap().get("externalApiId");
    		
    		if (jobExternalApiId == null) {
    			continue;
    		}
    		
    		// 5. 삭제 대상 External API를 사용하는 Job인지 확인한다.
    		if (!externalApiId.toString().equals(jobExternalApiId.toString())) {
    			continue;
    		}
    		
    		// 6. 참조 중인 Job 정보를 결과에 추가한다.
    		JobInfoDto jobInfo = new JobInfoDto();
    		jobInfo.setJobName(jobKey.getName());
    		jobInfo.setJobGroup(jobKey.getGroup());
    		jobInfo.setJobClassName(jobDetail.getJobClass().getSimpleName());
    		
    		jobList.add(jobInfo);
    	}
    	
    	return jobList;
    }
    
	/**
	 * Quartz 내부 Misfire 값을 관리 화면용 정책으로 변환하여 설정한다.
	 *
	 * @param jobInfo Misfire 정책을 설정할 Job 정보
	 */
    private void setMisfirePolicy(JobInfoDto jobInfo) {
    	
    	// 1. Trigger가 없는 Job은 Misfire 정책을 설정하지 않는다.
    	if (jobInfo.getTriggerType() == null || jobInfo.getMisfireInstr() == null) {
    		return;
    	}
    	
    	// 2. SMART_POLICY는 Trigger 종류와 관계없이 공통 값이다.
    	if (jobInfo.getMisfireInstr() == 0) {
    		jobInfo.setMisfirePolicy(MisfirePolicy.SMART_POLICY);
    		return;
    	}
    	
    	// 3. Cron Trigger의 Misfire 정책을 변환한다.
    	if ("CRON".equals(jobInfo.getTriggerType())) {
    		
    		if (jobInfo.getMisfireInstr() == 1) {
    			jobInfo.setMisfirePolicy(MisfirePolicy.FIRE_AND_PROCEED);
    		} else if (jobInfo.getMisfireInstr() == 2) {
    			jobInfo.setMisfirePolicy(MisfirePolicy.DO_NOTHING);
    		}
    		
    		return;
    	}
    	
    	// 4. Simple Trigger의 Misfire 정책을 변환한다.
    	if ("SIMPLE".equals(jobInfo.getTriggerType())) {
    		
    		if (jobInfo.getMisfireInstr() == 1) {
    			jobInfo.setMisfirePolicy(MisfirePolicy.FIRE_NOW);
    		} else if (jobInfo.getMisfireInstr() == 2) {
    			jobInfo.setMisfirePolicy(MisfirePolicy.NOW_WITH_EXISTING_COUNT);
    		} else if (jobInfo.getMisfireInstr() == 3) {
    			jobInfo.setMisfirePolicy(MisfirePolicy.NOW_WITH_REMAINING_COUNT);
    		} else if (jobInfo.getMisfireInstr() == 4) {
    			jobInfo.setMisfirePolicy(MisfirePolicy.NEXT_WITH_REMAINING_COUNT);
    		} else if (jobInfo.getMisfireInstr() == 5) {
    			jobInfo.setMisfirePolicy(MisfirePolicy.NEXT_WITH_EXISTING_COUNT);
    		}
    	}
    	
    }
    
}
