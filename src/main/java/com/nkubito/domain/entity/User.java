package com.nkubito.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User entity representing a user in the registration system.
 * The registration code is stored as a salted hash for security.
 * The auth token is stored in plain text as per requirements.
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Salted hash of the registration code (first 16 characters only).
     * We hash only the actual code part, not the checksum.
     */
    @Column(name = "registration_code_hash", nullable = false)
    private String registrationCodeHash;

    /**
     * Salt used for hashing the registration code.
     */
    @Column(name = "registration_code_salt", nullable = false)
    private String registrationCodeSalt;

    /**
     * Whether the user has completed registration.
     */
    @Column(nullable = false)
    private boolean registered;

    /**
     * Authentication token stored in plain text (as per requirements).
     * Only populated after successful registration.
     */
    @Column(name = "auth_token")
    private String authToken;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "registered_at")
    private LocalDateTime registeredAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        registered = false;
    }
}
