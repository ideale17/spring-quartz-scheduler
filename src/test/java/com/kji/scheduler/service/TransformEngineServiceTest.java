
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
	
}
