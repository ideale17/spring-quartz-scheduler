package com.kji.scheduler.config;

import java.io.IOException;
import java.net.HttpURLConnection;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
	
	@Bean
	public RestTemplate restTemplate(
			@Value("${external-api.connect-timeout}") int connectTimeout,
			@Value("${external-api.read-timeout}") int readTimeout) {
		
		// 1. External API 호출에 사용할 HTTP RequestFactory를 생성한다.
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory(){
				
			@Override
			protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
				super.prepareConnection(connection, httpMethod);
				connection.setInstanceFollowRedirects(false);
			}
			
		};
		
		// 2. 연결 및 응답 Timeout을 설정한다.
		requestFactory.setConnectTimeout(connectTimeout);
		requestFactory.setReadTimeout(readTimeout);
		
		// 3. Timeout이 적용된 RestTemplate을 생성한다.
		return new RestTemplate(requestFactory);
	}
	
}
