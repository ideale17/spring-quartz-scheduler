
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
	
	/**
	 * 원본 실행의 동일한 파라미터 조합과 페이지 요청을 조회한다.
	 *
	 * @param executionId 원본 실행 ID
	 * @param combinationNo 파라미터 조합 순번
	 * @param pageNo 페이지 번호
	 * @return 원본 요청 ID
	 */
	Long findOriginalRequestId(@Param("executionId") String executionId,
			@Param("combinationNo") int combinationNo,
			@Param("pageNo") Integer pageNo);
	
}
