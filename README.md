# Secure Registration System

A Java Spring Boot application implementing a user registration system with post-quantum cryptographic signatures.

## Features

1. **User Creation by Admin**: Admin creates users with name/email and receives a registration code with embedded checksum
2. **User Registration**: Users complete registration using their email and registration code
3. **Dilithium Digital Signatures**: All API responses are signed using Dilithium3 (post-quantum cryptography)
4. **Protocol Buffers**: All API communication uses Protocol Buffer serialization

## Technical Requirements

- Java 17+
- Maven 3.8+

## Project Structure

```
src/main/java/com/nkubito/
├── Application.java              # Main entry point
├── controller/
│   └── UserController.java       # REST API endpoints
├── service/
│   ├── UserService.java          # Service interface
│   └── impl/
│       └── UserServiceImpl.java  # Service implementation
├── repository/
│   └── UserRepository.java       # JPA repository
├── domain/entity/
│   └── User.java                 # User entity
├── crypto/
│   ├── DilithiumKeyManager.java      # Dilithium keypair management
│   ├── SigningService.java           # Response signing
│   ├── RegistrationCodeGenerator.java # Code generation with checksum
│   └── PasswordHasher.java           # PBKDF2 hashing for codes
└── exception/
    ├── GlobalExceptionHandler.java
    └── ...                            # Custom exceptions
```

## Setup and Run

### 1. Clone and navigate to project

```bash
cd user-managment
```

### 2. Build the project (compiles Protocol Buffers)

```bash
mvn clean compile
```

### 3. Run the application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### 4. Access the web interface

Open `http://localhost:8080` in your browser to access the minimal test UI.

## API Endpoints

All API endpoints use Protocol Buffer serialization (`application/octet-stream`).

### 1. Create User (Admin)

```
POST /api/users
Content-Type: application/octet-stream

Request: CreateUserRequest protobuf
Response: SignedResponse containing CreateUserResponse
```

### 2. Register User

```
POST /api/register
Content-Type: application/octet-stream

Request: RegisterUserRequest protobuf
Response: SignedResponse containing RegisterUserResponse
```

### 3. Get Server Public Key

```
GET /api/public-key

Response: SignedResponse containing PublicKeyResponse
```

## Registration Code Format

The registration code is a 20-character hexadecimal string:
- **First 16 characters**: Random registration code
- **Last 4 characters**: Last 2 bytes of MD5 hash of first 16 characters (checksum)

This allows clients to validate code integrity without contacting the server.

## Security Features

1. **Registration Code Storage**: Codes are stored as salted hashes using PBKDF2
2. **Integrity Verification**: Checksum is verified BEFORE any database access
3. **Digital Signatures**: Every response is signed using Dilithium private key
4. **Auth Tokens**: Stored in plain text (as per requirements)

## Signed Response Structure

Every API response follows this Protocol Buffer structure:

```protobuf
message SignedResponse {
    bytes body = 1;      // Serialized Protocol Buffer of actual response
    bytes signature = 2; // Dilithium signature of the body
}
```

## Database

Uses H2 in-memory database. Access the console at:
- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:registrationdb`
- Username: `sa`
- Password: (empty)

## Design Decisions

1. **Dilithium3**: Selected as a balanced option between security and performance
2. **PBKDF2**: Industry-standard password hashing with 65536 iterations
3. **MD5 for Checksum**: Used only for checksum (not security-critical), as specified
4. **H2 Database**: Lightweight in-memory database suitable for this demo

## Assumptions

1. Admin authentication is kept optional (publicly accessible) as per requirements
2. The registration code checksum uses lowercase hex for consistency
3. Email addresses are normalized to lowercase
4. Auth tokens are URL-safe Base64 encoded
# Secure-Registration-System
