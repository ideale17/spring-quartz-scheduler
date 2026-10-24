
package com.kji.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
		field.setSourcePath(sourcePath);
		field.setTargetName(targetName);
		field.setDataType(dataType);
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
	
}
