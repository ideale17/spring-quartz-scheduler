package com.kji.scheduler.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.util.Base64;

import org.junit.jupiter.api.Test;

class AesGcmEncryptionServiceTest {
	
	@Test
	void encryptAndDecrypt() {
		
		// 1. 테스트용 AES-256 키를 생성한다.
		byte[] keyBytes = new byte[32];
		new SecureRandom().nextBytes(keyBytes);
		
		String encryptionKey = Base64.getEncoder().encodeToString(keyBytes);
		
		// 2. 암호화 서비스를 생성한다.
		AesGcmEncryptionService encryptionService = new AesGcmEncryptionService(encryptionKey);
		
		// 3. 테스트 평문을 암호화한다.
		String plainValue = "test-api-key-1234";
		String encryptedValue = encryptionService.encrypt(plainValue);
		
		// 4. 암호화 결과를 확인한다.
		assertTrue(encryptedValue.startsWith("ENC:v1:"));
		assertNotEquals(plainValue, encryptedValue);
		
		// 5. 암호문을 복호화한다.
		String decryptedValue = encryptionService.decrypt(encryptedValue);
		
		// 6. 원래 평문과 동일한지 확인한다.
		assertEquals(plainValue, decryptedValue);
	}
}