
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
	
}
