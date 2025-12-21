package com.nkubito.controller;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.nkubito.crypto.DilithiumKeyManager;
import com.nkubito.crypto.SigningService;
import com.nkubito.exception.BusinessException;
import com.nkubito.proto.Messages;
import com.nkubito.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for user management APIs.
 * All responses are Protocol Buffers signed with Dilithium.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private static final MediaType PROTOBUF_MEDIA_TYPE = MediaType.parseMediaType("application/octet-stream");

    private final UserService userService;
    private final SigningService signingService;
    private final DilithiumKeyManager keyManager;

    /**
     * Create a new user (Admin action).
     * POST /api/users
     * 
     * Request: CreateUserRequest protobuf
     * Response: Signed CreateUserResponse protobuf
     */
    @PostMapping(
            value = "/users",
            consumes = "application/octet-stream",
            produces = "application/octet-stream"
    )
    public ResponseEntity<byte[]> createUser(@RequestBody byte[] requestBody) {
        Messages.CreateUserRequest request = parseCreateUserRequest(requestBody);
        log.info("Received create user request for email: {}", request.getEmail());

        Messages.CreateUserResponse response = userService.createUser(
                request.getName(),
                request.getEmail()
        );

        Messages.SignedResponse signedResponse = signingService.signResponse(response);

        return ResponseEntity
                .status(201)
                .contentType(PROTOBUF_MEDIA_TYPE)
                .body(signedResponse.toByteArray());
    }

    /**
     * Register a user with email and registration code.
     * POST /api/register
     * 
     * Request: RegisterUserRequest protobuf
     * Response: Signed RegisterUserResponse protobuf
     */
    @PostMapping(
            value = "/register",
            consumes = "application/octet-stream",
            produces = "application/octet-stream"
    )
    public ResponseEntity<byte[]> registerUser(@RequestBody byte[] requestBody) {
        Messages.RegisterUserRequest request = parseRegisterUserRequest(requestBody);
        log.info("Received registration request for email: {}", request.getEmail());

        Messages.RegisterUserResponse response = userService.registerUser(
                request.getEmail(),
                request.getRegistrationCode()
        );

        Messages.SignedResponse signedResponse = signingService.signResponse(response);

        return ResponseEntity
                .ok()
                .contentType(PROTOBUF_MEDIA_TYPE)
                .body(signedResponse.toByteArray());
    }

    /**
     * Get the server's Dilithium public key.
     * GET /api/public-key
     * 
     * No authentication required.
     * No request body.
     * Response: Signed PublicKeyResponse protobuf
     */
    @GetMapping(
            value = "/public-key",
            produces = "application/octet-stream"
    )
    public ResponseEntity<byte[]> getPublicKey() {
        log.info("Public key requested");

        Messages.PublicKeyResponse response = Messages.PublicKeyResponse.newBuilder()
                .setPublicKey(ByteString.copyFrom(keyManager.getPublicKeyBytes()))
                .setAlgorithm("Dilithium3")
                .build();

        Messages.SignedResponse signedResponse = signingService.signResponse(response);

        return ResponseEntity
                .ok()
                .contentType(PROTOBUF_MEDIA_TYPE)
                .body(signedResponse.toByteArray());
    }

    private Messages.CreateUserRequest parseCreateUserRequest(byte[] requestBody) {
        try {
            return Messages.CreateUserRequest.parseFrom(requestBody);
        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to parse CreateUserRequest", e);
            throw new BusinessException("Invalid request format: " + e.getMessage());
        }
    }

    private Messages.RegisterUserRequest parseRegisterUserRequest(byte[] requestBody) {
        try {
            return Messages.RegisterUserRequest.parseFrom(requestBody);
        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to parse RegisterUserRequest", e);
            throw new BusinessException("Invalid request format: " + e.getMessage());
        }
    }
}
