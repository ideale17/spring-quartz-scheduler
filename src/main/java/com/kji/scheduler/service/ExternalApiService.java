package com.kji.scheduler.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kji.scheduler.dto.ExternalApiDto;
import com.kji.scheduler.dto.ExternalApiParamDto;
import com.kji.scheduler.dto.ExternalApiRequestDto;
import com.kji.scheduler.mapper.ExternalApiMapper;

@Service
public class ExternalApiService {
	
	private final ExternalApiMapper externalApiMapper;
	
	public ExternalApiService(ExternalApiMapper externalApiMapper) {
		this.externalApiMapper = externalApiMapper;
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
		
		// 3. External API 기본 정보를 등록한다.
		int insertCount = externalApiMapper.insertExternalApi(externalApi);

		// 4. 등록 결과를 확인한다.
		if (insertCount != 1) {
			throw new IllegalStateException("External API 등록에 실패했습니다.");
		}
		
		// 5. 생성된 External API 식별자를 반환한다.
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
		
		// 4. External API 기본 정보를 등록한다.
		int insertCount = externalApiMapper.insertExternalApi(externalApi);

		if (insertCount != 1) {
			throw new IllegalStateException("External API 등록에 실패했습니다.");
		}
		
		// 5. 생성된 External API 식별자를 확인한다.
		Long externalApiId = externalApi.getExternalApiId();
		
		if (externalApiId == null) {
			throw new IllegalStateException("External API 식별자 생성에 실패했습니다.");
		}
		
		// 6. External API 파라미터를 등록한다.
		if (request.getParams() != null) {
			for (ExternalApiParamDto param : request.getParams()) {
				
				// 7. 생성된 External API 식별자를 파라미터에 설정한다.
				param.setExternalApiId(externalApiId);
				
				// 8. 파라미터 기본값을 설정한다.
				if (param.getRequiredYn() == null || param.getRequiredYn().isBlank()) {
					param.setRequiredYn("N");
				}
				
				if (param.getSortOrder() == null) {
					param.setSortOrder(0);
				}
				
				// 9. 파라미터를 등록한다.
				int paramInsertCount = externalApiMapper.insertExternalApiParam(param);
				
				if (paramInsertCount != 1) {
					throw new IllegalStateException("External API 파라미터 등록에 실패했습니다. " + "paramName: " + param.getParamName());
				}
			}
		}
		
		// 10. 생성된 External API 식별자를 반환한다.
		return externalApiId;
	}
	
	// External API 및 파라미터 수정
	@Transactional
	public void updateExternalApiWithParams(Long externalApiId, ExternalApiRequestDto request) {
		
		// 1. 수정 대상 External API 존재 여부를 확인한다.
		getExternalApi(externalApiId);
		
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
		
		// 5. URL의 식별자를 수정 대상에 설정한다.
		externalApi.setExternalApiId(externalApiId);
		
		// 6. External API 기본 정보를 수정한다.
		int updateCount = externalApiMapper.updateExternalApi(externalApi);
		
		if (updateCount != 1) {
			throw new IllegalStateException("External API 수정에 실패했습니다.");
		}
		
		// 7. 기존 파라미터를 모두 삭제한다.
		externalApiMapper.deleteExternalApiParams(externalApiId);
		
		// 8. 전달받은 파라미터를 다시 등록한다.
		if (request.getParams() != null) {
			for (ExternalApiParamDto param : request.getParams()) {
				
				// 9. 수정 대상 External API 식별자를 설정한다.
				param.setExternalApiId(externalApiId);
				
				// 10. 기본값을 설정한다.
				if (param.getRequiredYn() == null || param.getRequiredYn().isBlank()) {
					param.setRequiredYn("N");
				}
				
				if (param.getSortOrder() == null) {
					param.setSortOrder(0);
				}
				
				// 11. 파라미터를 등록한다.
				int insertCount = externalApiMapper.insertExternalApiParam(param);
				
				if (insertCount != 1) {
					throw new IllegalStateException("External API 파라미터 수정에 실패했습니다. " + "paramName: " + param.getParamName());
				}
				
			}
		}
		
	}
	
}