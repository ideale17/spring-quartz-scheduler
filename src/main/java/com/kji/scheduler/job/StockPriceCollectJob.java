package com.kji.scheduler.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.kji.scheduler.service.DashboardService;

@DisallowConcurrentExecution
public class StockPriceCollectJob extends QuartzJobBean {
	
	//private final RestTemplate restTemplate = new RestTemplate();
	
	//@Autowired
	private final DashboardService dashboardService;
	
	public StockPriceCollectJob(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}
	
	@Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		
//		try {
//			
//            String url = "http://localhost:8081/hello";
//            String response = restTemplate.getForObject(url, String.class);
//            System.out.println("API 호출 성공: " + response);
//            
//        } catch (Exception e) {
//            System.err.println("API 호출 실패: " + e.getMessage());
//            throw new JobExecutionException(e);
//        }

		try {
			dashboardService.getDashboard();
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
    }
	
}
