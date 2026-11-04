
package com.kji.scheduler.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kji.scheduler.dto.ExternalApiRequestLogDto;
import com.kji.scheduler.dto.ExternalApiRequestParamLogDto;

/**
 * External API 실제 요청 및 파라미터 이력을 저장한다.
 */
@Mapper
public interface ExternalApiRequestLogMapper {
	
	/**
	 * External API 논리적 HTTP 요청 정보를 저장한다.
	 *
	 * @param requestDto 요청 정보
	 * @return 저장된 행 수
	 */
	int insertRequest(ExternalApiRequestLogDto requestDto);
	
	/**
	 * External API 요청 파라미터 정보를 저장한다.
	 *
	 * @param paramDto 요청 파라미터 정보
	 * @return 저장된 행 수
	 */
	int insertRequestParam(ExternalApiRequestParamLogDto paramDto);
	
	/**
	 * 논리적 HTTP 요청 식별자로 실제 요청 파라미터 목록을 조회한다.
	 *
	 * @param requestId 논리적 HTTP 요청 식별자
	 * @return 실제 요청 파라미터 목록
	 */
	List<ExternalApiRequestParamLogDto> findRequestParams(@Param("requestId") Long requestId);
	
}
