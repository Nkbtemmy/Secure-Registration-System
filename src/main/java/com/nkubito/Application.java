package com.nkubito;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Secure Registration System - Main Application Entry Point
 * 
 * This application implements a user registration system with:
 * - Admin user creation with registration code generation
 * - User registration with code validation
 * - Dilithium post-quantum digital signatures on all responses
 * - Protocol Buffer serialization for all API communication
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
