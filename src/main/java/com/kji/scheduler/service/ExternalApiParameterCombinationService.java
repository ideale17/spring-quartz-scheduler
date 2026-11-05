
package com.kji.scheduler.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.kji.scheduler.dto.ExternalApiParamDto;
import com.kji.scheduler.dto.ExternalApiParamRepeatValueDto;
import com.kji.scheduler.dto.ExternalApiParameterCombinationDto;

/**
 * External API 파라미터의 반복 값을 생성하고 요청 조합을 구성한다.
 */
@Service
public class ExternalApiParameterCombinationService {
	
	private static final int MAX_COMBINATION_COUNT = 100;
	
	private final DynamicParameterResolver dynamicParameterResolver;
	
	public ExternalApiParameterCombinationService(DynamicParameterResolver dynamicParameterResolver) {
		this.dynamicParameterResolver = dynamicParameterResolver;
	}
	
	/**
	 * External API 파라미터의 전체 실행 조합을 생성한다.
	 *
	 * @param params External API 파라미터 목록
	 * @param executionTime 실행 기준 시간
	 * @return 요청 위치별 파라미터 조합 목록
	 */
	public List<ExternalApiParameterCombinationDto> createCombinations(
			List<ExternalApiParamDto> params, LocalDateTime executionTime) {
		
		// 1. 실행 기준 시간을 검증한다.
		if (executionTime == null) {
			throw new IllegalArgumentException("External API 실행 기준 시간이 없습니다.");
		}
		
		// 2. 최초 요청 조합을 생성한다.
		List<ExternalApiParameterCombinationDto> combinations = new ArrayList<>();
		combinations.add(new ExternalApiParameterCombinationDto());
		
		if (params == null || params.isEmpty()) {
			return combinations;
		}
		
		// 3. 같은 위치에 동일한 이름의 파라미터가 있는지 확인한다.
		Set<String> paramNames = new HashSet<>();
		
		// 4. 파라미터별 값을 생성하고 기존 조합과 결합한다.
		for (ExternalApiParamDto param : params) {
			
			if (param == null || param.getParamName() == null || param.getParamName().isBlank()) {
				throw new IllegalArgumentException("External API 파라미터 이름이 없습니다.");
			}
			
			String location = param.getParamLocation();
			
			if (!"HEADER".equals(location) && !"QUERY".equals(location) && !"BODY".equals(location)) {
				throw new IllegalArgumentException("지원하지 않는 파라미터 위치입니다. paramName: " + param.getParamName());
			}
			
			if (!paramNames.add(location + ":" + param.getParamName())) {
				throw new IllegalArgumentException("동일한 위치에 중복된 파라미터가 있습니다. paramName: " + param.getParamName());
			}
			
			List<String> values = resolveValues(param, executionTime);
			
			// 4-1. 전체 조합 수가 제한을 초과하는지 확인한다.
			if ((long) combinations.size() * values.size() > MAX_COMBINATION_COUNT) {
				throw new IllegalArgumentException("External API 반복 조합은 최대 "
						+ MAX_COMBINATION_COUNT + "개까지 생성할 수 있습니다.");
			}
			
			// 4-2. 기존 조합마다 현재 파라미터 값을 하나씩 추가한다.
			List<ExternalApiParameterCombinationDto> nextCombinations = new ArrayList<>();
			
			for (ExternalApiParameterCombinationDto combination : combinations) {
				
				for (String value : values) {
					
					ExternalApiParameterCombinationDto next = copyCombination(combination);
					
					// 4-3. 필수값을 검증하고, 값이 없는 선택 파라미터는 제외한다.
					if (value == null || value.isBlank()) {
						
						if ("Y".equals(param.getRequiredYn())) {
							throw new IllegalArgumentException("필수 파라미터 값이 없습니다. paramName: " + param.getParamName());
						}
						
					} else {
						addParameter(next, location, param.getParamName(), value);
					}
					
					nextCombinations.add(next);
				}
			}
			
			// 4-4. 생성된 조합을 다음 파라미터 처리에 사용한다.
			combinations = nextCombinations;
		}
		
		// 5. 최종 요청 조합을 반환한다.
		return combinations;
	}
	
	/**
	 * 반복 방식에 따라 실제 파라미터 값 목록을 생성한다.
	 *
	 * @param param 파라미터 설정
	 * @param executionTime 실행 기준 시간
	 * @return 실제 파라미터 값 목록
	 */
	private List<String> resolveValues(ExternalApiParamDto param, LocalDateTime executionTime) {
		
		// 1. 반복 방식의 기본값을 설정한다.
		String repeatType = param.getRepeatType();
		
		if (repeatType == null || repeatType.isBlank()) {
			repeatType = "NONE";
		}
		
		// 2. 반복 방식에 따라 값을 생성한다.
		return switch (repeatType) {
		
			case "NONE" -> {
				String value = dynamicParameterResolver.resolve(param, executionTime);
				yield java.util.Collections.singletonList(value);
			}
			
			case "LIST" -> resolveListValues(param);
			
			case "RANGE" -> resolveRangeValues(param, executionTime);
			
			default -> throw new IllegalArgumentException("지원하지 않는 반복 방식입니다. repeatType: " + repeatType);
		};
	}
	
	/**
	 * LIST 방식의 파라미터 값을 생성한다.
	 *
	 * @param param 파라미터 설정
	 * @return 반복 목록 값
	 */
	private List<String> resolveListValues(ExternalApiParamDto param) {
		
		// 1. LIST 반복에 사용할 값 유형을 검증한다.
		if (!"STATIC".equals(param.getValueType())) {
			throw new IllegalArgumentException("LIST 반복은 STATIC 값 유형에서만 사용할 수 있습니다.");
		}
		
		// 2. 반복 목록이 존재하는지 확인한다.
		List<ExternalApiParamRepeatValueDto> repeatValues = param.getRepeatValues();
		
		if (repeatValues == null || repeatValues.isEmpty()) {
			throw new IllegalArgumentException("반복 목록 값이 없습니다. paramName: " + param.getParamName());
		}
		
		if (repeatValues.size() > MAX_COMBINATION_COUNT) {
			throw new IllegalArgumentException("반복 목록은 최대 100개까지 사용할 수 있습니다.");
		}
		
		// 3. 등록된 순서대로 실제 값을 추출한다.
		List<String> values = new ArrayList<>();
		Set<String> uniqueValues = new HashSet<>();
		
		for (ExternalApiParamRepeatValueDto repeatValue : repeatValues) {
			
			if (repeatValue == null || repeatValue.getParamValue() == null
					|| repeatValue.getParamValue().isBlank()) {
				throw new IllegalArgumentException("반복 목록에 빈 값이 있습니다. paramName: " + param.getParamName());
			}
			
			String value = repeatValue.getParamValue().trim();
			
			if (!uniqueValues.add(value)) {
				throw new IllegalArgumentException("반복 목록에 중복된 값이 있습니다. paramName: " + param.getParamName());
			}
			
			values.add(value);
		}
		
		// 4. 반복 목록 값을 반환한다.
		return values;
	}
	
	/**
	 * RANGE 방식의 날짜 파라미터 값을 생성한다.
	 *
	 * @param param 파라미터 설정
	 * @param executionTime 실행 기준 시간
	 * @return 반복 날짜 값 목록
	 */
	private List<String> resolveRangeValues(ExternalApiParamDto param, LocalDateTime executionTime) {
		
		// 1. RANGE 반복에 사용할 값 유형을 검증한다.
		if (!"CURRENT_DATE".equals(param.getValueType())) {
			throw new IllegalArgumentException("RANGE 반복은 CURRENT_DATE 값 유형에서만 사용할 수 있습니다.");
		}
		
		// 2. 반복 단위와 시작·종료·간격을 검증한다.
		String repeatUnit = param.getRepeatUnit();
		Integer start = param.getRepeatStart();
		Integer end = param.getRepeatEnd();
		Integer step = param.getRepeatStep();
		
		if (!"DAY".equals(repeatUnit) && !"MONTH".equals(repeatUnit) && !"YEAR".equals(repeatUnit)) {
			throw new IllegalArgumentException("지원하지 않는 날짜 반복 단위입니다. paramName: " + param.getParamName());
		}
		
		if (start == null || end == null || step == null || step <= 0
				|| start < -10000 || start > 10000
				|| end < -10000 || end > 10000 || start > end) {
			throw new IllegalArgumentException("날짜 반복 범위가 올바르지 않습니다. paramName: " + param.getParamName());
		}
		
		long count = ((long) end - start) / step + 1;
		
		if (count > MAX_COMBINATION_COUNT) {
			throw new IllegalArgumentException("날짜 반복은 최대 100개까지 생성할 수 있습니다.");
		}
		
		// 3. 기존 오프셋을 먼저 적용하여 반복 기준 시간을 계산한다.
		LocalDateTime baseTime = applyOffset(
				executionTime, param.getOffsetUnit(), param.getOffsetValue()
		);
		
		// 4. 반복 계산에 사용할 임시 파라미터를 생성한다.
		ExternalApiParamDto rangeParam = new ExternalApiParamDto();
		rangeParam.setParamName(param.getParamName());
		rangeParam.setValueType(param.getValueType());
		rangeParam.setValueFormat(param.getValueFormat());
		rangeParam.setOffsetUnit(repeatUnit);
		
		// 5. 시작부터 종료까지 날짜 값을 생성한다.
		List<String> values = new ArrayList<>();
		Set<String> uniqueValues = new HashSet<>();
		
		for (long offset = start; offset <= end; offset += step) {
			
			rangeParam.setOffsetValue((int) offset);
			
			String value = dynamicParameterResolver.resolve(rangeParam, baseTime);
			
			if (!uniqueValues.add(value)) {
				throw new IllegalArgumentException("날짜 반복 결과에 중복된 값이 있습니다. paramName: " + param.getParamName());
			}
			
			values.add(value);
		}
		
		// 6. 날짜 반복 결과를 반환한다.
		return values;
	}
	
	/**
	 * 기존 파라미터 오프셋을 실행 기준 시간에 적용한다.
	 *
	 * @param executionTime 실행 기준 시간
	 * @param offsetUnit 기존 오프셋 단위
	 * @param offsetValue 기존 오프셋 값
	 * @return 오프셋이 적용된 시간
	 */
	private LocalDateTime applyOffset(LocalDateTime executionTime, String offsetUnit, Integer offsetValue) {
		
		// 1. 오프셋이 없으면 실행 기준 시간을 유지한다.
		if (offsetUnit == null || offsetUnit.isBlank()) {
			return executionTime;
		}
		
		// 2. 기존 오프셋을 날짜에 적용한다.
		int value = offsetValue == null ? 0 : offsetValue;
		
		return switch (offsetUnit) {
			case "MINUTE" -> executionTime.plusMinutes(value);
			case "HOUR" -> executionTime.plusHours(value);
			case "DAY" -> executionTime.plusDays(value);
			case "MONTH" -> executionTime.plusMonths(value);
			case "YEAR" -> executionTime.plusYears(value);
			default -> throw new IllegalArgumentException("지원하지 않는 날짜 오프셋 단위입니다. offsetUnit: " + offsetUnit);
		};
	}
	
	/**
	 * 기존 요청 조합을 복사한다.
	 *
	 * @param source 복사할 요청 조합
	 * @return 복사된 요청 조합
	 */
	private ExternalApiParameterCombinationDto copyCombination(ExternalApiParameterCombinationDto source) {
		
		// 1. 새로운 요청 조합을 생성한다.
		ExternalApiParameterCombinationDto target = new ExternalApiParameterCombinationDto();
		
		// 2. 기존 요청 위치별 파라미터를 복사한다.
		target.setHeaders(new LinkedHashMap<>(source.getHeaders()));
		target.setQueryParams(new LinkedHashMap<>(source.getQueryParams()));
		target.setBodyParams(new LinkedHashMap<>(source.getBodyParams()));
		
		return target;
	}
	
	/**
	 * 요청 위치에 따라 파라미터를 추가한다.
	 *
	 * @param combination 현재 요청 조합
	 * @param location 파라미터 위치
	 * @param name 파라미터 이름
	 * @param value 파라미터 값
	 */
	private void addParameter(ExternalApiParameterCombinationDto combination, String location, String name, String value) {
		
		// 1. 요청 위치에 따라 값을 설정한다.
		Map<String, String> target = switch (location) {
			case "HEADER" -> combination.getHeaders();
			case "QUERY" -> combination.getQueryParams();
			case "BODY" -> combination.getBodyParams();
			default -> throw new IllegalArgumentException("지원하지 않는 파라미터 위치입니다. " + location);
		};
		
		target.put(name, value);
	}
	
}
