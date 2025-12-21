package com.nkubito.crypto;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
import com.nkubito.proto.Messages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Service for signing Protocol Buffer responses with Dilithium signature.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SigningService {

    private final DilithiumKeyManager keyManager;

    /**
     * Wrap a Protocol Buffer message in a SignedResponse with Dilithium signature.
     * 
     * @param message The Protocol Buffer message to sign
     * @return SignedResponse containing the serialized message and its signature
     */
    public Messages.SignedResponse signResponse(MessageLite message) {
        // Serialize the message body
        byte[] bodyBytes = message.toByteArray();
        
        // Sign the serialized body
        byte[] signature = keyManager.sign(bodyBytes);
        
        log.debug("Signed response of {} bytes with signature of {} bytes", 
                  bodyBytes.length, signature.length);
        
        // Build and return the signed response
        return Messages.SignedResponse.newBuilder()
                .setBody(ByteString.copyFrom(bodyBytes))
                .setSignature(ByteString.copyFrom(signature))
                .build();
    }

    /**
     * Sign raw bytes and create a SignedResponse.
     * Used when the body is already serialized.
     */
    public Messages.SignedResponse signBytes(byte[] bodyBytes) {
        byte[] signature = keyManager.sign(bodyBytes);
        
        return Messages.SignedResponse.newBuilder()
                .setBody(ByteString.copyFrom(bodyBytes))
                .setSignature(ByteString.copyFrom(signature))
                .build();
    }
}
