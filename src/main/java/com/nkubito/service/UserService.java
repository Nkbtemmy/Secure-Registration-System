package com.nkubito.service;

import com.nkubito.proto.Messages;

/**
 * Service interface for user management operations.
 */
public interface UserService {

    /**
     * Create a new user with the given name and email.
     * Generates a registration code and returns user details.
     * 
     * @param name User's name
     * @param email User's email address
     * @return CreateUserResponse containing user info and registration code
     */
    Messages.CreateUserResponse createUser(String name, String email);

    /**
     * Register a user using their email and registration code.
     * Validates integrity first (checksum), then validates against stored hash.
     * 
     * @param email User's email address
     * @param registrationCode The 20-character registration code
     * @return RegisterUserResponse containing auth token and user info
     */
    Messages.RegisterUserResponse registerUser(String email, String registrationCode);
}
