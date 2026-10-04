package com.kji.scheduler.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * AES-GCM 방식으로 문자열 암호화 및 복호화를 처리한다.
 * 외부 API 인증정보와 같이 복호화가 필요한 민감정보 저장에 사용한다.
 *
 * @author kji
 * @since 2026. 10. 4.
 */
@Service
public class AesGcmEncryptionService {
	
	private static final String ALGORITHM = "AES/GCM/NoPadding";
	private static final int IV_LENGTH = 12;
	private static final int TAG_LENGTH = 128;
	private static final String ENCRYPTED_PREFIX = "ENC:v1:";
	
	private final SecretKeySpec secretKey;
	private final SecureRandom secureRandom;
	
	public AesGcmEncryptionService(@Value("${app.security.encryption-key}") String encryptionKey) {
		
		// 1. Base64 형식의 암호화 키를 디코딩한다.
		byte[] keyBytes;
		
		try {
			keyBytes = Base64.getDecoder().decode(encryptionKey);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException("암호화 키 형식이 올바르지 않습니다.", e);
		}
		
		// 2. AES-256 키 길이를 확인한다.
		if (keyBytes.length != 32) {
			throw new IllegalStateException("AES-256 암호화 키는 32바이트여야 합니다.");
		}
		
		// 3. AES 키와 난수 생성기를 초기화한다.
		this.secretKey = new SecretKeySpec(keyBytes, "AES");
		this.secureRandom = new SecureRandom();
	}
	
	/**
	 * 문자열을 AES-GCM 방식으로 암호화한다.
	 *
	 * @param 	value 	암호화할 평문
	 * @return			접두사가 포함된 암호문
	 */
	public String encrypt(String value) {
		
		// 1. 값이 없으면 그대로 반환한다.
		if (value == null || value.isBlank()) {
			return value;
		}
		
		// 2. 이미 암호화된 값이면 다시 암호화하지 않는다.
		if (value.startsWith(ENCRYPTED_PREFIX)) {
			return value;
		}
		
		try {
			
			// 3. 암호화마다 새로운 IV를 생성한다.
			byte[] iv = new byte[IV_LENGTH];
			secureRandom.nextBytes(iv);
			
			// 4. AES-GCM 암호화 객체를 초기화한다.
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH, iv);
			
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
			
			// 5. 평문을 암호화한다.
			byte[] encryptedBytes = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
			
			// 6. IV와 암호문을 하나의 데이터로 합친다.
			byte[] result = ByteBuffer
					.allocate(iv.length + encryptedBytes.length)
					.put(iv)
					.put(encryptedBytes)
					.array();
			
			// 7. 암호화 버전과 Base64 문자열을 함께 반환한다.
			return ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(result);
			
		} catch (Exception e) {
			throw new IllegalStateException("인증 정보 암호화에 실패했습니다.", e);
		}
	}
	
	/**
	 * AES-GCM 방식으로 암호화된 문자열을 복호화한다.
	 *
	 * @param encryptedValue	복호화할 암호문
	 * @return					복호화된 평문
	 */
	public String decrypt(String encryptedValue) {
		
		// 1. 값이 없으면 그대로 반환한다.
		if (encryptedValue == null || encryptedValue.isBlank()) {
			return encryptedValue;
		}
		
		// 2. 암호화된 값이 아니면 기존 평문 데이터로 보고 그대로 반환한다.
		if (!encryptedValue.startsWith(ENCRYPTED_PREFIX)) {
			return encryptedValue;
		}
		
		try {
			
			// 3. 암호화 버전 정보를 제외한다.
			String encodedValue = encryptedValue.substring(ENCRYPTED_PREFIX.length());
			
			// 4. Base64 문자열을 원본 바이트 배열로 변환한다.
			byte[] decodedBytes = Base64.getDecoder().decode(encodedValue);
			
			if (decodedBytes.length <= IV_LENGTH) {
				throw new IllegalStateException("암호화된 인증 정보 형식이 올바르지 않습니다.");
			}
			
			// 5. IV와 암호문을 분리한다.
			byte[] iv = new byte[IV_LENGTH];
			byte[] encryptedBytes = new byte[decodedBytes.length - IV_LENGTH];
			
			System.arraycopy(decodedBytes, 0, iv, 0, IV_LENGTH);
			System.arraycopy(decodedBytes, IV_LENGTH, encryptedBytes, 0, encryptedBytes.length);
			
			// 6. AES-GCM 복호화 객체를 초기화한다.
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH, iv);
			
			cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
			
			// 7. 암호문을 복호화한다.
			byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
			
			return new String(decryptedBytes, StandardCharsets.UTF_8);
			
		} catch (IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("인증 정보 복호화에 실패했습니다.", e);
		}
	}
	
}