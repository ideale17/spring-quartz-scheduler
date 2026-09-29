package com.kji.scheduler.service;

import java.util.List;

import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kji.scheduler.dto.ExternalApiDto;
import com.kji.scheduler.dto.ExternalApiParamDto;
import com.kji.scheduler.dto.ExternalApiRequestDto;
import com.kji.scheduler.dto.JobInfoDto;
import com.kji.scheduler.mapper.ExternalApiMapper;

@Service
public class ExternalApiService {
	
	private final ExternalApiMapper externalApiMapper;
	private final DynamicJobService dynamicJobService;
	
	public ExternalApiService(ExternalApiMapper externalApiMapper, DynamicJobService dynamicJobService) {
		this.externalApiMapper = externalApiMapper;
		this.dynamicJobService = dynamicJobService;
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
	
	// External API 등록
	public Long createExternalApi(ExternalApiDto externalApi) {
		
		// 1. 필수 입력값을 검증한다.
		if (externalApi == null) {
			throw new IllegalArgumentException("External API 등록 정보가 없습니다.");
		}
		
		if (externalApi.getApiName() == null || externalApi.getApiName().isBlank()) {
			throw new IllegalArgumentException("External API 이름은 필수입니다.");
		}

		if (externalApi.getApiUrl() == null || externalApi.getApiUrl().isBlank()) {
			throw new IllegalArgumentException("External API URL은 필수입니다.");
		}
		
		if (externalApi.getHttpMethod() == null || externalApi.getHttpMethod().isBlank()) {
			throw new IllegalArgumentException("HTTP Method는 필수입니다.");
		}
		
		// 2. 사용 여부가 없으면 기본값 Y를 설정한다.
		if (externalApi.getEnabled() == null || externalApi.getEnabled().isBlank()) {
			externalApi.setEnabled("Y");
		}
		
		// 3. 재시도 설정을 검증하고 기본값을 설정한다.
		validateRetryPolicy(externalApi);
		
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
		
		if (externalApi.getApiUrl() == null || externalApi.getApiUrl().isBlank()) {
			throw new IllegalArgumentException("External API URL은 필수입니다.");
		}
		
		if (externalApi.getHttpMethod() == null || externalApi.getHttpMethod().isBlank()) {
			throw new IllegalArgumentException("HTTP Method는 필수입니다.");
		}
		
		// 3. 사용 여부가 없으면 기본값 Y를 설정한다.
		if (externalApi.getEnabled() == null || externalApi.getEnabled().isBlank()) {
			externalApi.setEnabled("Y");
		}
		
		// 4. 재시도 설정을 검증하고 기본값을 설정한다.
		validateRetryPolicy(externalApi);
		
		// 5. 인증 설정을 검증하고 기본값을 설정한다.
		validateAuth(externalApi);
		
		// 6. External API 기본 정보를 등록한다.
		int insertCount = externalApiMapper.insertExternalApi(externalApi);

		if (insertCount != 1) {
			throw new IllegalStateException("External API 등록에 실패했습니다.");
		}
		
		// 7. 생성된 External API 식별자를 확인한다.
		Long externalApiId = externalApi.getExternalApiId();
		
		if (externalApiId == null) {
			throw new IllegalStateException("External API 식별자 생성에 실패했습니다.");
		}
		
		// 8. External API 파라미터를 등록한다.
		if (request.getParams() != null) {
			for (ExternalApiParamDto param : request.getParams()) {
				
				// 9. 생성된 External API 식별자를 파라미터에 설정한다.
				param.setExternalApiId(externalApiId);
				
				// 10. 파라미터 기본값을 설정한다.
				if (param.getRequiredYn() == null || param.getRequiredYn().isBlank()) {
					param.setRequiredYn("N");
				}
				
				if (param.getSortOrder() == null) {
					param.setSortOrder(0);
				}
				
				// 11. 파라미터를 등록한다.
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
		
		if (externalApi.getApiUrl() == null || externalApi.getApiUrl().isBlank()) {
			throw new IllegalArgumentException("External API URL은 필수입니다.");
		}
		
		if (externalApi.getHttpMethod() == null || externalApi.getHttpMethod().isBlank()) {
			throw new IllegalArgumentException("HTTP Method는 필수입니다.");
		}
		
		// 4. 사용 여부가 없으면 기본값 Y를 설정한다.
		if (externalApi.getEnabled() == null || externalApi.getEnabled().isBlank()) {
			externalApi.setEnabled("Y");
		}
		
		// 5. 재시도 설정을 검증하고 기본값을 설정한다.
		validateRetryPolicy(externalApi);
		
		// 6. 인증 방식이 동일하고 비밀값을 새로 입력하지 않은 경우 기존 값을 유지한다.
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
		
		// 7. 인증 설정을 검증하고 기본값을 설정한다.
		validateAuth(externalApi);
		
		// 8. URL의 식별자를 수정 대상에 설정한다.
		externalApi.setExternalApiId(externalApiId);
		
		// 9. External API 기본 정보를 수정한다.
		int updateCount = externalApiMapper.updateExternalApi(externalApi);
		
		if (updateCount != 1) {
			throw new IllegalStateException("External API 수정에 실패했습니다.");
		}
		
		// 10. 기존 파라미터를 모두 삭제한다.
		externalApiMapper.deleteExternalApiParams(externalApiId);
		
		// 10. 전달받은 파라미터를 다시 등록한다.
		if (request.getParams() != null) {
			for (ExternalApiParamDto param : request.getParams()) {
				
				// 11. 수정 대상 External API 식별자를 설정한다.
				param.setExternalApiId(externalApiId);
				
				// 12. 기본값을 설정한다.
				if (param.getRequiredYn() == null || param.getRequiredYn().isBlank()) {
					param.setRequiredYn("N");
				}
				
				if (param.getSortOrder() == null) {
					param.setSortOrder(0);
				}
				
				// 13. 파라미터를 등록한다.
				int insertCount = externalApiMapper.insertExternalApiParam(param);
				
				if (insertCount != 1) {
					throw new IllegalStateException("External API 파라미터 수정에 실패했습니다. " + "paramName: " + param.getParamName());
				}
				
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
		
		// 5. External API 기본 정보를 삭제한다.
		int deleteCount = externalApiMapper.deleteExternalApi(externalApiId);
		
		if (deleteCount != 1) {
			throw new IllegalStateException("External API 삭제에 실패했습니다.");
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
	
}