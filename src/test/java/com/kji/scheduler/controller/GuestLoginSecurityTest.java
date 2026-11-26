package com.kji.scheduler.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.kji.scheduler.config.SecurityConfig;
import com.kji.scheduler.dto.DashboardDto;
import com.kji.scheduler.dto.ExternalApiCallAttemptDetailDto;
import com.kji.scheduler.dto.ExternalApiDto;
import com.kji.scheduler.dto.ExternalApiPagingDto;
import com.kji.scheduler.dto.JobHistoryPageDto;
import com.kji.scheduler.dto.JobInfoDto;
import com.kji.scheduler.dto.RawDataDetailDto;
import com.kji.scheduler.dto.RawDataPageDto;
import com.kji.scheduler.dto.SchedulerInfoDto;
import com.kji.scheduler.security.GuestLoginService;
import com.kji.scheduler.service.AppUserService;
import com.kji.scheduler.service.DashboardService;
import com.kji.scheduler.service.DynamicJobService;
import com.kji.scheduler.service.ExternalApiCallLogService;
import com.kji.scheduler.service.ExternalApiExecutionLogService;
import com.kji.scheduler.service.ExternalApiExecutionService;
import com.kji.scheduler.service.ExternalApiService;
import com.kji.scheduler.service.RawDataService;
import com.kji.scheduler.service.SchedulerInfoService;
import com.kji.scheduler.service.TransformAutoConfigService;

import jakarta.servlet.http.Cookie;

@WebMvcTest(controllers = {AuthController.class, DashboardController.class, DynamicJobController.class,
		RawDataController.class, SchedulerInfoController.class, ExternalApiController.class,
		TransformAutoConfigController.class},
		properties = "app.security.guest-login-enabled=true")
@Import({SecurityConfig.class, GuestLoginService.class})
@MockitoBean(types = {ExternalApiExecutionLogService.class, ExternalApiExecutionService.class})
class GuestLoginSecurityTest {
	@Autowired
	private MockMvc mvc;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@MockitoBean
	private AppUserService appUserService;
	@MockitoBean
	private DynamicJobService dynamicJobService;
	@MockitoBean
	private DashboardService dashboardService;
	@MockitoBean
	private RawDataService rawDataService;
	@MockitoBean
	private SchedulerInfoService schedulerInfoService;
	@MockitoBean
	private ExternalApiService externalApiService;
	@MockitoBean
	private ExternalApiCallLogService externalApiCallLogService;
	@MockitoBean
	private TransformAutoConfigService transformAutoConfigService;
	@MockitoBean
	private UserDetailsService userDetailsService;
	@MockitoSpyBean
	private GuestLoginService guestLoginService;

	@Test
	void guestSessionSupportsApprovedReadsAndBlocksOtherApis() throws Exception {
		JobInfoDto job = new JobInfoDto();
		job.setJobName("existing-job");
		job.setJobData(new byte[] {1, 2, 3});
		when(dynamicJobService.getAllScheduledJobs()).thenReturn(List.of(job));
		when(dashboardService.getDashboard()).thenReturn(new DashboardDto());
		when(dynamicJobService.getJobHistory(any())).thenReturn(new JobHistoryPageDto(List.of(), 0, 1, 10));
		when(rawDataService.getRawDataList(any())).thenReturn(new RawDataPageDto(List.of(), 0, 1, 10));
		RawDataDetailDto rawData = new RawDataDetailDto();
		rawData.setResponseBody("{\"result\":\"ok\"}");
		when(rawDataService.getRawDataDetail(1L)).thenReturn(rawData);
		ExternalApiCallAttemptDetailDto attempt = new ExternalApiCallAttemptDetailDto();
		attempt.setRawDataId(1L);
		when(externalApiCallLogService.getCallAttemptDetail(1L)).thenReturn(attempt);
		when(schedulerInfoService.getSchedulerInfo()).thenReturn(new SchedulerInfoDto());
		ExternalApiDto externalApi = new ExternalApiDto();
		externalApi.setApiName("existing-api");
		when(externalApiService.getExternalApiDetail(1L)).thenReturn(externalApi);
		when(externalApiService.getExternalApiPaging(1L)).thenReturn(new ExternalApiPagingDto());
		when(externalApiService.getExternalApiParamList(1L)).thenReturn(List.of());
		when(transformAutoConfigService.getTransformAutoConfigList(1L)).thenReturn(List.of());

		MockHttpSession anonymousSession = new MockHttpSession();
		String oldSessionId = anonymousSession.getId();
		Cookie token = getCsrfCookie();
		MockHttpSession session = (MockHttpSession) mvc.perform(withCsrf(post("/auth/guest-login"), token)
				.session(anonymousSession).with(request -> {
					request.setRequestedSessionId(oldSessionId);
					request.setRequestedSessionIdValid(true);
					return request;
				}))
				.andExpect(status().isOk()).andReturn().getRequest().getSession(false);
		assertNotNull(session);
		assertNotEquals(oldSessionId, session.getId());
		String guestSessionId = session.getId();

		mvc.perform(get("/auth/me").session(session)).andExpect(status().isOk())
				.andExpect(jsonPath("$.roles.length()").value(1))
				.andExpect(jsonPath("$.roles[0]").value("ROLE_GUEST"));
		mvc.perform(get("/dashboard/info").session(session)).andExpect(status().isOk());
		mvc.perform(get("/jobs/list").session(session)).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].jobName").value("existing-job"))
				.andExpect(jsonPath("$[0].jobData").value("AQID"));
		mvc.perform(get("/jobs/history").session(session)).andExpect(status().isOk());
		mvc.perform(get("/scheduler/info").session(session)).andExpect(status().isOk());
		mvc.perform(get("/raw-data/list").session(session)).andExpect(status().isOk());
		mvc.perform(get("/raw-data/list")).andExpect(status().isUnauthorized());
		mvc.perform(get("/raw-data/detail").param("rawDataId", "1").session(session))
				.andExpect(status().isOk()).andExpect(jsonPath("$.responseBody").value("{\"result\":\"ok\"}"));
		mvc.perform(get("/external-api/detail").param("externalApiId", "1").session(session))
				.andExpect(status().isOk()).andExpect(jsonPath("$.apiName").value("existing-api"));
		for (String path : List.of("/external-api/paging", "/external-api/params", "/transform-auto-config/list")) {
			mvc.perform(get(path).param("externalApiId", "1").session(session)).andExpect(status().isOk());
		}
		mvc.perform(get("/external-api/call-history/attempt-detail").param("apiCallLogId", "1").session(session))
				.andExpect(status().isOk()).andExpect(jsonPath("$.rawDataId").value(1));
		mvc.perform(get("/external-api/call-history/attempt-detail").param("apiCallLogId", "1"))
				.andExpect(status().isUnauthorized());
		mvc.perform(get("/jobs/job-classes").session(session)).andExpect(status().isForbidden());

		Cookie guestToken = getCsrfCookie();
		mvc.perform(withCsrf(post("/jobs/run"), guestToken).session(session)).andExpect(status().isForbidden());
		mvc.perform(withCsrf(post("/external-api/execute"), guestToken).param("externalApiId", "1").session(session))
				.andExpect(status().isForbidden());
		mvc.perform(withCsrf(post("/external-api/restart"), guestToken).param("executionId", "existing-run").session(session))
				.andExpect(status().isForbidden());
		mvc.perform(withCsrf(post("/analysis-load/add"), guestToken).session(session)).andExpect(status().isForbidden());
		mvc.perform(withCsrf(post("/ai-analysis/execute"), guestToken).session(session)).andExpect(status().isForbidden());
		mvc.perform(withCsrf(put("/transform-auto-config/update"), guestToken).session(session)).andExpect(status().isForbidden());
		mvc.perform(withCsrf(post("/auth/guest-login"), guestToken).session(session)).andExpect(status().isOk());
		assertEquals(guestSessionId, session.getId());

		mvc.perform(post("/auth/logout").session(session)).andExpect(status().isOk());
		assertTrue(session.isInvalid());
		mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void guestLoginRequiresCsrfAndEnabledSetting() throws Exception {
		mvc.perform(post("/auth/guest-login")).andExpect(status().isForbidden());
		mvc.perform(get("/auth/guest-login-enabled")).andExpect(jsonPath("$.guestLoginEnabled").value(true));

		doReturn(false).when(guestLoginService).isEnabled();
		mvc.perform(get("/auth/guest-login-enabled")).andExpect(jsonPath("$.guestLoginEnabled").value(false));
		mvc.perform(withCsrf(post("/auth/guest-login"), getCsrfCookie())).andExpect(status().isNotFound());
	}

	@Test
	void existingUserSessionsKeepTheirRolesAndAccess() throws Exception {
		when(dynamicJobService.getAvailableJobTypes()).thenReturn(List.of("ExistingJob"));
		for (String role : List.of("ADMIN", "USER")) {
			when(userDetailsService.loadUserByUsername(role)).thenReturn(User.withUsername(role)
					.password(passwordEncoder.encode("test-password")).roles(role).build());
			MockHttpSession session = (MockHttpSession) mvc.perform(post("/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"username\":\"" + role + "\",\"password\":\"test-password\"}"))
					.andExpect(status().isOk()).andReturn().getRequest().getSession(false);
			assertNotNull(session);
			mvc.perform(withCsrf(post("/auth/guest-login"), getCsrfCookie()).session(session)).andExpect(status().isConflict());
			mvc.perform(get("/auth/me").session(session)).andExpect(jsonPath("$.roles[0]").value("ROLE_" + role));
			mvc.perform(get("/jobs/job-classes").session(session)).andExpect(status().isOk());
		}
	}

	private Cookie getCsrfCookie() throws Exception {
		Cookie token = mvc.perform(get("/auth/guest-login-enabled")).andExpect(status().isOk())
				.andReturn().getResponse().getCookie("XSRF-TOKEN");
		assertNotNull(token);
		return token;
	}

	private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request, Cookie token) {
		return request.cookie(token).header("X-XSRF-TOKEN", token.getValue());
	}
}
