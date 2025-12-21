package com.nkubito.service.impl;

import com.nkubito.crypto.PasswordHasher;
import com.nkubito.crypto.RegistrationCodeGenerator;
import com.nkubito.domain.entity.User;
import com.nkubito.exception.BusinessException;
import com.nkubito.exception.InvalidRegistrationCodeException;
import com.nkubito.exception.ResourceNotFoundException;
import com.nkubito.exception.UserAlreadyExistsException;
import com.nkubito.exception.UserAlreadyRegisteredException;
import com.nkubito.proto.Messages;
import com.nkubito.repository.UserRepository;
import com.nkubito.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementation of UserService handling user creation and registration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RegistrationCodeGenerator codeGenerator;
    private final PasswordHasher passwordHasher;

    @Override
    @Transactional
    public Messages.CreateUserResponse createUser(String name, String email) {
        log.info("Creating user with email: {}", email);

        // Validate inputs
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException("Name is required");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new BusinessException("Email is required");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(email.toLowerCase())) {
            throw new UserAlreadyExistsException("User with email " + email + " already exists");
        }

        // Generate registration code
        String registrationCode = codeGenerator.generateCode();
        String codeBase = codeGenerator.extractCodeBase(registrationCode);

        // Generate salt and hash the code base
        String salt = passwordHasher.generateSalt();
        String codeHash = passwordHasher.hash(codeBase, salt);

        // Create and save user
        User user = User.builder()
                .name(name.trim())
                .email(email.toLowerCase().trim())
                .registrationCodeHash(codeHash)
                .registrationCodeSalt(salt)
                .registered(false)
                .build();

        userRepository.save(user);
        log.info("User created successfully with id: {}", user.getId());

        // Return response with the full registration code (to be shared with user)
        return Messages.CreateUserResponse.newBuilder()
                .setName(user.getName())
                .setEmail(user.getEmail())
                .setRegistrationCode(registrationCode)
                .build();
    }

    @Override
    @Transactional
    public Messages.RegisterUserResponse registerUser(String email, String registrationCode) {
        log.info("Attempting to register user with email: {}", email);

        // Validate inputs
        if (email == null || email.trim().isEmpty()) {
            throw new BusinessException("Email is required");
        }
        if (registrationCode == null || registrationCode.trim().isEmpty()) {
            throw new BusinessException("Registration code is required");
        }

        // STEP 1: Verify registration code integrity BEFORE database access
        // This validates the checksum embedded in the code
        if (!codeGenerator.verifyIntegrity(registrationCode)) {
            log.warn("Registration code integrity check failed for email: {}", email);
            throw new InvalidRegistrationCodeException("Invalid registration code format or checksum");
        }
        log.debug("Registration code integrity verified for email: {}", email);

        // STEP 2: Now query the database
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        // Check if already registered
        if (user.isRegistered()) {
            throw new UserAlreadyRegisteredException("User is already registered");
        }

        // STEP 3: Validate registration code against stored hash
        String codeBase = codeGenerator.extractCodeBase(registrationCode);
        if (!passwordHasher.verify(codeBase, user.getRegistrationCodeHash(), user.getRegistrationCodeSalt())) {
            log.warn("Registration code validation failed for email: {}", email);
            throw new InvalidRegistrationCodeException("Invalid registration code");
        }
        log.debug("Registration code validated successfully for email: {}", email);

        // STEP 4: Mark user as registered and generate auth token
        String authToken = passwordHasher.generateAuthToken();
        user.setRegistered(true);
        user.setAuthToken(authToken);  // Stored in plain text as per requirements
        user.setRegisteredAt(LocalDateTime.now());

        userRepository.save(user);
        log.info("User registered successfully: {}", email);

        // Return response with auth token and user info
        return Messages.RegisterUserResponse.newBuilder()
                .setAuthToken(authToken)
                .setName(user.getName())
                .setEmail(user.getEmail())
                .build();
    }
}
