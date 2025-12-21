package com.nkubito.crypto;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.bouncycastle.pqc.jcajce.spec.DilithiumParameterSpec;
import org.springframework.stereotype.Component;

import java.security.*;

/**
 * Manages Dilithium post-quantum digital signature keypair.
 * The keypair is generated once at application startup and reused for all signatures.
 */
@Component
@Slf4j
public class DilithiumKeyManager {

    private KeyPair keyPair;

    @PostConstruct
    public void init() {
        try {
            // Register BouncyCastle providers
            Security.addProvider(new BouncyCastleProvider());
            Security.addProvider(new BouncyCastlePQCProvider());

            // Generate Dilithium keypair
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("Dilithium", "BCPQC");
            keyPairGenerator.initialize(DilithiumParameterSpec.dilithium3);
            this.keyPair = keyPairGenerator.generateKeyPair();

            log.info("Dilithium keypair generated successfully");
            log.debug("Public key algorithm: {}", keyPair.getPublic().getAlgorithm());
        } catch (Exception e) {
            log.error("Failed to generate Dilithium keypair", e);
            throw new RuntimeException("Failed to initialize Dilithium cryptography", e);
        }
    }

    /**
     * Get the public key for distribution to clients.
     */
    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    /**
     * Get the encoded public key bytes.
     */
    public byte[] getPublicKeyBytes() {
        return keyPair.getPublic().getEncoded();
    }

    /**
     * Get the private key for signing.
     */
    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }

    /**
     * Sign data using Dilithium private key.
     */
    public byte[] sign(byte[] data) {
        try {
            Signature signature = Signature.getInstance("Dilithium", "BCPQC");
            signature.initSign(keyPair.getPrivate());
            signature.update(data);
            return signature.sign();
        } catch (Exception e) {
            log.error("Failed to sign data", e);
            throw new RuntimeException("Signing failed", e);
        }
    }

    /**
     * Verify a signature using the public key.
     */
    public boolean verify(byte[] data, byte[] signatureBytes) {
        try {
            Signature signature = Signature.getInstance("Dilithium", "BCPQC");
            signature.initVerify(keyPair.getPublic());
            signature.update(data);
            return signature.verify(signatureBytes);
        } catch (Exception e) {
            log.error("Failed to verify signature", e);
            return false;
        }
    }
}
