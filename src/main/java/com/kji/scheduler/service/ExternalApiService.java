package com.kji.scheduler.service;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;

import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kji.scheduler.crypto.AesGcmEncryptionService;
import com.kji.scheduler.dto.ExternalApiAuthDto;
import com.kji.scheduler.dto.ExternalApiBasicDto;
import com.kji.scheduler.dto.ExternalApiDto;
import com.kji.scheduler.dto.ExternalApiPagingDto;
import com.kji.scheduler.dto.ExternalApiParamDto;
import com.kji.scheduler.dto.ExternalApiRequestDto;
import com.kji.scheduler.dto.JobInfoDto;
import com.kji.scheduler.mapper.ExternalApiMapper;

@Service
public class ExternalApiService {
	
	private final ExternalApiMapper externalApiMapper;
	private final DynamicJobService dynamicJobService;
	private final AesGcmEncryptionService encryptionService;
	
	public ExternalApiService(
			ExternalApiMapper externalApiMapper,
			DynamicJobService dynamicJobService,
			AesGcmEncryptionService encryptionService) {
		this.externalApiMapper = externalApiMapper;
		this.dynamicJobService = dynamicJobService;
		this.encryptionService = encryptionService;
	}
	
	// External API 목록 조회
	public List<ExternalApiDto> getExternalApiList() {
		
		// 1. 등록된 External API 목록을 조회한다.
		return externalApiMapper.findAllExternalApiList();
	}
	
	// External API 단건 조회
	public ExternalApiDto getExternalApi(Long externalApiId) {
		
		// 1. External API 기본 정보를 조회한다.
		ExternalApiDto externalApi = externalApiMapper.findExternalApi(externalApiId);
		
		// 2. 조회 결과가 없으면 예외를 발생시킨다.
		if (externalApi == null) {
			throw new IllegalArgumentException("존재하지 않는 External API입니다. externalApiId: " + externalApiId);
		}
		
		// 3. External API 인증 비밀값을 복호화한다.
		switch (externalApi.getAuthType()) {		
			case "API_KEY", "BEARER" ->
				externalApi.setAuthValue(encryptionService.decrypt(externalApi.getAuthValue()));
				
			case "BASIC" ->
				externalApi.setAuthPassword(encryptionService.decrypt(externalApi.getAuthPassword()));
				
		}
		
		return externalApi;
	}
	
	// External API 화면용 단건 조회
	public ExternalApiDto getExternalApiDetail(Long externalApiId) {
		
	    // 1. 화면에 표시할 External API 기본 정보를 조회한다.
	    ExternalApiDto externalApi = externalApiMapper.findExternalApiDetail(externalApiId);
	    
	    // 2. 조회 결과가 없으면 예외를 발생시킨다.
	    if (externalApi == null) {
	        throw new IllegalArgumentException("존재하지 않는 External API입니다. externalApiId: " + externalApiId);
	    }
	    
	    return externalApi;
	}
	
	// External API 파라미터 목록 조회
	public List<ExternalApiParamDto> getExternalApiParamList(Long externalApiId) {
		
		// 1. External API 존재 여부를 확인한다.
		getExternalApi(externalApiId);
		
		// 2. External API에 등록된 파라미터 목록을 조회한다.
		return externalApiMapper.findExternalApiParamList(externalApiId);
	}
	
	// External API 페이징 설정 조회
	public ExternalApiPagingDto getExternalApiPaging(Long externalApiId) {
		
		// 1. External API 존재 여부를 확인한다.
		getExternalApi(externalApiId);
		
		// 2. External API 페이징 설정을 조회한다.
		return externalApiMapper.findExternalApiPaging(externalApiId);
	}
	
	// External API 등록
	public Long createExternalApi(ExternalApiDto externalApi) {
		
		// 1. 필수 입력값을 검증한다.
		if (externalApi == null) {
			throw new IllegalArgumentException("External API 등록 정보가 없습니다.");
		}
		
		if (externalApi.getApiName() == null || externalApi.getApiName().isBlank()) {
			throw new IllegalArgumentException("External API 이름은 필수입니다.");
		}
		
		// 2. External API URL을 검증한다.
		validateExternalApiUrl(externalApi.getApiUrl());
		
		if (externalApi.getHttpMethod() == null || externalApi.getHttpMethod().isBlank()) {
			throw new IllegalArgumentException("HTTP Method는 필수입니다.");
		}
		
		// 3. 사용 여부가 없으면 기본값 Y를 설정한다.
		if (externalApi.getEnabled() == null || externalApi.getEnabled().isBlank()) {
			externalApi.setEnabled("Y");
		}
		
		// 4. 재시도 설정을 검증하고 기본값을 설정한다.
		validateRetryPolicy(externalApi);
		
		// 5. External API 기본 정보를 등록한다.
		int insertCount = externalApiMapper.insertExternalApi(externalApi);

		// 6. 등록 결과를 확인한다.
		if (insertCount != 1) {
			throw new IllegalStateException("External API 등록에 실패했습니다.");
		}
		
		// 7. 생성된 External API 식별자를 반환한다.
		return externalApi.getExternalApiId();
	}
	
	// External API 및 파라미터 등록
	@Transactional
	public Long createExternalApiWithParams(ExternalApiRequestDto request) {
		
		// 1. 등록 요청 정보를 검증한다.
		if (request == null || request.getExternalApi() == null) {
			throw new IllegalArgumentException("External API 등록 정보가 없습니다.");
		}
		
		ExternalApiDto externalApi = request.getExternalApi();
		
		// 2. External API 기본 정보를 검증한다.
		if (externalApi.getApiName() == null || externalApi.getApiName().isBlank()) {
			throw new IllegalArgumentException("External API 이름은 필수입니다.");
		}
		
		// 3. External API URL을 검증한다.
		validateExternalApiUrl(externalApi.getApiUrl());
		
		if (externalApi.getHttpMethod() == null || externalApi.getHttpMethod().isBlank()) {
			throw new IllegalArgumentException("HTTP Method는 필수입니다.");
		}
		
		// 4. 사용 여부가 없으면 기본값 Y를 설정한다.
		if (externalApi.getEnabled() == null || externalApi.getEnabled().isBlank()) {
			externalApi.setEnabled("Y");
		}
		
		// 5. 재시도 설정을 검증하고 기본값을 설정한다.
		validateRetryPolicy(externalApi);
		
		// 6. 인증 설정을 검증하고 기본값을 설정한다.
		validateAuth(externalApi);
		
		// 7. 인증 방식에 따라 인증 정보를 암호화한다.
		switch (externalApi.getAuthType()) {
			case "API_KEY", "BEARER" ->
				externalApi.setAuthValue(encryptionService.encrypt(externalApi.getAuthValue()));
				
			case "BASIC" ->
				externalApi.setAuthPassword(encryptionService.encrypt(externalApi.getAuthPassword()));
		}
		
		// 8. External API 기본 정보를 등록한다.
		int insertCount = externalApiMapper.insertExternalApi(externalApi);

		if (insertCount != 1) {
			throw new IllegalStateException("External API 등록에 실패했습니다.");
		}
		
		// 9. 생성된 External API 식별자를 확인한다.
		Long externalApiId = externalApi.getExternalApiId();
		
		if (externalApiId == null) {
			throw new IllegalStateException("External API 식별자 생성에 실패했습니다.");
		}
		
		// 10. 페이징 설정이 있으면 등록한다.
		ExternalApiPagingDto paging = request.getPaging();
		
		if (paging != null) {
			
			// 10-1. External API 식별자를 설정한다.
			paging.setExternalApiId(externalApiId);
			
			// 10-2. 페이징 설정을 검증하고 기본값을 설정한다.
			validatePaging(paging);
			
			// 10-3. 페이징 설정을 등록한다.
			int pagingInsertCount = externalApiMapper.insertExternalApiPaging(paging);
			
			if (pagingInsertCount != 1) {
				throw new IllegalStateException("External API 페이징 설정 등록에 실패했습니다.");
			}
			
		}
		
		// 11. External API 파라미터를 등록한다.
		if (request.getParams() != null) {
			
			for (ExternalApiParamDto param : request.getParams()) {
				
				// 11-1. 생성된 External API 식별자를 파라미터에 설정한다.
				param.setExternalApiId(externalApiId);
				
				// 11-2. 파라미터 기본값을 설정한다.
				if (param.getRequiredYn() == null || param.getRequiredYn().isBlank()) {
					param.setRequiredYn("N");
				}
				
				if (param.getSortOrder() == null) {
					param.setSortOrder(0);
				}
				
				// 11-3. 파라미터를 등록한다.
				int paramInsertCount = externalApiMapper.insertExternalApiParam(param);
				
				if (paramInsertCount != 1) {
					throw new IllegalStateException("External API 파라미터 등록에 실패했습니다. " + "paramName: " + param.getParamName());
				}
			}
			
		}
		
		// 12. 생성된 External API 식별자를 반환한다.
		return externalApiId;
	}
	
	// External API 및 파라미터 수정
	@Transactional
	public void updateExternalApiWithParams(Long externalApiId, ExternalApiRequestDto request) {
		
		// 1. 수정 대상 External API 정보를 조회한다.
		ExternalApiDto savedExternalApi = getExternalApi(externalApiId);
		
		// 2. 수정 요청 정보를 검증한다.
		if (request == null || request.getExternalApi() == null) {
			throw new IllegalArgumentException("External API 수정 정보가 없습니다.");
		}
		
		ExternalApiDto externalApi = request.getExternalApi();
		
		// 3. 기본 입력값을 검증한다.
		if (externalApi.getApiName() == null || externalApi.getApiName().isBlank()) {
			throw new IllegalArgumentException("External API 이름은 필수입니다.");
		}
		
		// 4. External API URL을 검증한다.
		validateExternalApiUrl(externalApi.getApiUrl());
		
		if (externalApi.getHttpMethod() == null || externalApi.getHttpMethod().isBlank()) {
			throw new IllegalArgumentException("HTTP Method는 필수입니다.");
		}
		
		// 5. 사용 여부가 없으면 기본값 Y를 설정한다.
		if (externalApi.getEnabled() == null || externalApi.getEnabled().isBlank()) {
			externalApi.setEnabled("Y");
		}
		
		// 6. 재시도 설정을 검증하고 기본값을 설정한다.
		validateRetryPolicy(externalApi);
		
		// 7. 인증 방식이 동일하고 비밀값을 새로 입력하지 않은 경우 기존 값을 유지한다.
		if (savedExternalApi.getAuthType() != null && savedExternalApi.getAuthType().equals(externalApi.getAuthType())) {
		    if ("API_KEY".equals(externalApi.getAuthType())
		            && (externalApi.getAuthValue() == null
		            || externalApi.getAuthValue().isBlank())) {
		    	
		        externalApi.setAuthValue(savedExternalApi.getAuthValue());
		    }
		    
		    if ("BEARER".equals(externalApi.getAuthType())
		            && (externalApi.getAuthValue() == null
		            || externalApi.getAuthValue().isBlank())) {
		    	
		        externalApi.setAuthValue(savedExternalApi.getAuthValue());
		    }
		    
		    if ("BASIC".equals(externalApi.getAuthType())
		            && (externalApi.getAuthPassword() == null
		            || externalApi.getAuthPassword().isBlank())) {
		    	
		        externalApi.setAuthPassword(savedExternalApi.getAuthPassword());
		    }
		}
		
		// 8. 인증 설정을 검증하고 기본값을 설정한다.
		validateAuth(externalApi);
		
		// 9. 인증 방식에 따라 인증 정보를 암호화한다.
		switch (externalApi.getAuthType()) {
			case "API_KEY", "BEARER" ->
				externalApi.setAuthValue(encryptionService.encrypt(externalApi.getAuthValue()));
				
			case "BASIC" ->
				externalApi.setAuthPassword(encryptionService.encrypt(externalApi.getAuthPassword()));
		}
		
		// 10. URL의 식별자를 수정 대상에 설정한다.
		externalApi.setExternalApiId(externalApiId);
		
		// 11. External API 기본 정보를 수정한다.
		int updateCount = externalApiMapper.updateExternalApi(externalApi);
		
		if (updateCount != 1) {
			throw new IllegalStateException("External API 수정에 실패했습니다.");
		}
		
		// 12. 기존 파라미터를 모두 삭제한다.
		externalApiMapper.deleteExternalApiParams(externalApiId);
		
		// 13. 전달받은 파라미터를 다시 등록한다.
		if (request.getParams() != null) {
			for (ExternalApiParamDto param : request.getParams()) {
				
				// 13-1. 수정 대상 External API 식별자를 설정한다.
				param.setExternalApiId(externalApiId);
				
				// 13-2. 기본값을 설정한다.
				if (param.getRequiredYn() == null || param.getRequiredYn().isBlank()) {
					param.setRequiredYn("N");
				}
				
				if (param.getSortOrder() == null) {
					param.setSortOrder(0);
				}
				
				// 13-3. 파라미터를 등록한다.
				int insertCount = externalApiMapper.insertExternalApiParam(param);
				
				if (insertCount != 1) {
					throw new IllegalStateException("External API 파라미터 수정에 실패했습니다. " + "paramName: " + param.getParamName());
				}
				
			}
		}
		
	}
	
	// External API 기본 정보 수정
	public void updateExternalApiBasic(Long externalApiId, ExternalApiBasicDto basic) {
		
		// 1. 수정 대상 External API 존재 여부를 확인한다.
		getExternalApi(externalApiId);
		
		// 2. 수정 요청 정보를 검증한다.
		if (basic == null) {
			throw new IllegalArgumentException("External API 기본 수정 정보가 없습니다.");
		}
		
		if (basic.getApiName() == null || basic.getApiName().isBlank()) {
			throw new IllegalArgumentException("External API 이름은 필수입니다.");
		}
		
		if (basic.getApiUrl() == null || basic.getApiUrl().isBlank()) {
			throw new IllegalArgumentException("External API URL은 필수입니다.");
		}
		
		if (basic.getHttpMethod() == null || basic.getHttpMethod().isBlank()) {
			throw new IllegalArgumentException("HTTP Method는 필수입니다.");
		}
		
		// 3. 사용 여부가 없으면 기본값 Y를 설정한다.
		if (basic.getEnabled() == null || basic.getEnabled().isBlank()) {
			basic.setEnabled("Y");
		}
		
		// 4. 재시도 설정을 검증하고 기본값을 설정한다.
		validateRetryPolicy(basic);
		
		// 5. External API 기본 정보를 수정한다.
		int updateCount = externalApiMapper.updateExternalApiBasic(externalApiId, basic);
		
		// 6. 수정 결과를 확인한다.
		if (updateCount != 1) {
			throw new IllegalStateException("External API 기본 정보 수정에 실패했습니다.");
		}
		
	}
	
	// External API 인증 정보 수정
	public void updateExternalApiAuth(Long externalApiId, ExternalApiAuthDto auth) {
		
		// 1. 수정 대상 External API 정보를 조회한다.
		ExternalApiDto savedExternalApi = getExternalApi(externalApiId);
		
		// 2. 수정 요청 정보를 검증한다.
		if (auth == null) {
			throw new IllegalArgumentException("External API 인증 수정 정보가 없습니다.");
		}
		
		// 3. 인증 방식이 동일하고 인증 정보를 새로 입력하지 않은 경우 기존 값을 유지한다.
		if (savedExternalApi.getAuthType() != null
				&& savedExternalApi.getAuthType().equals(auth.getAuthType())) {
			
			if ("API_KEY".equals(auth.getAuthType()) && (auth.getAuthValue() == null || auth.getAuthValue().isBlank())) {
				auth.setAuthValue(savedExternalApi.getAuthValue());
			}
			
			if ("BEARER".equals(auth.getAuthType()) && (auth.getAuthValue() == null || auth.getAuthValue().isBlank())) {
				auth.setAuthValue(savedExternalApi.getAuthValue());
			}
			
			if ("BASIC".equals(auth.getAuthType()) && (auth.getAuthPassword() == null || auth.getAuthPassword().isBlank())) {
				auth.setAuthPassword(savedExternalApi.getAuthPassword());
			}
		}
		
		// 4. 인증 설정을 검증하고 사용하지 않는 값을 초기화한다.
		validateAuth(auth);
		
		// 5. 인증 방식에 따라 인증 정보를 암호화한다.
		switch (auth.getAuthType()) {
			case "API_KEY", "BEARER" ->
				auth.setAuthValue(encryptionService.encrypt(auth.getAuthValue()));
				
			case "BASIC" ->
				auth.setAuthPassword(encryptionService.encrypt(auth.getAuthPassword()));
		}
		
		// 6. External API 인증 정보를 수정한다.
		int updateCount = externalApiMapper.updateExternalApiAuth(externalApiId, auth);
		
		// 7. 수정 결과를 확인한다.
		if (updateCount != 1) {
			throw new IllegalStateException("External API 인증 정보 수정에 실패했습니다.");
		}
		
	}
	
	// External API 페이징 설정 저장
	@Transactional
	public void saveExternalApiPaging(Long externalApiId, ExternalApiPagingDto paging) {
		
		// 1. External API 존재 여부를 확인한다.
		getExternalApi(externalApiId);
		
		// 2. 페이징 설정 정보를 검증한다.
		if (paging == null) {
			throw new IllegalArgumentException("External API 페이징 설정 정보가 없습니다.");
		}
		
		// 3. External API 식별자를 설정한다.
		paging.setExternalApiId(externalApiId);
		
		// 4. 페이징 설정을 검증하고 기본값을 설정한다.
		validatePaging(paging);
		
		// 5. 기존 페이징 설정을 조회한다.
		ExternalApiPagingDto savedPaging = externalApiMapper.findExternalApiPaging(externalApiId);
		
		int saveCount;
		
		// 6. 기존 설정이 없으면 신규 등록한다.
		if (savedPaging == null) {
			saveCount = externalApiMapper.insertExternalApiPaging(paging);
			
		// 7. 기존 설정이 있으면 수정한다.
		} else {
			saveCount = externalApiMapper.updateExternalApiPaging(paging);
		}
		
		// 8. 저장 결과를 확인한다.
		if (saveCount != 1) {
			throw new IllegalStateException("External API 페이징 설정 저장에 실패했습니다.");
		}
	}
	
	// External API 파라미터 수정
	@Transactional
	public void updateExternalApiParams(Long externalApiId, List<ExternalApiParamDto> params) {
		
		// 1. 수정 대상 External API 존재 여부를 확인한다.
		getExternalApi(externalApiId);
		
		// 2. 기존 External API 파라미터를 모두 삭제한다.
		externalApiMapper.deleteExternalApiParams(externalApiId);
		
		// 3. 수정할 파라미터가 없으면 삭제 상태로 종료한다.
		if (params == null || params.isEmpty()) {
			return;
		}
		
		// 4. 전달받은 파라미터를 다시 등록한다.
		for (ExternalApiParamDto param : params) {
			
			// 4-1. External API 식별자를 설정한다.
			param.setExternalApiId(externalApiId);
			
			// 4-2. 필수 여부가 없으면 기본값 N을 설정한다.
			if (param.getRequiredYn() == null || param.getRequiredYn().isBlank()) {
				param.setRequiredYn("N");
			}
			
			// 4-3. 정렬 순서가 없으면 기본값 0을 설정한다.
			if (param.getSortOrder() == null) {
				param.setSortOrder(0);
			}
			
			// 4-4. External API 파라미터를 등록한다.
			int insertCount = externalApiMapper.insertExternalApiParam(param);
			
			// 4-5. 등록 결과를 확인한다.
			if (insertCount != 1) {
				throw new IllegalStateException("External API 파라미터 수정에 실패했습니다. paramName: " + param.getParamName());
			}
		}
		
	}
	
	// External API 삭제
	@Transactional
	public void deleteExternalApi(Long externalApiId) throws SchedulerException {
		
		// 1. 삭제 대상 External API 존재 여부를 확인한다.
		getExternalApi(externalApiId);
		
		// 2. 해당 External API를 사용하는 Quartz Job을 조회한다.
		List<JobInfoDto> usingJobs = dynamicJobService.getJobsUsingExternalApi(externalApiId);
		
		// 3. 사용 중인 Job이 존재하면 삭제를 차단한다.
		if (!usingJobs.isEmpty()) {
			JobInfoDto usingJob = usingJobs.get(0);
			throw new IllegalStateException("External API를 사용하는 Job이 존재하여 삭제할 수 없습니다. jobName: " + usingJob.getJobName() + ", jobGroup: " + usingJob.getJobGroup());
		}
		
		// 4. External API 파라미터를 먼저 삭제한다.
		externalApiMapper.deleteExternalApiParams(externalApiId);
		
		// 5. External API 페이징 설정을 삭제한다.
		externalApiMapper.deleteExternalApiPaging(externalApiId);
		
		// 6. External API 기본 정보를 삭제한다.
		int deleteCount = externalApiMapper.deleteExternalApi(externalApiId);
		
		if (deleteCount != 1) {
			throw new IllegalStateException("External API 삭제에 실패했습니다.");
		}
		
	}
	
	// External API 페이징 설정 삭제
	@Transactional
	public void deleteExternalApiPaging(Long externalApiId) {
		
		// 1. External API 존재 여부를 확인한다.
		getExternalApi(externalApiId);
		
		// 2. 기존 페이징 설정을 조회한다.
		ExternalApiPagingDto savedPaging = externalApiMapper.findExternalApiPaging(externalApiId);
		
		// 3. 페이징 설정이 없으면 정상 종료한다.
		if (savedPaging == null) {
			return;
		}
		
		// 4. 페이징 설정을 삭제한다.
		int deleteCount = externalApiMapper.deleteExternalApiPaging(externalApiId);
		
		// 5. 삭제 결과를 확인한다.
		if (deleteCount != 1) {
			throw new IllegalStateException("External API 페이징 설정 삭제에 실패했습니다.");
		}
		
	}
	
	// External API 재시도 설정을 검증하고 기본값을 설정한다.
	private void validateRetryPolicy(ExternalApiDto externalApi) {
		
		// 1. 재시도 사용 여부가 없으면 기본값 N을 설정한다.
		if (externalApi.getRetryEnabled() == null || externalApi.getRetryEnabled().isBlank()) {
			externalApi.setRetryEnabled("N");
		}
		
		// 2. 재시도 사용 여부 값을 검증한다.
		if (!"Y".equals(externalApi.getRetryEnabled()) && !"N".equals(externalApi.getRetryEnabled())) {
			throw new IllegalArgumentException("재시도 사용 여부는 Y 또는 N이어야 합니다. retryEnabled: " + externalApi.getRetryEnabled());
		}
		
		// 3. 재시도를 사용하지 않으면 재시도 관련 값을 0으로 초기화한다.
		if ("N".equals(externalApi.getRetryEnabled())) {
			externalApi.setMaxRetryCount(0);
			externalApi.setRetryIntervalSec(0);
			return;
		}
		
		// 4. 최대 재시도 횟수를 검증한다.
		if (externalApi.getMaxRetryCount() == null || externalApi.getMaxRetryCount() <= 0) {
			throw new IllegalArgumentException("재시도를 사용하는 경우 최대 재시도 횟수는 1 이상이어야 합니다.");
		}
		
		// 5. 재시도 간격을 검증한다.
		if (externalApi.getRetryIntervalSec() == null || externalApi.getRetryIntervalSec() <= 0) {
			throw new IllegalArgumentException("재시도를 사용하는 경우 재시도 간격은 1초 이상이어야 합니다.");
		}
		
	}
	
	// External API 인증 설정을 검증하고 기본값을 설정한다.
	private void validateAuth(ExternalApiDto externalApi) {
	    
	    // 1. 인증 방식이 없으면 인증 없음으로 설정한다.
	    if (externalApi.getAuthType() == null || externalApi.getAuthType().isBlank()) {
	        externalApi.setAuthType("NONE");
	    }
	    
	    // 2. 인증 방식에 따라 필요한 값을 검증한다.
	    switch (externalApi.getAuthType()) {
	    
		    case "NONE":
		        externalApi.setAuthLocation(null);
		        externalApi.setAuthKey(null);
		        externalApi.setAuthValue(null);
		        externalApi.setAuthUsername(null);
		        externalApi.setAuthPassword(null);
		        break;
		        
		    case "API_KEY":
		        if (externalApi.getAuthLocation() == null || externalApi.getAuthLocation().isBlank()) {
		            throw new IllegalArgumentException("API Key 인증은 전달 위치가 필수입니다.");
		        }
		        
		        if (!"HEADER".equals(externalApi.getAuthLocation()) && !"QUERY".equals(externalApi.getAuthLocation())) {
		            throw new IllegalArgumentException("API Key 전달 위치는 HEADER 또는 QUERY여야 합니다. authLocation: " + externalApi.getAuthLocation());
		        }
		        
		        if (externalApi.getAuthKey() == null || externalApi.getAuthKey().isBlank()) {
		            throw new IllegalArgumentException("API Key 인증은 Key 이름이 필수입니다.");
		        }
		        
		        if (externalApi.getAuthValue() == null || externalApi.getAuthValue().isBlank()) {
		            throw new IllegalArgumentException("API Key 인증은 Key 값이 필수입니다.");
		        }
		        
		        externalApi.setAuthUsername(null);
		        externalApi.setAuthPassword(null);
		        break;
		        
		    case "BEARER":
		        if (externalApi.getAuthValue() == null || externalApi.getAuthValue().isBlank()) {
		            throw new IllegalArgumentException("Bearer Token 인증은 Token 값이 필수입니다.");
		        }
		        
		        externalApi.setAuthLocation(null);
		        externalApi.setAuthKey(null);
		        externalApi.setAuthUsername(null);
		        externalApi.setAuthPassword(null);
		        break;
		        
		    case "BASIC":
		        if (externalApi.getAuthUsername() == null || externalApi.getAuthUsername().isBlank()) {
		            throw new IllegalArgumentException("Basic Auth는 사용자명이 필수입니다.");
		        }
		        
		        if (externalApi.getAuthPassword() == null || externalApi.getAuthPassword().isBlank()) {
		            throw new IllegalArgumentException("Basic Auth는 비밀번호가 필수입니다.");
		        }
		        
		        externalApi.setAuthLocation(null);
		        externalApi.setAuthKey(null);
		        externalApi.setAuthValue(null);
		        break;
		        
		    default:
		        throw new IllegalArgumentException(
		                "지원하지 않는 인증 방식입니다. authType: " + externalApi.getAuthType());
	    }
	}
	
	// External API 기본 정보의 재시도 설정을 검증하고 기본값을 설정한다.
	private void validateRetryPolicy(ExternalApiBasicDto basic) {
		
		// 1. 재시도 사용 여부가 없으면 기본값 N을 설정한다.
		if (basic.getRetryEnabled() == null || basic.getRetryEnabled().isBlank()) {
			basic.setRetryEnabled("N");
		}
		
		// 2. 재시도 사용 여부 값을 검증한다.
		if (!"Y".equals(basic.getRetryEnabled()) && !"N".equals(basic.getRetryEnabled())) {
			throw new IllegalArgumentException("재시도 사용 여부는 Y 또는 N이어야 합니다. retryEnabled: " + basic.getRetryEnabled());
		}
		
		// 3. 재시도를 사용하지 않으면 재시도 관련 값을 0으로 초기화한다.
		if ("N".equals(basic.getRetryEnabled())) {
			basic.setMaxRetryCount(0);
			basic.setRetryIntervalSec(0);
			return;
		}
		
		// 4. 최대 재시도 횟수를 검증한다.
		if (basic.getMaxRetryCount() == null || basic.getMaxRetryCount() <= 0) {
			throw new IllegalArgumentException("재시도를 사용하는 경우 최대 재시도 횟수는 1 이상이어야 합니다.");
		}
		
		// 5. 재시도 간격을 검증한다.
		if (basic.getRetryIntervalSec() == null || basic.getRetryIntervalSec() <= 0) {
			throw new IllegalArgumentException("재시도를 사용하는 경우 재시도 간격은 1초 이상이어야 합니다.");
		}
		
	}
	
	// External API 인증 설정을 검증하고 기본값을 설정한다.
	private void validateAuth(ExternalApiAuthDto auth) {
		
		// 1. 인증 방식이 없으면 인증 없음으로 설정한다.
		if (auth.getAuthType() == null || auth.getAuthType().isBlank()) {
			auth.setAuthType("NONE");
		}
		
		// 2. 인증 방식에 따라 필요한 값을 검증한다.
		switch (auth.getAuthType()) {
		
			case "NONE":
				auth.setAuthLocation(null);
				auth.setAuthKey(null);
				auth.setAuthValue(null);
				auth.setAuthUsername(null);
				auth.setAuthPassword(null);
				break;
				
			case "API_KEY":
				if (auth.getAuthLocation() == null || auth.getAuthLocation().isBlank()) {
					throw new IllegalArgumentException("API Key 인증은 전달 위치가 필수입니다.");
				}
				
				if (!"HEADER".equals(auth.getAuthLocation()) && !"QUERY".equals(auth.getAuthLocation())) {
					throw new IllegalArgumentException("API Key 전달 위치는 HEADER 또는 QUERY여야 합니다. authLocation: " + auth.getAuthLocation());
				}
				
				if (auth.getAuthKey() == null || auth.getAuthKey().isBlank()) {
					throw new IllegalArgumentException("API Key 인증은 Key 이름이 필수입니다.");
				}
				
				if (auth.getAuthValue() == null || auth.getAuthValue().isBlank()) {
					throw new IllegalArgumentException("API Key 인증은 Key 값이 필수입니다.");
				}
				
				auth.setAuthUsername(null);
				auth.setAuthPassword(null);
				break;
				
			case "BEARER":
				if (auth.getAuthValue() == null || auth.getAuthValue().isBlank()) {
					throw new IllegalArgumentException("Bearer Token 인증은 Token 값이 필수입니다.");
				}
				
				auth.setAuthLocation(null);
				auth.setAuthKey(null);
				auth.setAuthUsername(null);
				auth.setAuthPassword(null);
				break;
				
			case "BASIC":
				if (auth.getAuthUsername() == null || auth.getAuthUsername().isBlank()) {
					
					throw new IllegalArgumentException("Basic Auth는 사용자명이 필수입니다.");
				}
				
				if (auth.getAuthPassword() == null || auth.getAuthPassword().isBlank()) {
					
					throw new IllegalArgumentException("Basic Auth는 비밀번호가 필수입니다.");
				}
				
				auth.setAuthLocation(null);
				auth.setAuthKey(null);
				auth.setAuthValue(null);
				break;
				
			default:
				throw new IllegalArgumentException("지원하지 않는 인증 방식입니다. authType: " + auth.getAuthType());
				
		}
		
	}
	
	// External API 페이징 설정을 검증하고 기본값을 설정한다.
	private void validatePaging(ExternalApiPagingDto paging) {
		
		// 1. 사용 여부가 없으면 기본값 Y를 설정한다.
		if (paging.getEnabled() == null || paging.getEnabled().isBlank()) {
			paging.setEnabled("Y");
		}
		
		// 2. 사용 여부를 검증한다.
		if (!"Y".equals(paging.getEnabled()) && !"N".equals(paging.getEnabled())) {
			throw new IllegalArgumentException("페이징 사용 여부는 Y 또는 N이어야 합니다. enabled: " + paging.getEnabled());
		}
		
		// 3. 페이징 방식을 검증한다.
		if (!"PAGE".equals(paging.getPaginationType())) {
			throw new IllegalArgumentException("현재 지원하는 페이징 방식은 PAGE입니다. paginationType: " + paging.getPaginationType());
		}
		
		// 4. 페이징 종료 조건을 검증한다.
		if (!"TOTAL_COUNT".equals(paging.getTerminationType())) {
			throw new IllegalArgumentException("현재 지원하는 페이징 종료 조건은 TOTAL_COUNT입니다. terminationType: " + paging.getTerminationType());
		}
		
		// 5. 페이지 번호 파라미터 위치를 검증한다.
		if (!"QUERY".equals(paging.getPageParamLocation()) && !"BODY".equals(paging.getPageParamLocation())) {
			throw new IllegalArgumentException("페이지 번호 파라미터 위치는 QUERY 또는 BODY여야 합니다.");
		}
		
		// 6. 페이지 번호 파라미터 이름을 검증한다.
		if (paging.getPageParamName() == null || paging.getPageParamName().isBlank()) {
			throw new IllegalArgumentException("페이지 번호 파라미터 이름은 필수입니다.");
		}
		
		// 7. 시작 페이지 번호를 검증한다.
		if (paging.getPageStart() == null) {
			paging.setPageStart(1);
		}
		
		if (paging.getPageStart() < 0) {
			throw new IllegalArgumentException("시작 페이지 번호는 0 이상이어야 합니다.");
		}
		
		// 8. 페이지 크기 파라미터 위치를 검증한다.
		if (!"QUERY".equals(paging.getSizeParamLocation()) && !"BODY".equals(paging.getSizeParamLocation())) {
			throw new IllegalArgumentException("페이지 크기 파라미터 위치는 QUERY 또는 BODY여야 합니다.");
		}
		
		// 9. 페이지 크기 파라미터 이름을 검증한다.
		if (paging.getSizeParamName() == null || paging.getSizeParamName().isBlank()) {
			throw new IllegalArgumentException("페이지 크기 파라미터 이름은 필수입니다.");
		}
		
		// 10. 페이지 크기를 검증한다.
		if (paging.getPageSize() == null || paging.getPageSize() <= 0) {
			throw new IllegalArgumentException("페이지 크기는 1 이상이어야 합니다.");
		}
		
		// 11. 전체 건수 경로를 검증한다.
		if (paging.getTotalCountPath() == null || paging.getTotalCountPath().isBlank()) {
			throw new IllegalArgumentException("전체 건수 경로는 필수입니다.");
		}
		
		// 12. 최대 요청 횟수 기본값을 설정한다.
		if (paging.getMaxRequestCount() == null) {
			paging.setMaxRequestCount(100);
		}
		
		// 13. 최대 요청 횟수를 검증한다.
		if (paging.getMaxRequestCount() <= 0) {
			throw new IllegalArgumentException("최대 요청 횟수는 1 이상이어야 합니다.");
		}
		
	}
	
	/**
	 * 외부 API URL을 검증한다.
	 *
	 * @param apiUrl
	 */
	public void validateExternalApiUrl(String apiUrl) {
		
		// 1. URL 필수값을 확인한다.
		if (apiUrl == null || apiUrl.isBlank()) {
			throw new IllegalArgumentException("외부 API URL은 필수입니다.");
		}
		
		URI uri;
		
		// 2. URL 형식을 확인한다.
		try {
			uri = new URI(apiUrl);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("외부 API URL 형식이 올바르지 않습니다.", e);
		}
		
		// 3. HTTP 또는 HTTPS 프로토콜만 허용한다.
		String scheme = uri.getScheme();
		
		if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
			throw new IllegalArgumentException("외부 API URL은 HTTP 또는 HTTPS만 사용할 수 있습니다.");
		}
		
		// 4. Host가 존재하는지 확인한다.
		String host = uri.getHost();
		
		if (host == null || host.isBlank()) {
			throw new IllegalArgumentException("외부 API URL의 Host가 올바르지 않습니다.");
		}
		
		// 5. Host가 내부 또는 로컬 주소를 가리키는지 확인한다.
		try {
			InetAddress[] addresses = InetAddress.getAllByName(host);
			
			for (InetAddress address : addresses) {
				if (address.isAnyLocalAddress()
						|| address.isLoopbackAddress()
						|| address.isLinkLocalAddress()
						|| address.isSiteLocalAddress()
						|| address.isMulticastAddress()) {
					
					throw new IllegalArgumentException(
							"내부 또는 로컬 네트워크 주소는 외부 API URL로 사용할 수 없습니다."
					);
				}
			}
			
		} catch (UnknownHostException e) {
			throw new IllegalArgumentException("외부 API URL의 Host를 확인할 수 없습니다.", e);
		}
	}
	
}