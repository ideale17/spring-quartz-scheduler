package com.kji.scheduler.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kji.scheduler.dto.AnalysisLoadListDto;
import com.kji.scheduler.dto.AnalysisLoadSearchDto;

@Mapper
public interface AnalysisLoadMapper {
	
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
			@Param("startAt") LocalDateTime startAt,
			@Param("endAt") LocalDateTime endAt);
	
	/**
	 * 동일한 조건의 가공 결과 전체 건수를 조회한다.
	 */
	long countAnalysisLoadList(
			@Param("searchDto") AnalysisLoadSearchDto searchDto,
			@Param("transformRuleId") Long transformRuleId,
			@Param("startAt") LocalDateTime startAt,
			@Param("endAt") LocalDateTime endAt);
	
}