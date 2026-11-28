package com.kji.scheduler.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kji.scheduler.dto.AnalysisLoadColumnValueDto;
import com.kji.scheduler.dto.AnalysisLoadListDto;
import com.kji.scheduler.dto.AnalysisLoadResultSearchDto;
import com.kji.scheduler.dto.AnalysisLoadSearchDto;
import com.kji.scheduler.dto.AnalysisLoadSourceDto;

@Mapper
public interface AnalysisLoadMapper {
	
	/**
	 * 적재 대상 테이블의 기본키 컬럼 목록을 조회한다.
	 *
	 * @param schema 대상 스키마
	 * @param table 대상 테이블
	 * @return 기본키 컬럼 목록
	 */
	List<String> findResultPrimaryKeyColumns(
			@Param("schema") String schema,
			@Param("table") String table);
	
	/**
	 * 적재 대상 테이블에 저장된 전체 데이터 건수를 조회한다.
	 *
	 * @param target 검증된 대상 테이블 식별자
	 * @return 전체 데이터 건수
	 */
	long countAnalysisLoadResults(@Param("target") String target);
	
	/**
	 * 검색조건에 해당하는 적재 결과 목록을 조회한다.
	 *
	 * @param searchDto 검색조건 및 페이징 정보
	 * @param target 검증된 대상 테이블 식별자
	 * @param projection 조회 대상 컬럼
	 * @param sort 기본키 기준 정렬 조건
	 * @return JSON 형태의 적재 결과 목록
	 */
	List<String> findAnalysisLoadResults(
			@Param("searchDto") AnalysisLoadResultSearchDto searchDto,
			@Param("target") String target,
			@Param("projection") String projection,
			@Param("sort") String sort);
	
	/**
	 * 가공 완료 결과를 기반으로 적재 대기 작업을 등록한다.
	 *
	 * @return 등록된 행 수. 결과가 적재 모델 버전과
	 *         일치하지 않으면 0을 반환한다.
	 */
	int insertAnalysisLoadSource(
			@Param("analysisModelVersionId") Long analysisModelVersionId,
			@Param("transformRuleVersionId") Long transformRuleVersionId,
			@Param("transformResultId") Long transformResultId);
	
	/**
	 * 적재 모델에 연결된 가공 규칙의 버전 번호를 조회한다.
	 */
	Integer findTransformRuleVersionNo(@Param("transformRuleVersionId") Long transformRuleVersionId);
	
	/**
	 * 선택한 적재 모델 기준으로 가공 결과 및 적재 상태를 조회한다.
	 */
	List<AnalysisLoadListDto> findAnalysisLoadList(
			@Param("searchDto") AnalysisLoadSearchDto searchDto,
			@Param("transformRuleId") Long transformRuleId,
			@Param("analysisModelVersionId") Long analysisModelVersionId,
			@Param("transformRuleVersionId") Long transformRuleVersionId,
			@Param("startAt") LocalDateTime startAt,
			@Param("endAt") LocalDateTime endAt);
	
	/**
	 * 동일한 조건의 가공 결과 전체 건수를 조회한다.
	 */
	long countAnalysisLoadList(
			@Param("searchDto") AnalysisLoadSearchDto searchDto,
			@Param("transformRuleId") Long transformRuleId,
			@Param("analysisModelVersionId") Long analysisModelVersionId,
			@Param("transformRuleVersionId") Long transformRuleVersionId,
			@Param("startAt") LocalDateTime startAt,
			@Param("endAt") LocalDateTime endAt);
	
	/**
	 * 대기 중인 적재 작업을 최대 지정 건수만큼 원자적으로 선점한다.
	 *
	 * @param batchSize 최대 선점 건수
	 * @return 선점한 적재 작업 목록
	 */
	List<AnalysisLoadSourceDto> claimPendingLoads(@Param("batchSize") int batchSize);
	
	/**
	 * 적재 작업 식별자로 실행에 필요한 모델 및 가공 결과 정보를 조회한다.
	 *
	 * @param analysisLoadSourceId 적재 작업 식별자
	 * @return 적재 실행 정보, 없으면 null
	 */
	AnalysisLoadSourceDto findAnalysisLoadSource(@Param("analysisLoadSourceId") Long analysisLoadSourceId);
	
	/**
	 * 변환된 분석 데이터를 업무 테이블에 저장한다.
	 *
	 * @param analysisLoadSourceId 적재 작업 식별자
	 * @param sourceItemNo 가공 결과 배열 항목 순번
	 * @param values DB 컬럼과 변환된 값 목록
	 * @return 저장된 행 수
	 */
	int insertRealEstateSaleTrade(
			@Param("analysisLoadSourceId") Long analysisLoadSourceId,
			@Param("sourceItemNo") int sourceItemNo,
			@Param("values") List<AnalysisLoadColumnValueDto> values);
	
	/**
	 * 적재 작업을 성공 상태로 변경하고 실제 적재 건수를 저장한다.
	 *
	 * @param analysisLoadSourceId 적재 작업 식별자
	 * @param loadedCount 적재 완료 건수
	 * @return 변경된 행 수
	 */
	int markLoadSuccess(
			@Param("analysisLoadSourceId") Long analysisLoadSourceId,
			@Param("loadedCount") int loadedCount);
	
	/**
	 * 적재 작업을 실패 상태로 변경하고 오류 메시지를 저장한다.
	 *
	 * @param analysisLoadSourceId 적재 작업 식별자
	 * @param errorMessage 실패 원인
	 * @return 변경된 행 수
	 */
	int markLoadFailed(
			@Param("analysisLoadSourceId") Long analysisLoadSourceId,
			@Param("errorMessage") String errorMessage);
	
}
