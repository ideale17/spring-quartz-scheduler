package com.kji.scheduler;

import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:scheduler-context",
		"spring.quartz.job-store-type=memory"
})
class DemoApplicationTests {

	@DynamicPropertySource
	static void testProperties(DynamicPropertyRegistry registry) {
		registry.add("app.security.encryption-key", () -> Base64.getEncoder().encodeToString(new byte[32]));
	}

	@Test
	void contextLoads() {
	}

}
