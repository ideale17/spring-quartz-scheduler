package com.example.demo.job;

import java.util.HashMap;
import java.util.Map;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.web.client.RestTemplate;

@DisallowConcurrentExecution
public class WeatherCollectJob extends QuartzJobBean {
	
	private final RestTemplate restTemplate = new RestTemplate();
	
	@Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		
		JobDataMap dataMap = context.getJobDetail().getJobDataMap();
		
		 String baseDate = dataMap.getString("baseDate");
		 String baseTime = dataMap.getString("baseTime");
		 String nx = dataMap.getString("nx");
		 String ny = dataMap.getString("ny");
		
//		try {
//			
//            String url = "http://localhost:8081/weathers/fetchWeather";
//            String response = restTemplate.getForObject(url, String.class);
//            System.out.println("API 호출 성공: " + response);
//            
//        } catch (Exception e) {
//            System.err.println("API 호출 실패: " + e.getMessage());
//            throw new JobExecutionException(e);
//        }
        
		 
		 
		 Map<String, Object> body = new HashMap<>();
		 body.put("baseDate", baseDate);
		 body.put("baseTime", baseTime);
		 body.put("nx", nx);
		 body.put("ny", ny);
		 
		 HttpHeaders headers = new HttpHeaders();
		 headers.setContentType(MediaType.APPLICATION_JSON);

		 HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

		 ResponseEntity<String> response = restTemplate.postForEntity(
		     "http://localhost:8081/weathers/fetchWeather",
		     entity,
		     String.class
		 );
		 
    }
	
}
