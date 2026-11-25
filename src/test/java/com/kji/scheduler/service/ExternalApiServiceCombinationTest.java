package com.kji.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.kji.scheduler.crypto.AesGcmEncryptionService;
import com.kji.scheduler.dto.ExternalApiDto;
import com.kji.scheduler.dto.ExternalApiParamDto;
import com.kji.scheduler.dto.ExternalApiRequestDto;
import com.kji.scheduler.mapper.ExternalApiMapper;
import com.kji.scheduler.mapper.TransformAutoConfigMapper;

class ExternalApiServiceCombinationTest {

	private final ExternalApiMapper mapper = mock(ExternalApiMapper.class);
	private final ExternalApiService service = new ExternalApiService(
			mapper, mock(TransformAutoConfigMapper.class), mock(DynamicJobService.class),
			mock(AesGcmEncryptionService.class));

	@Test
	void createRejectsExcessiveCombinationCountBeforeWrite() {
		ExternalApiService createService = spy(service);
		ExternalApiDto api = new ExternalApiDto();
		api.setApiName("과거 데이터 조회");
		api.setApiUrl("https://example.test/history");
		api.setHttpMethod("GET");
		doNothing().when(createService).validateExternalApiUrl(api.getApiUrl());

		ExternalApiRequestDto request = new ExternalApiRequestDto();
		request.setExternalApi(api);
		request.setParams(List.of(monthRange("startMonth", 7), monthRange("endMonth", 43)));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> createService.createExternalApi(request));

		assertEquals("External API 반복 조합은 최대 300개까지 생성할 수 있습니다.", exception.getMessage());
		verifyNoInteractions(mapper);
	}

	@Test
	void updateRejectsExcessiveCombinationCountBeforeDeletingExistingParams() {
		ExternalApiDto api = new ExternalApiDto();
		api.setAuthType("NONE");
		when(mapper.findExternalApi(1L)).thenReturn(api);
		List<ExternalApiParamDto> params = List.of(monthRange("startMonth", 7), monthRange("endMonth", 43));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> service.updateExternalApiParams(1L, params));

		assertEquals("External API 반복 조합은 최대 300개까지 생성할 수 있습니다.", exception.getMessage());
		verify(mapper).findExternalApi(1L);
		verifyNoMoreInteractions(mapper);
	}

	@Test
	void updateAcceptsThreeHundredCombinations() {
		ExternalApiDto api = new ExternalApiDto();
		api.setAuthType("NONE");
		when(mapper.findExternalApi(1L)).thenReturn(api);
		when(mapper.insertExternalApiParam(any())).thenAnswer(invocation -> {
			ExternalApiParamDto param = invocation.getArgument(0);
			param.setParamId(1L);
			return 1;
		});

		service.updateExternalApiParams(1L, List.of(monthRange("startMonth", 25), monthRange("endMonth", 12)));

		verify(mapper).deleteExternalApiParams(1L);
		verify(mapper, times(2)).insertExternalApiParam(any());
	}

	private ExternalApiParamDto monthRange(String name, int count) {
		ExternalApiParamDto param = new ExternalApiParamDto();
		param.setParamName(name);
		param.setParamLocation("QUERY");
		param.setValueType("CURRENT_DATE");
		param.setRepeatType("RANGE");
		param.setRepeatUnit("MONTH");
		param.setRepeatStart(1 - count);
		param.setRepeatEnd(0);
		return param;
	}
}
