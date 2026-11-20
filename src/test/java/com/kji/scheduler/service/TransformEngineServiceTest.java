package com.kji.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kji.scheduler.dto.TransformRuleFieldDto;
import com.kji.scheduler.dto.TransformRuleVersionDto;

class TransformEngineServiceTest {
	
	private TransformEngineService service;
	private TransformRuleVersionDto version;
	
	@BeforeEach
	void setUp() {
		service = new TransformEngineService(new ObjectMapper());
		
		version = new TransformRuleVersionDto();
		version.setDataFormat("JSON");
	}
	
	@Test
	void 단일객체_가공() {
		
		String rawData = """
				{
				  "current": {
				    "temperature": 16.7,
				    "humidity": 36
				  }
				}
				""";
		
		List<TransformRuleFieldDto> fields = List.of(
				createField("current.temperature", "temp", "DECIMAL", "Y"),
				createField("current.humidity", "hum", "INTEGER", "Y"));
		
		JsonNode result = service.transformJson(rawData, version, fields);
		
		assertEquals(16.7, result.get("temp").doubleValue());
		assertEquals(36, result.get("hum").intValue());
	}
	
	@Test
	void 반복배열_가공() {
		
		String rawData = """
				{
				  "features": [
				    {"id": "eq001"},
				    {"id": "eq002"}
				  ]
				}
				""";
		
		version.setRepeatPath("features");
		
		List<TransformRuleFieldDto> fields = List.of(createField("id", "earthquakeId", "STRING", "Y"));
		
		JsonNode result = service.transformJson(rawData, version, fields);
		
		assertEquals(2, result.size());
		assertEquals("eq001", result.get(0).get("earthquakeId").asText());
		assertEquals("eq002", result.get(1).get("earthquakeId").asText());
	}
	
	@Test
	void 필수필드_누락() {
		
		String rawData = """
				{"temperature": 16.7}
				""";
		
		List<TransformRuleFieldDto> fields = List.of(
				createField("humidity", "hum", "INTEGER", "Y"));
		
		assertThrows(IllegalArgumentException.class,
				() -> service.transformJson(rawData, version, fields));
	}
	
	private TransformRuleFieldDto createField(String sourcePath, String targetName, String dataType, String requiredYn) {
		
		TransformRuleFieldDto field = new TransformRuleFieldDto();
		field.setSourcePaths(List.of(sourcePath));
		field.setTransformType("DIRECT");
		field.setTargetName(targetName);
		field.setDataType(dataType);
		field.setRequiredYn(requiredYn);
		
		return field;
	}
	
	/**
	 * 여러 원본 경로를 사용하는 결합 필드 설정을 생성한다.
	 *
	 * @param sourcePaths 원본 필드 경로 목록
	 * @param targetName 출력 필드명
	 * @param transformType 가공 유형
	 * @param requiredYn 필수 여부
	 * @return 결합 필드 설정
	 */
	private TransformRuleFieldDto createCombineField(
			List<String> sourcePaths, String targetName, String transformType, String requiredYn) {

		TransformRuleFieldDto field = new TransformRuleFieldDto();
		field.setSourcePaths(sourcePaths);
		field.setTransformType(transformType);
		field.setTargetName(targetName);
		field.setDataType("STRING");
		field.setRequiredYn(requiredYn);

		return field;
	}

	@Test
	void XML_아파트거래_반복가공() {
		
		// 1. XML 샘플과 반복 경로를 준비한다.
		version.setDataFormat("XML");
		version.setRepeatPath("response.body.items.item");
		
		String rawData = """
				<response>
					<body>
						<items>
							<item>
								<aptNm>동부센트레빌</aptNm>
								<dealAmount>84,000</dealAmount>
								<dealYear>2026</dealYear>
								<excluUseAr>84.96</excluUseAr>
							</item>
							<item>
								<aptNm>염창3우성</aptNm>
								<dealAmount>108,000</dealAmount>
								<dealYear>2026</dealYear>
								<excluUseAr>84.93</excluUseAr>
							</item>
						</items>
					</body>
				</response>
				""";
		
		List<TransformRuleFieldDto> fields = List.of(
				createField("aptNm", "apartmentName", "STRING", "Y"),
				createField("dealAmount", "dealAmount", "STRING", "Y"),
				createField("dealYear", "dealYear", "INTEGER", "Y"),
				createField("excluUseAr", "exclusiveArea", "DECIMAL", "Y"));
		
		// 2. XML 가공을 실행한다.
		JsonNode result = service.transformXml(rawData, version, fields);
		
		// 3. 반복 건수와 타입 변환 결과를 검증한다.
		assertEquals(2, result.size());
		assertEquals("동부센트레빌", result.get(0).get("apartmentName").asText());
		assertEquals("84,000", result.get(0).get("dealAmount").asText());
		assertEquals(2026, result.get(0).get("dealYear").intValue());
		assertEquals(84.96, result.get(0).get("exclusiveArea").doubleValue());
		assertEquals("염창3우성", result.get(1).get("apartmentName").asText());
	}

	@Test
	void XML_필수필드_누락() {
		
		version.setDataFormat("XML");
		version.setRepeatPath("response.body.items.item");
		
		String rawData = """
				<response>
					<body>
						<items>
							<item><aptNm> </aptNm></item>
						</items>
					</body>
				</response>
				""";
		
		List<TransformRuleFieldDto> fields = List.of(
				createField("aptNm", "apartmentName", "STRING", "Y"));
		
		assertThrows(IllegalArgumentException.class,
				() -> service.transformXml(rawData, version, fields));
	}
	
	@Test
	void XML_DTD_사용차단() {
		
		version.setDataFormat("XML");
		
		String rawData = """
				<!DOCTYPE response [
					<!ENTITY example SYSTEM "file:///etc/passwd">
				]>
				<response><name>&example;</name></response>
				""";
		
		List<TransformRuleFieldDto> fields = List.of(
				createField("name", "name", "STRING", "Y"));
		
		assertThrows(IllegalArgumentException.class,
				() -> service.transformXml(rawData, version, fields));
	}
	
	@Test
	void ISO_DATETIME_한국시간을_UTC로_변환() {
		
		// 1. 한국 시간대가 포함된 날짜 데이터를 준비한다.
		String rawData = """
				{
				  "eventTime": "2026-10-24T12:30:00+09:00"
				}
				""";
		
		TransformRuleFieldDto field = createField("eventTime", "occurredAt", "DATETIME", "Y");
		field.setValueFormat("ISO_DATETIME");
		
		// 2. 가공 규칙을 적용한다.
		JsonNode result = service.transformJson(rawData, version, List.of(field));
		
		// 3. UTC 기준으로 변환되었는지 확인한다.
		assertEquals("2026-10-24T03:30:00Z", result.get("occurredAt").asText());
	}
	
	@Test
	void ISO_DATETIME_UTC시간을_그대로_유지() {
		
		// 1. UTC 기준 날짜 데이터를 준비한다.
		String rawData = """
				{
				  "eventTime": "2026-10-24T03:30:00Z"
				}
				""";
		
		TransformRuleFieldDto field = createField("eventTime", "occurredAt", "DATETIME", "Y");
		field.setValueFormat("ISO_DATETIME");
		
		// 2. 가공 규칙을 적용한다.
		JsonNode result = service.transformJson(rawData, version, List.of(field));
		
		// 3. 동일한 UTC 시각을 유지하는지 확인한다.
		assertEquals("2026-10-24T03:30:00Z", result.get("occurredAt").asText());
	}
	
	@Test
	void ISO_DATETIME_시간대정보_누락시_예외발생() {
		
		// 1. 시간대 정보가 없는 날짜 데이터를 준비한다.
		String rawData = """
				{
				  "eventTime": "2026-10-24T12:30:00"
				}
				""";
		
		TransformRuleFieldDto field = createField("eventTime", "occurredAt", "DATETIME", "Y");
		field.setValueFormat("ISO_DATETIME");
		
		// 2. 시간대가 없는 날짜는 변환을 거부하는지 확인한다.
		assertThrows(IllegalArgumentException.class,
				() -> service.transformJson(rawData, version, List.of(field)));
	}
	
	@Test
	void EPOCH_MILLIS_기존변환_유지() {
		
		// 1. Unix 타임스탬프(밀리초)를 준비한다.
		String rawData = """
				{
				  "eventTime": 1792812600000
				}
				""";
		
		TransformRuleFieldDto field = createField("eventTime", "occurredAt", "DATETIME", "Y");
		field.setValueFormat("EPOCH_MILLIS");
		
		// 2. 가공 규칙을 적용한다.
		JsonNode result = service.transformJson(rawData, version, List.of(field));
		
		// 3. 기존 밀리초 변환 결과가 유지되는지 확인한다.
		assertEquals("2026-10-24T03:30:00Z", result.get("occurredAt").asText());
	}
	
	@Test
	void XML_ISO_DATETIME_한국시간을_UTC로_변환() {
		
		// 1. 날짜를 포함한 XML 데이터를 준비한다.
		version.setDataFormat("XML");
		
		String rawData = """
				<response>
					<eventTime>2026-10-24T12:30:00+09:00</eventTime>
				</response>
				""";
		
		TransformRuleFieldDto field = createField("eventTime", "occurredAt", "DATETIME", "Y");
		field.setValueFormat("ISO_DATETIME");
		
		// 2. XML 가공 규칙을 적용한다.
		JsonNode result = service.transformXml(rawData, version, List.of(field));
		
		// 3. UTC 기준으로 변환되었는지 확인한다.
		assertEquals("2026-10-24T03:30:00Z", result.get(0).get("occurredAt").asText());
	}
	

	@Test
	void JSON_CONCAT_구분자로_문자열결합() {

		// 1. 결합할 원본 문자열과 필드 설정을 준비한다.
		String rawData = """
				{
				  "roadNm": "테헤란로",
				  "buildingNo": "123"
				}
				""";

		TransformRuleFieldDto field = createCombineField(
				List.of("roadNm", "buildingNo"), "address", "CONCAT", "Y");
		field.setSeparator(" ");

		// 2. 가공한 문자열에 구분자가 적용되는지 확인한다.
		JsonNode result = service.transformJson(rawData, version, List.of(field));

		assertEquals("테헤란로 123", result.get("address").asText());
	}

	@Test
	void JSON_CONCAT_빈구분자로_문자열결합() {

		// 1. 구분자 없이 결합할 코드 값을 준비한다.
		String rawData = """
				{"sggCd":"11680","umdCd":"10100"}
				""";

		TransformRuleFieldDto field = createCombineField(
				List.of("sggCd", "umdCd"), "fullCode", "CONCAT", "Y");
		field.setSeparator("");

		// 2. 입력 순서대로 값이 결합되는지 확인한다.
		JsonNode result = service.transformJson(rawData, version, List.of(field));

		assertEquals("1168010100", result.get("fullCode").asText());
	}

	@Test
	void JSON_DATE_COMBINE_한자리_월일을_ISO날짜로_변환() {

		// 1. 한 자리 월일을 포함한 거래 데이터를 준비한다.
		String rawData = """
				{"dealYear":"2026","dealMonth":"1","dealDay":"7"}
				""";

		TransformRuleFieldDto field = createCombineField(
				List.of("dealYear", "dealMonth", "dealDay"), "contractDate", "DATE_COMBINE", "Y");

		// 2. 유효한 ISO 날짜 문자열로 변환되는지 확인한다.
		JsonNode result = service.transformJson(rawData, version, List.of(field));

		assertEquals("2026-01-07", result.get("contractDate").asText());
	}

	@Test
	void JSON_DATE_COMBINE_존재하지_않는_날짜는_예외() {

		// 1. 존재하지 않는 2월 30일 데이터를 준비한다.
		String rawData = """
				{"dealYear":"2026","dealMonth":"2","dealDay":"30"}
				""";

		TransformRuleFieldDto field = createCombineField(
				List.of("dealYear", "dealMonth", "dealDay"), "contractDate", "DATE_COMBINE", "Y");

		// 2. 잘못된 날짜를 거부하는지 확인한다.
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> service.transformJson(rawData, version, List.of(field)));

		assertTrue(exception.getMessage().contains("날짜 결합에 실패했습니다"));
	}

	@Test
	void JSON_DATE_COMBINE_숫자가_아닌_월은_예외() {

		// 1. 숫자가 아닌 월 값이 포함된 데이터를 준비한다.
		String rawData = """
				{"dealYear":"2026","dealMonth":"ABC","dealDay":"17"}
				""";

		TransformRuleFieldDto field = createCombineField(
				List.of("dealYear", "dealMonth", "dealDay"), "contractDate", "DATE_COMBINE", "Y");

		// 2. 숫자 변환 실패도 날짜 결합 오류로 처리하는지 확인한다.
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> service.transformJson(rawData, version, List.of(field)));

		assertTrue(exception.getMessage().contains("날짜 결합에 실패했습니다"));
	}

	@Test
	void JSON_DATE_COMBINE_선택필드_누락시_NULL() {

		// 1. 일자가 누락된 데이터를 준비한다.
		String rawData = """
				{"dealYear":"2026","dealMonth":"11"}
				""";

		TransformRuleFieldDto field = createCombineField(
				List.of("dealYear", "dealMonth", "dealDay"), "contractDate", "DATE_COMBINE", "N");

		// 2. 선택값 누락 시 JSON null을 반환하는지 확인한다.
		JsonNode result = service.transformJson(rawData, version, List.of(field));

		assertTrue(result.get("contractDate").isNull());
	}

	@Test
	void JSON_DATE_COMBINE_필수필드_누락시_예외() {

		// 1. 일자가 누락된 데이터를 준비한다.
		String rawData = """
				{"dealYear":"2026","dealMonth":"11"}
				""";

		TransformRuleFieldDto field = createCombineField(
				List.of("dealYear", "dealMonth", "dealDay"), "contractDate", "DATE_COMBINE", "Y");

		// 2. 필수값 누락 시 예외가 발생하는지 확인한다.
		assertThrows(IllegalArgumentException.class,
				() -> service.transformJson(rawData, version, List.of(field)));
	}

	@Test
	void XML_CONCAT_문자열결합() {

		// 1. 결합할 XML 원본과 필드 설정을 준비한다.
		version.setDataFormat("XML");

		String rawData = """
				<response>
					<roadNm>테헤란로</roadNm>
					<buildingNo>123</buildingNo>
				</response>
				""";

		TransformRuleFieldDto field = createCombineField(
				List.of("roadNm", "buildingNo"), "address", "CONCAT", "Y");
		field.setSeparator(" ");

		// 2. XML 원본 필드가 결합되는지 확인한다.
		JsonNode result = service.transformXml(rawData, version, List.of(field));

		assertEquals("테헤란로 123", result.get(0).get("address").asText());
	}

	@Test
	void XML_DATE_COMBINE_반복데이터_날짜결합() {

		// 1. 반복되는 XML 거래 데이터를 준비한다.
		version.setDataFormat("XML");
		version.setRepeatPath("response.body.items.item");

		String rawData = """
				<response>
					<body>
						<items>
							<item><dealYear>2026</dealYear><dealMonth>1</dealMonth><dealDay>7</dealDay></item>
							<item><dealYear>2026</dealYear><dealMonth>11</dealMonth><dealDay>20</dealDay></item>
						</items>
					</body>
				</response>
				""";

		TransformRuleFieldDto field = createCombineField(
				List.of("dealYear", "dealMonth", "dealDay"), "contractDate", "DATE_COMBINE", "Y");

		// 2. 각 항목에 정확한 날짜가 생성되는지 확인한다.
		JsonNode result = service.transformXml(rawData, version, List.of(field));

		assertEquals(2, result.size());
		assertEquals("2026-01-07", result.get(0).get("contractDate").asText());
		assertEquals("2026-11-20", result.get(1).get("contractDate").asText());
	}

}