package com.kji.scheduler.typehandler;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * PostgreSQL JSONB 배열과 List<String> 사이의 변환을 처리한다.
 */
public class StringListJsonbTypeHandler extends BaseTypeHandler<List<String>> {
	
	private final ObjectMapper objectMapper = new ObjectMapper();
	
	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
		
		// 1. 원본 경로 목록을 JSON 문자열로 변환한다.
		try {
			String json = objectMapper.writeValueAsString(parameter);
			
			// 2. PostgreSQL JSONB 타입으로 값을 바인딩한다.
			ps.setObject(i, json, Types.OTHER);
			
		} catch (IOException e) {
			throw new SQLException("가공 규칙 원본 경로 저장에 실패했습니다.", e);
		}
	}
	
	@Override
	public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
		return parseJson(rs.getString(columnName));
	}
	
	@Override
	public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		return parseJson(rs.getString(columnIndex));
	}
	
	@Override
	public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		return parseJson(cs.getString(columnIndex));
	}
	
	/**
	 * JSONB 배열 문자열을 원본 경로 목록으로 변환한다.
	 *
	 * @param json DB에서 조회한 JSONB 문자열
	 * @return 원본 경로 목록
	 */
	private List<String> parseJson(String json) throws SQLException {
		
		// 1. NULL 값을 확인한다.
		if (json == null) {
			return null;
		}
		
		// 2. JSON 배열을 문자열 목록으로 변환한다.
		try {
			return objectMapper.readValue(json, new TypeReference<List<String>>() {});
			
		} catch (IOException e) {
			throw new SQLException("가공 규칙 원본 경로 조회에 실패했습니다.", e);
		}
	}
	
}