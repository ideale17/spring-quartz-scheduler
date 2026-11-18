
package com.kji.scheduler.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kji.scheduler.dto.AnalysisModelDto;
import com.kji.scheduler.dto.AnalysisModelFieldDto;
import com.kji.scheduler.dto.AnalysisModelFieldRowDto;
import com.kji.scheduler.dto.AnalysisModelSearchDto;
import com.kji.scheduler.dto.AnalysisModelVersionDto;
import com.kji.scheduler.dto.AnalysisTargetColumnDto;

@Mapper
public interface AnalysisModelMapper {
	
	/**
	 * 검색조건에 해당하는 데이터 적재 모델 목록을 조회한다.
	 */
	List<AnalysisModelDto> findAnalysisModelList(@Param("searchDto") AnalysisModelSearchDto searchDto);
	
	/**
	 * 대상 업무 테이블에 존재하는 컬럼명을 조회한다.
	 */
	List<String> findTargetColumnNames(
			@Param("targetSchema") String targetSchema,
			@Param("targetTable") String targetTable);
	
	/**
	 * 업무 테이블에서 반드시 값을 제공해야 하는 컬럼명을 조회한다.
	 */
	List<String> findRequiredTargetColumnNames(
			@Param("targetSchema") String targetSchema,
			@Param("targetTable") String targetTable);
	
	/**
	 * 대상 업무 테이블의 컬럼 메타데이터를 조회한다.
	 *
	 * @param targetSchema 대상 스키마
	 * @param targetTable 대상 테이블
	 * @return 컬럼 정보 목록
	 */
	List<AnalysisTargetColumnDto> findTargetColumnList(
			@Param("targetSchema") String targetSchema,
			@Param("targetTable") String targetTable);

	/**
	 * 분석 모델 기본 정보를 식별자로 조회한다.
	 *
	 * @param analysisModelId 분석 모델 식별자
	 * @return 분석 모델 정보, 없으면 null
	 */
	AnalysisModelDto findAnalysisModel(@Param("analysisModelId") Long analysisModelId);
	
	/**
	 * 분석 모델의 현재 버전 정보를 조회한다.
	 *
	 * @param analysisModelId 분석 모델 식별자
	 * @return 현재 버전 정보, 없으면 null
	 */
	AnalysisModelVersionDto findCurrentAnalysisModelVersion(@Param("analysisModelId") Long analysisModelId);
	
	/**
	 * 분석 모델의 전체 버전 이력을 조회한다.
	 *
	 * @param analysisModelId 분석 모델 식별자
	 * @return 버전 목록
	 */
	List<AnalysisModelVersionDto> findAnalysisModelVersionList(@Param("analysisModelId") Long analysisModelId);
	
	/**
	 * 분석 모델 버전을 식별자로 조회한다.
	 *
	 * @param analysisModelVersionId 분석 모델 버전 식별자
	 * @return 모델 버전 정보, 없으면 null
	 */
	AnalysisModelVersionDto findAnalysisModelVersion(@Param("analysisModelVersionId") Long analysisModelVersionId);
	
	/**
	 * 특정 모델 버전의 필드 매핑을 조회한다.
	 *
	 * @param analysisModelVersionId 모델 버전 식별자
	 * @return 필드 매핑 목록
	 */
	List<AnalysisModelFieldRowDto> findAnalysisModelFieldList(@Param("analysisModelVersionId") Long analysisModelVersionId);
	
	/**
	 * 데이터 적재 모델 기본 정보를 저장한다.
	 */
	int insertAnalysisModel(AnalysisModelDto model);
	
	/**
	 * 데이터 적재 모델 버전 정보를 저장한다.
	 */
	int insertAnalysisModelVersion(AnalysisModelVersionDto version);
	
	/**
	 * 데이터 적재 모델의 필드 매핑 정보를 저장한다.
	 *
	 * @param field 필드 매핑 설정
	 * @param sourceFieldsJson JSON 문자열로 변환한 원본 필드 목록
	 */
	int insertAnalysisModelField(
			@Param("field") AnalysisModelFieldDto field,
			@Param("sourceFieldsJson") String sourceFieldsJson);
	
	/**
	 * 분석 모델의 기본 정보를 수정한다.
	 *
	 * @param model 수정할 분석 모델 정보
	 * @return 수정된 행 수
	 */
	int updateAnalysisModel(AnalysisModelDto model);
	
	/**
	 * 분석 모델의 현재 버전을 변경한다.
	 *
	 * @param analysisModelId 분석 모델 식별자
	 * @param expectedVersion 수정 요청 당시의 버전 번호
	 * @param nextVersion 새 버전 번호
	 * @return 수정된 행 수
	 */
	int updateAnalysisModelCurrentVersion(
			@Param("analysisModelId") Long analysisModelId,
			@Param("expectedVersion") Integer expectedVersion,
			@Param("nextVersion") Integer nextVersion);
	
}