
package com.kji.scheduler.service;

import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kji.scheduler.dto.TransformRuleFieldDto;
import com.kji.scheduler.dto.TransformRuleVersionDto;

@Service
public class TransformEngineService {
	
	private final ObjectMapper objectMapper;
	
	public TransformEngineService(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}
	
	/**
	 * JSON 원본 데이터에 가공 규칙을 적용한다.
	 *
	 * @param rawData 원본 JSON 문자열
	 * @param version 가공 규칙 버전
	 * @param fields 추출 필드 목록
	 * @return 가공된 JSON 데이터
	 */
	public JsonNode transformJson(String rawData, TransformRuleVersionDto version, List<TransformRuleFieldDto> fields) {
		
		// 1. 원본 데이터와 가공 규칙을 검증한다.
		if (rawData == null || rawData.isBlank()) {
			throw new IllegalArgumentException("가공할 JSON 데이터가 없습니다.");
		}
		
		if (version == null || !"JSON".equals(version.getDataFormat())) {
			throw new IllegalArgumentException("JSON 가공 규칙이 올바르지 않습니다.");
		}
		
		if (fields == null || fields.isEmpty()) {
			throw new IllegalArgumentException("가공할 추출 필드가 없습니다.");
		}
		
		try {
			// 2. 원본 JSON을 파싱한다.
			JsonNode root = objectMapper.readTree(rawData);
			
			if (root == null || (!root.isObject() && !root.isArray())) {
				throw new IllegalArgumentException("JSON 원본은 객체 또는 배열이어야 합니다.");
			}
			
			// 3. 반복 경로에 해당하는 가공 대상을 조회한다.
			String repeatPath = version.getRepeatPath();
			boolean hasRepeatPath = repeatPath != null && !repeatPath.isBlank();
			
			JsonNode target = hasRepeatPath ? getSourceValue(root, repeatPath.trim()) : root;
			
			if (target == null || target.isNull() || target.isMissingNode()) {
				throw new IllegalArgumentException("반복 경로에 해당하는 데이터가 없습니다.");
			}
			
			if (hasRepeatPath && !target.isArray()) {
				throw new IllegalArgumentException("반복 경로는 JSON 배열을 가리켜야 합니다.");
			}
			
			// 4. 배열과 단일 객체를 구분하여 가공한다.
			if (target.isArray()) {
				ArrayNode results = objectMapper.createArrayNode();
				
				for (int i = 0; i < target.size(); i++) {
					results.add(transformItem(target.get(i), fields, i + 1));
				}
				
				return results;
			}
			
			return transformItem(target, fields, 1);
			
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("JSON 형식이 올바르지 않습니다.", e);
		}
		
	}

	/**
	 * 점으로 구분된 경로를 이용해 JSON 값을 조회한다.
	 *
	 * @param data 탐색 대상 JSON
	 * @param sourcePath 원본 필드 경로
	 * @return 경로에 해당하는 JSON 값
	 */
	private JsonNode getSourceValue(JsonNode data, String sourcePath) {
		
		if (sourcePath == null || sourcePath.isBlank()) {
			return null;
		}
		
		JsonNode value = data;
		
		for (String key : sourcePath.split("\\.", -1)) {
			if (key.isEmpty() || value == null || !value.isObject()) {
				return null;
			}
			
			value = value.get(key);
		}
		
		return value;
	}

	/**
	 * JSON 객체 한 건의 추출 필드를 가공한다.
	 *
	 * @param item 가공 대상 객체
	 * @param fields 추출 필드 목록
	 * @param index 가공 대상 순번
	 * @return 가공된 JSON 객체
	 */
	private ObjectNode transformItem(JsonNode item, List<TransformRuleFieldDto> fields, int index) {
		
		// 1. 가공 대상이 JSON 객체인지 확인한다.
		if (item == null || !item.isObject()) {
			throw new IllegalArgumentException(index + "번째 가공 대상은 JSON 객체여야 합니다.");
		}
		
		ObjectNode result = objectMapper.createObjectNode();
		
		// 2. 추출 필드별로 경로 조회 및 타입 변환을 수행한다.
		for (TransformRuleFieldDto field : fields) {
			if (field == null || field.getSourcePaths() == null || field.getSourcePaths().isEmpty()
					|| field.getTargetName() == null || field.getTargetName().isBlank()) {
				throw new IllegalArgumentException("가공 규칙 추출 필드 정보가 올바르지 않습니다.");
			}
			
			try {
				// 3. 가공 유형과 원본 경로 목록을 확인한다.
				List<String> sourcePaths = field.getSourcePaths();
				String transformType = field.getTransformType() == null ? "DIRECT" : field.getTransformType();
				
				JsonNode converted;
				
				if ("DIRECT".equals(transformType)) {
					// 4. 단일 원본 필드를 조회하여 기존 타입 변환을 수행한다.
					if (sourcePaths.size() != 1) {
						throw new IllegalArgumentException("DIRECT 원본 경로는 1개여야 합니다.");
					}
					
					JsonNode value = getSourceValue(item, sourcePaths.get(0));
					converted = convertFieldValue(value, field);
					
				} else {
					// 5. 여러 원본 필드 값을 조회하여 결합한다.
					List<JsonNode> values = new ArrayList<>();
					
					for (String sourcePath : sourcePaths) {
						values.add(getSourceValue(item, sourcePath));
					}
					
					converted = combineFieldValues(values, field);
				}
				
				// 6. 출력 필드명으로 가공 결과를 저장한다.
				result.set(field.getTargetName().trim(), converted);
				
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(index + "번째 데이터: " + e.getMessage(), e);
			}
			
		}
		
		return result;
	}

	/**
	 * 추출한 JSON 값을 설정된 데이터 타입으로 변환한다.
	 *
	 * @param value 추출한 원본 값
	 * @param field 필드 변환 설정
	 * @return 변환된 JSON 값
	 */
	private JsonNode convertFieldValue(JsonNode value, TransformRuleFieldDto field) {
		
		String targetName = field.getTargetName();
		
		// 1. 필수값 존재 여부를 확인한다.
		if (value == null || value.isNull() || value.isMissingNode()
				|| (value.isTextual() && value.textValue().isEmpty())) {
			
			if ("Y".equals(field.getRequiredYn())) {
				throw new IllegalArgumentException("필수 필드 값이 없습니다: " + targetName);
			}
			
			return NullNode.getInstance();
		}
		
		// 2. 데이터 타입에 따라 값을 변환한다.
		String dataType = field.getDataType();
		
		if (dataType == null) {
			throw new IllegalArgumentException("데이터 타입이 없습니다: " + targetName);
		}
		
		try {
			switch (dataType) {
				case "STRING":
					return objectMapper.getNodeFactory().textNode(
							value.isContainerNode() ? value.toString() : value.asText());
					
				case "INTEGER":
					if (!value.isNumber() && !value.isTextual()) {
						throw new IllegalArgumentException("정수 변환 대상이 아닙니다.");
					}
					
					BigInteger integer = new BigDecimal(value.asText()).toBigIntegerExact();
					return objectMapper.getNodeFactory().numberNode(integer);
					
				case "DECIMAL":
					if (!value.isNumber() && !value.isTextual()) {
						throw new IllegalArgumentException("실수 변환 대상이 아닙니다.");
					}
					
					BigDecimal decimal = new BigDecimal(value.asText());
					return objectMapper.getNodeFactory().numberNode(decimal);
					
				case "BOOLEAN":
					if (value.isBoolean()) {
						return objectMapper.getNodeFactory().booleanNode(value.booleanValue());
					}
					
					String booleanValue = value.asText();
					
					if ("true".equals(booleanValue) || "1".equals(booleanValue)) {
						return objectMapper.getNodeFactory().booleanNode(true);
					}
					
					if ("false".equals(booleanValue) || "0".equals(booleanValue)) {
						return objectMapper.getNodeFactory().booleanNode(false);
					}
					
					throw new IllegalArgumentException("BOOLEAN 변환에 실패했습니다.");
					
				case "DATETIME":
					String format = field.getValueFormat();
					
					if ("EPOCH_MILLIS".equals(format)) {
						if (!value.isNumber() && !value.isTextual()) {
							throw new IllegalArgumentException("날짜 변환 대상이 아닙니다.");
						}
						
						long millis = new BigDecimal(value.asText()).longValueExact();
						return objectMapper.getNodeFactory().textNode(Instant.ofEpochMilli(millis).toString());
					}
					
					if ("ISO_DATETIME".equals(format)) {
						if (!value.isTextual()) {
							throw new IllegalArgumentException("날짜 변환 대상이 아닙니다.");
						}
						
						// 1. 시간대 정보가 포함된 ISO 날짜를 파싱한다.
						OffsetDateTime dateTime = OffsetDateTime.parse(value.asText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
						
						// 2. UTC 기준 시각으로 변환하여 반환한다.
						return objectMapper.getNodeFactory().textNode(dateTime.toInstant().toString());
					}
					
					throw new IllegalArgumentException("날짜 입력 형식이 올바르지 않습니다.");
					
				default:
					throw new IllegalArgumentException("지원하지 않는 데이터 타입입니다: " + dataType);
			}
			
		} catch (NumberFormatException | ArithmeticException | DateTimeParseException e) {
			throw new IllegalArgumentException("데이터 타입 변환에 실패했습니다: " + targetName, e);
		}
		
	}
	
	/**
	 * XML 원본 데이터에 가공 규칙을 적용한다.
	 *
	 * @param rawData 원본 XML 문자열
	 * @param version 가공 규칙 버전
	 * @param fields 추출 필드 목록
	 * @return 가공된 JSON 데이터
	 */
	public JsonNode transformXml(String rawData, TransformRuleVersionDto version, List<TransformRuleFieldDto> fields) {
		
		// 1. 원본 데이터와 가공 규칙을 검증한다.
		if (rawData == null || rawData.isBlank()) {
			throw new IllegalArgumentException("가공할 XML 데이터가 없습니다.");
		}
		
		if (version == null || !"XML".equals(version.getDataFormat())) {
			throw new IllegalArgumentException("XML 가공 규칙이 올바르지 않습니다.");
		}
		
		if (fields == null || fields.isEmpty()) {
			throw new IllegalArgumentException("가공할 추출 필드가 없습니다.");
		}
		
		try {
			// 2. 외부 엔터티와 DTD 처리를 차단한 XML 파서를 생성한다.
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setXIncludeAware(false);
			factory.setExpandEntityReferences(false);
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			
			Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(rawData)));
			Element root = document.getDocumentElement();
			
			// 3. 반복 경로에 해당하는 XML 요소를 조회한다.
			List<Element> targets;
			String repeatPath = version.getRepeatPath();
			
			if (repeatPath == null || repeatPath.isBlank()) {
				targets = List.of(root);
			} else {
				targets = findXmlElements(root, repeatPath.trim());
			}
			
			if (targets.isEmpty()) {
				throw new IllegalArgumentException("반복 경로에 해당하는 XML 데이터가 없습니다.");
			}
			
			// 4. 반복 대상별로 추출 필드를 가공한다.
			ArrayNode results = objectMapper.createArrayNode();
			
			for (int i = 0; i < targets.size(); i++) {
				results.add(transformXmlItem(targets.get(i), fields, i + 1));
			}
			
			return results;
			
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalArgumentException("XML 파싱 또는 가공에 실패했습니다: " + e.getMessage(), e);
		}
	}
	
	/**
	 * 점으로 구분된 경로에 해당하는 XML 요소를 조회한다.
	 *
	 * @param root 탐색 시작 요소
	 * @param path XML 요소 경로
	 * @return 경로에 해당하는 XML 요소 목록
	 */
	private List<Element> findXmlElements(Element root, String path) {
		
		// 1. 경로의 첫 번째 요소를 확인한다.
		String[] names = path.split("\\.", -1);
		
		if (names.length == 0 || !root.getTagName().equals(names[0])) {
			return List.of();
		}
		
		List<Element> current = new ArrayList<>();
		current.add(root);
		
		// 2. 하위 요소를 순차적으로 탐색한다.
		for (int i = 1; i < names.length; i++) {
			if (names[i].isBlank()) {
				throw new IllegalArgumentException("XML 경로가 올바르지 않습니다: " + path);
			}
			
			List<Element> next = new ArrayList<>();
			
			for (Element parent : current) {
				NodeList children = parent.getChildNodes();
				
				for (int j = 0; j < children.getLength(); j++) {
					Node child = children.item(j);
					
					if (child instanceof Element element && names[i].equals(element.getTagName())) {
						next.add(element);
					}
				}
			}
			
			current = next;
			
			if (current.isEmpty()) {
				break;
			}
		}
		
		return current;
	}
	
	/**
	 * 지정한 경로에 해당하는 XML 요소의 값을 조회한다.
	 *
	 * @param item 가공 대상 XML 요소
	 * @param sourcePath 원본 필드 경로
	 * @return 추출한 JSON 값, 없으면 NullNode
	 */
	private JsonNode getXmlSourceValue(Element item, String sourcePath) {
		
		// 1. 원본 경로를 검증한다.
		if (sourcePath == null || sourcePath.isBlank()) {
			throw new IllegalArgumentException("XML 원본 경로가 올바르지 않습니다.");
		}
		
		// 2. 원본 경로에 해당하는 XML 요소를 조회한다.
		List<Element> elements = findXmlElements(item, item.getTagName() + "." + sourcePath.trim());
		
		if (elements.isEmpty()) {
			return NullNode.getInstance();
		}
		
		// 3. 단일 값을 추출해야 하므로 중복 요소를 검증한다.
		if (elements.size() > 1) {
			throw new IllegalArgumentException("추출 경로에 여러 XML 요소가 존재합니다: " + sourcePath);
		}
		
		// 4. XML 요소의 문자열 값을 반환한다.
		String text = elements.get(0).getTextContent().trim();
		
		if (text.isEmpty()) {
			return NullNode.getInstance();
		}
		
		return objectMapper.getNodeFactory().textNode(text);
	}
	
	/**
	 * XML 요소 한 건의 추출 필드를 가공한다.
	 *
	 * @param item 가공 대상 XML 요소
	 * @param fields 추출 필드 목록
	 * @param index 가공 대상 순번
	 * @return 가공된 JSON 객체
	 */
	private ObjectNode transformXmlItem(Element item, List<TransformRuleFieldDto> fields, int index) {
		
		ObjectNode result = objectMapper.createObjectNode();
		
		// 1. 추출 필드별로 XML 요소를 조회하고 가공한다.
		for (TransformRuleFieldDto field : fields) {
			if (field == null || field.getSourcePaths() == null || field.getSourcePaths().isEmpty()
					|| field.getTargetName() == null || field.getTargetName().isBlank()) {
				throw new IllegalArgumentException("가공 규칙 추출 필드 정보가 올바르지 않습니다.");
			}
			
			try {
				// 2. 가공 유형과 원본 경로 목록을 확인한다.
				List<String> sourcePaths = field.getSourcePaths();
				String transformType = field.getTransformType() == null ? "DIRECT" : field.getTransformType();
				
				JsonNode converted;
				
				if ("DIRECT".equals(transformType)) {
					// 3. 단일 XML 요소 값을 조회하여 기존 타입 변환을 수행한다.
					if (sourcePaths.size() != 1) {
						throw new IllegalArgumentException("DIRECT 원본 경로는 1개여야 합니다.");
					}
					
					JsonNode value = getXmlSourceValue(item, sourcePaths.get(0));
					converted = convertFieldValue(value, field);
					
				} else {
					// 4. 여러 XML 요소 값을 조회하여 결합한다.
					List<JsonNode> values = new ArrayList<>();
					
					for (String sourcePath : sourcePaths) {
						values.add(getXmlSourceValue(item, sourcePath));
					}
					
					converted = combineFieldValues(values, field);
				}
				
				// 5. 출력 필드명으로 가공 결과를 저장한다.
				result.set(field.getTargetName().trim(), converted);
				
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(index + "번째 데이터: " + e.getMessage(), e);
			}
		}
		
		return result;
	}
	
	/**
	 * 여러 원본 필드를 설정된 가공 유형에 따라 하나의 값으로 결합한다.
	 *
	 * @param values 원본 경로 순서대로 조회한 값 목록
	 * @param field 가공 규칙 필드 설정
	 * @return 결합된 JSON 값
	 */
	private JsonNode combineFieldValues(List<JsonNode> values, TransformRuleFieldDto field) {
		
		// 1. 필수 원본값의 누락 여부를 확인한다.
		for (JsonNode value : values) {
			if (value == null || value.isNull() || value.isMissingNode() || (value.isTextual() && value.asText().isBlank())) {
				
				if ("Y".equals(field.getRequiredYn())) {
					throw new IllegalArgumentException("결합할 필수 필드 값이 없습니다: " + field.getTargetName());
				}
				
				return NullNode.getInstance();
			}
			
			if (!value.isValueNode() || value.isBoolean()) {
				throw new IllegalArgumentException("결합할 수 없는 원본 값입니다: " + field.getTargetName());
			}
		}
		
		// 2. 가공 유형에 따라 결합 값을 생성한다.
		switch (field.getTransformType()) {
			case "CONCAT":
				
				if (values.size() < 2 || field.getSeparator() == null) {
					throw new IllegalArgumentException("문자열 결합 설정이 올바르지 않습니다.");
				}
				
				List<String> strings = new ArrayList<>();
				
				for (JsonNode value : values) {
					strings.add(value.asText());
				}
				
				return objectMapper.getNodeFactory().textNode(String.join(field.getSeparator(), strings));
				
			case "DATE_COMBINE":
				
				if (values.size() != 3) {
					throw new IllegalArgumentException("날짜 결합에는 연, 월, 일 값 3개가 필요합니다.");
				}
				
				try {
					// 3. 연, 월, 일 값을 실제 날짜로 검증한다.
					int year = Integer.parseInt(values.get(0).asText().trim());
					int month = Integer.parseInt(values.get(1).asText().trim());
					int day = Integer.parseInt(values.get(2).asText().trim());
					
					LocalDate date = LocalDate.of(year, month, day);
					
					// 4. 검증된 날짜를 ISO 날짜 문자열로 반환한다.
					return objectMapper.getNodeFactory().textNode(date.toString());
					
				} catch (NumberFormatException | DateTimeException e) {
					throw new IllegalArgumentException("날짜 결합에 실패했습니다: " + field.getTargetName(), e);
				}
				
			default:
				throw new IllegalArgumentException("지원하지 않는 결합 유형입니다: " + field.getTransformType());
		}
		
	}
	
}
