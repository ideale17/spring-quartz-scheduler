package com.kji.scheduler.dto;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.TriggerKey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobLogDto {
	private Long logId;
    private String fireInstanceId;
    private String jobName;
    private String jobGroup;
    private String triggerName;
    private String triggerGroup;
    private LocalDateTime scheduledFireTime;
    private LocalDateTime actualFireTime;
    private LocalDateTime finishedAt;
    private Long runMillis;
    private String status;            // STARTED / SUCCESS / FAILED / VETOED
    private String exceptionMessage;
    private LocalDateTime createdAt;
    
    public static JobLogDto fromContextStart(JobExecutionContext ctx) {
    	
        JobKey jk = ctx.getJobDetail().getKey();
        TriggerKey tk = ctx.getTrigger().getKey();
        
        return JobLogDto.builder()
            .fireInstanceId(ctx.getFireInstanceId())
            .jobName(jk.getName())
            .jobGroup(jk.getGroup())
            .triggerName(tk.getName())
            .triggerGroup(tk.getGroup())
            .scheduledFireTime(toLdt(ctx.getScheduledFireTime()))
            .actualFireTime(toLdt(ctx.getFireTime()))
            .status("STARTED")
            .build();
        
    }

    private static LocalDateTime toLdt(Date d) {
        return (d == null) ? null : LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault());
    }
    
}
