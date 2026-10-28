package com.kji.scheduler.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kji.scheduler.dto.AiAnalysisDataDto;
import com.kji.scheduler.dto.AiAnalysisSearchDto;
import com.kji.scheduler.dto.AiFieldStatisticsDto;
import com.kji.scheduler.mapper.AiAnalysisMapper;

class AiAnalysisServiceTest {
	
	@Test
	void 숫자필드_통계계산_성공() {
		
		// 1. 데이터 조회 Mapper와 AI 클라이언트를 Mock으로 생성한다.
		AiAnalysisMapper mapper = mock(AiAnalysisMapper.class);
		OpenAiAnalysisClient client = mock(OpenAiAnalysisClient.class);
		
		AiAnalysisService service = new AiAnalysisService(
				mapper, new ObjectMapper(), client);
		
		// 2. 테스트용 가공 결과 JSON을 구성한다.
		AiAnalysisDataDto result = new AiAnalysisDataDto();
		result.setTransformResultId(1L);
		result.setResultData("""
				[
				  {"dealAmount": "10,000"},
				  {"dealAmount": 20000},
				  {"dealAmount": "30,000"},
				  {"dealAmount": null},
				  {},
				  {"dealAmount": "invalid"}
				]
				""");
		
		when(mapper.findAnalysisData(any(AiAnalysisSearchDto.class)))
				.thenReturn(List.of(result));
		when(mapper.countAnalysisData(any(AiAnalysisSearchDto.class)))
				.thenReturn(1L);
		
		// 3. 검색조건을 구성한다.
		AiAnalysisSearchDto searchDto = new AiAnalysisSearchDto();
		searchDto.setExternalApiId(4L);
		searchDto.setTransformRuleId(6L);
		searchDto.setVersionNo(1);
		searchDto.setStartDate(LocalDate.of(2026, 10, 26));
		searchDto.setEndDate(LocalDate.of(2026, 10, 27));
		
		// 4. 거래금액 통계를 계산한다.
		AiFieldStatisticsDto statistics = service.calculateFieldStatistics(searchDto, "dealAmount");
		
		// 5. 통계 결과를 검증한다.
		assertEquals(6, statistics.getTotalRecordCount());
		assertEquals(3, statistics.getValidCount());
		assertEquals(1, statistics.getNullCount());
		assertEquals(1, statistics.getMissingCount());
		assertEquals(1, statistics.getInvalidCount());
		
		assertEquals(0, new BigDecimal("60000").compareTo(statistics.getSum()));
		assertEquals(0, new BigDecimal("10000").compareTo(statistics.getMin()));
		assertEquals(0, new BigDecimal("30000").compareTo(statistics.getMax()));
		assertEquals(0, new BigDecimal("20000").compareTo(statistics.getAverage()));
		
		assertTrue(statistics.isComplete());
	}
	
}