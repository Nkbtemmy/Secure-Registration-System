package com.nkubito.crypto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Utility for generating and validating registration codes.
 * 
 * Registration code format (20 hex characters):
 * - First 16 characters: actual registration code
 * - Last 4 characters: last 2 bytes of MD5 hash of the first 16 chars (hex encoded)
 */
@Component
@Slf4j
public class RegistrationCodeGenerator {

    private static final int CODE_LENGTH = 16;
    private static final int CHECKSUM_LENGTH = 4;
    private static final int TOTAL_LENGTH = CODE_LENGTH + CHECKSUM_LENGTH;
    
    private final SecureRandom secureRandom = new SecureRandom();
    private final HexFormat hexFormat = HexFormat.of().withLowerCase();

    /**
     * Generate a new registration code with embedded checksum.
     * 
     * @return 20-character hexadecimal registration code
     */
    public String generateCode() {
        // Generate 8 random bytes (16 hex characters)
        byte[] randomBytes = new byte[8];
        secureRandom.nextBytes(randomBytes);
        
        // Convert to hex string (first 16 characters)
        String codeBase = hexFormat.formatHex(randomBytes);
        
        // Generate checksum (last 4 characters)
        String checksum = calculateChecksum(codeBase);
        
        String fullCode = codeBase + checksum;
        log.debug("Generated registration code: {}", fullCode);
        
        return fullCode;
    }

    /**
     * Calculate the checksum for a code base.
     * The checksum is the last 2 bytes of the MD5 hash, hex encoded.
     */
    public String calculateChecksum(String codeBase) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(codeBase.getBytes());
            
            // Take last 2 bytes of the hash
            byte[] lastTwoBytes = new byte[2];
            lastTwoBytes[0] = hash[hash.length - 2];
            lastTwoBytes[1] = hash[hash.length - 1];
            
            return hexFormat.formatHex(lastTwoBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    /**
     * Verify the integrity of a registration code using its embedded checksum.
     * This validation happens BEFORE any database access.
     * 
     * @param fullCode The complete 20-character registration code
     * @return true if the checksum is valid
     */
    public boolean verifyIntegrity(String fullCode) {
        if (fullCode == null || fullCode.length() != TOTAL_LENGTH) {
            log.debug("Invalid code length: {}", fullCode == null ? "null" : fullCode.length());
            return false;
        }
        
        // Validate hex format
        if (!fullCode.matches("[a-fA-F0-9]{20}")) {
            log.debug("Invalid hex format: {}", fullCode);
            return false;
        }
        
        String codeBase = fullCode.substring(0, CODE_LENGTH);
        String providedChecksum = fullCode.substring(CODE_LENGTH).toLowerCase();
        String expectedChecksum = calculateChecksum(codeBase.toLowerCase());
        
        boolean valid = providedChecksum.equals(expectedChecksum);
        log.debug("Checksum validation: provided={}, expected={}, valid={}", 
                  providedChecksum, expectedChecksum, valid);
        
        return valid;
    }

    /**
     * Extract the actual code (first 16 characters) from a full registration code.
     * This is the part that gets hashed and stored.
     */
    public String extractCodeBase(String fullCode) {
        if (fullCode == null || fullCode.length() < CODE_LENGTH) {
            throw new IllegalArgumentException("Invalid registration code");
        }
        return fullCode.substring(0, CODE_LENGTH).toLowerCase();
    }
}
