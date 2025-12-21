package com.nkubito.controller;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.nkubito.crypto.DilithiumKeyManager;
import com.nkubito.crypto.SigningService;
import com.nkubito.exception.BusinessException;
import com.nkubito.proto.Messages;
import com.nkubito.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "User Management", description = "APIs for user creation and registration")
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
    @Operation(
            summary = "Create a new user (Admin)",
            description = """
                    Creates a new user account with the provided name and email.
                    Returns a registration code that should be shared with the user.
                    
                    **Request Body (Protocol Buffer - CreateUserRequest):**
                    - `name` (string): User's full name
                    - `email` (string): User's email address
                    
                    **Response Body (Protocol Buffer - SignedResponse):**
                    - `body`: Serialized CreateUserResponse containing name, email, and registration_code
                    - `signature`: Dilithium signature of the body
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully",
                    content = @Content(mediaType = "application/octet-stream",
                            schema = @Schema(description = "SignedResponse protobuf containing CreateUserResponse"))),
            @ApiResponse(responseCode = "400", description = "Invalid request (missing name or email)"),
            @ApiResponse(responseCode = "409", description = "User with this email already exists")
    })
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
    @Operation(
            summary = "Complete user registration",
            description = """
                    Completes user registration using email and registration code.
                    The registration code integrity (checksum) is verified BEFORE database access.
                    
                    **Request Body (Protocol Buffer - RegisterUserRequest):**
                    - `email` (string): User's email address
                    - `registration_code` (string): 20-character hex registration code
                    
                    **Response Body (Protocol Buffer - SignedResponse):**
                    - `body`: Serialized RegisterUserResponse containing auth_token, name, and email
                    - `signature`: Dilithium signature of the body
                    
                    **Registration Code Format:**
                    - First 16 characters: Actual registration code
                    - Last 4 characters: MD5 checksum (last 2 bytes, hex encoded)
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registration successful",
                    content = @Content(mediaType = "application/octet-stream",
                            schema = @Schema(description = "SignedResponse protobuf containing RegisterUserResponse"))),
            @ApiResponse(responseCode = "400", description = "Invalid registration code format or checksum"),
            @ApiResponse(responseCode = "404", description = "User not found with the provided email"),
            @ApiResponse(responseCode = "409", description = "User already registered")
    })
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
    @Operation(
            summary = "Get server's Dilithium public key",
            description = """
                    Returns the server's Dilithium digital signature public key.
                    This can be used by clients to verify the signatures on all API responses.
                    
                    **No authentication required.**
                    **No request body.**
                    
                    **Response Body (Protocol Buffer - SignedResponse):**
                    - `body`: Serialized PublicKeyResponse containing public_key (bytes) and algorithm (string)
                    - `signature`: Dilithium signature of the body
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Public key retrieved successfully",
                    content = @Content(mediaType = "application/octet-stream",
                            schema = @Schema(description = "SignedResponse protobuf containing PublicKeyResponse")))
    })
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
