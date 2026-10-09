package com.kji.scheduler.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kji.scheduler.dto.ExternalApiAuthDto;
import com.kji.scheduler.dto.ExternalApiBasicDto;
import com.kji.scheduler.dto.ExternalApiDto;
import com.kji.scheduler.dto.ExternalApiPagingDto;
import com.kji.scheduler.dto.ExternalApiParamDto;

@Mapper
public interface ExternalApiMapper {

	/**
	 * External API 목록을 식별자 내림차순으로 조회한다.
	 *
	 * @return External API 목록
	 */
	List<ExternalApiDto> findAllExternalApiList();

	/**
	 * External API 식별자를 조건으로 External API 정보를 조회한다.
	 *
	 * @param externalApiId 조회 대상 External API 식별자
	 * @return External API 정보, 없으면 null
	 */
	ExternalApiDto findExternalApi(@Param("externalApiId") Long externalApiId);
	
	/**
	 * External API 식별자를 조건으로 화면용 상세 정보와 인증 비밀값 설정 여부를 조회한다.
	 *
	 * @param externalApiId 조회 대상 External API 식별자
	 * @return 화면용 External API 상세 정보, 없으면 null
	 */
	ExternalApiDto findExternalApiDetail(@Param("externalApiId") Long externalApiId);
	
	/**
	 * External API 식별자를 조건으로 파라미터 목록을 정렬 순서와 파라미터 식별자순으로 조회한다.
	 *
	 * @param externalApiId 조회 대상 External API 식별자
	 * @return External API 파라미터 목록
	 */
	List<ExternalApiParamDto> findExternalApiParamList(@Param("externalApiId") Long externalApiId);
	
	/**
	 * External API 정보를 저장한다.
	 *
	 * @param externalApi 저장할 External API 정보
	 * @return 저장된 행 수
	 */
	int insertExternalApi(ExternalApiDto externalApi);
	
	/**
	 * External API 파라미터 정보를 저장한다.
	 *
	 * @param param 저장할 External API 파라미터 정보
	 * @return 저장된 행 수
	 */
	int insertExternalApiParam(ExternalApiParamDto param);
	
	/**
	 * External API 식별자를 조건으로 기본 정보와 인증 정보를 수정한다.
	 *
	 * @param externalApi 식별자를 포함한 External API 수정 정보
	 * @return 수정된 행 수
	 */
	int updateExternalApi(ExternalApiDto externalApi);
	
	/**
	 * External API 식별자를 조건으로 기본 정보를 수정한다.
	 *
	 * @param externalApiId 수정 대상 External API 식별자
	 * @param basic 수정할 External API 기본 정보
	 * @return 수정된 행 수
	 */
	int updateExternalApiBasic(@Param("externalApiId") Long externalApiId, @Param("basic") ExternalApiBasicDto basic);
	
	/**
	 * External API 식별자를 조건으로 인증 정보를 수정한다.
	 *
	 * @param externalApiId 수정 대상 External API 식별자
	 * @param auth 수정할 External API 인증 정보
	 * @return 수정된 행 수
	 */
	int updateExternalApiAuth(@Param("externalApiId") Long externalApiId, @Param("auth") ExternalApiAuthDto auth);
	
	/**
	 * External API 식별자를 조건으로 해당 API의 파라미터를 모두 삭제한다.
	 *
	 * @param externalApiId 삭제 대상 External API 식별자
	 * @return 삭제된 행 수
	 */
	int deleteExternalApiParams(@Param("externalApiId") Long externalApiId);
	
	/**
	 * External API 식별자를 조건으로 External API 정보를 삭제한다.
	 *
	 * @param externalApiId 삭제 대상 External API 식별자
	 * @return 삭제된 행 수
	 */
	int deleteExternalApi(@Param("externalApiId") Long externalApiId);
	
	/**
	 * External API 식별자를 조건으로 페이징 설정을 조회한다.
	 *
	 * @param externalApiId 조회 대상 External API 식별자
	 * @return External API 페이징 설정, 없으면 null
	 */
	ExternalApiPagingDto findExternalApiPaging(@Param("externalApiId") Long externalApiId);
	
	/**
	 * External API 페이징 설정을 저장한다.
	 *
	 * @param paging 저장할 External API 페이징 설정
	 * @return 저장된 행 수
	 */
	int insertExternalApiPaging(ExternalApiPagingDto paging);
	
	/**
	 * External API 식별자를 조건으로 페이징 설정을 수정한다.
	 *
	 * @param paging 식별자를 포함한 페이징 수정 정보
	 * @return 수정된 행 수
	 */
	int updateExternalApiPaging(ExternalApiPagingDto paging);
	
	/**
	 * External API 식별자를 조건으로 페이징 설정을 삭제한다.
	 *
	 * @param externalApiId 삭제 대상 External API 식별자
	 * @return 삭제된 행 수
	 */
	int deleteExternalApiPaging(@Param("externalApiId") Long externalApiId);
	
}
