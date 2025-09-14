# Spring Boot OTP Authentication with MSG91

A complete Spring Boot application implementing OTP-based authentication using MSG91's managed OTP flow with JWT token generation.

## Features

- **OTP Authentication**: Request and verify OTP using MSG91's managed OTP flow
- **JWT Tokens**: Generate access and refresh tokens with configurable TTL
- **Phone Number Normalization**: Automatic normalization of Indian phone numbers
- **Simplified Flow**: Uses MSG91's requestId directly - no custom session management needed
- **Minimal Storage**: Only stores phone mapping temporarily (easily replaceable with Redis)
- **Error Handling**: Structured JSON error responses
- **Validation**: Input validation with detailed error messages

## Prerequisites

- Java 21+
- Maven 3.6+
- PostgreSQL 12+
- MSG91 account with API credentials

## Configuration

### Environment Variables

Set the following environment variables or update `application.yml`:

```bash
export MSG91_AUTH_KEY="your-msg91-auth-key"
export MSG91_TEMPLATE_ID="your-template-id"
export JWT_SECRET="your-jwt-secret-key"
export DB_USERNAME="demo_user"
export DB_PASSWORD="demo_password"
```

### Database Setup

1. **Install PostgreSQL** (if not already installed)
2. **Create database and user**:
   ```sql
   CREATE DATABASE demo_db;
   CREATE USER demo_user WITH PASSWORD 'demo_password';
   GRANT ALL PRIVILEGES ON DATABASE demo_db TO demo_user;
   ```
3. **The application will automatically create the `users` table** on first run

### Application Configuration

The application uses the following configuration in `application.yml`:

```yaml
msg91:
  baseUrl: https://api.msg91.com
  authKey: ${MSG91_AUTH_KEY:your-msg91-auth-key-here}
  otp:
    generatePath: /api/v5/otp
    verifyPath: /api/v5/otp/verify
    templateId: ${MSG91_TEMPLATE_ID:your-template-id-here}

jwt:
  secret: ${JWT_SECRET:mySecretKey123456789012345678901234567890}
  issuer: demo-app
  accessTtlMinutes: 90
  refreshTtlDays: 10
```

## Running the Application

1. **Clone and navigate to the project directory**
2. **Set environment variables** (see Configuration section)
3. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```

The application will start on `http://localhost:8080`

## API Endpoints

### 1. Request OTP

**Endpoint**: `POST /auth/otp/request`

**Request Body**:
```json
{
  "phone": "+919876543210",
  "purpose": "LOGIN"
}
```

**Response**:
```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "expiresInSec": 300,
  "resendInSec": 30
}
```

### 2. Verify OTP

**Endpoint**: `POST /auth/otp/verify`

**Request Body**:
```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "code": "123456",
  "phone": "+919876543210"
}
```

**Response**:
```json
{
  "verified": true,
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "User",
    "username": null,
    "email": null,
    "phone": "919876543210",
    "avatarUrl": null,
    "phoneVerified": true,
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-15T10:30:00Z",
    "created": false
  },
  "tokens": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresInSec": 5400,
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

### 3. Update User Profile

**Endpoint**: `POST /auth/profile/update`

**Headers**:
```
Authorization: Bearer <access_token>
```

**Request Body**:
```json
{
  "name": "John Doe",
  "username": "johndoe",
  "email": "john@example.com",
  "avatarUrl": "https://example.com/avatar.jpg"
}
```

**Response**:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "John Doe",
  "username": "johndoe",
  "email": "john@example.com",
  "phone": "919876543210",
  "avatarUrl": "https://example.com/avatar.jpg",
  "phoneVerified": true,
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:35:00Z",
  "created": false
}
```

## cURL Test Examples

### 1. Request OTP

```bash
curl -X POST http://localhost:8080/auth/otp/request \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "+919876543210",
    "purpose": "LOGIN"
  }'
```

**Expected Response**:
```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "expiresInSec": 300,
  "resendInSec": 30
}
```

### 2. Verify OTP

```bash
curl -X POST http://localhost:8080/auth/otp/verify \
  -H "Content-Type: application/json" \
  -d '{
    "requestId": "550e8400-e29b-41d4-a716-446655440000",
    "code": "123456",
    "phone": "+919876543210"
  }'
```

**Expected Response**:
```json
{
  "verified": true,
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "User",
    "username": null,
    "email": null,
    "phone": "919876543210",
    "avatarUrl": null,
    "phoneVerified": true,
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-15T10:30:00Z",
    "created": false
  },
  "tokens": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresInSec": 5400,
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

### 3. Update User Profile

```bash
curl -X POST http://localhost:8080/auth/profile/update \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-access-token>" \
  -d '{
    "name": "John Doe",
    "username": "johndoe",
    "email": "john@example.com",
    "avatarUrl": "https://example.com/avatar.jpg"
  }'
```

**Expected Response**:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "John Doe",
  "username": "johndoe",
  "email": "john@example.com",
  "phone": "919876543210",
  "avatarUrl": "https://example.com/avatar.jpg",
  "phoneVerified": true,
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:35:00Z",
  "created": false
}
```

### 4. Test with Different Phone Formats

```bash
# Test with 10-digit Indian number (will be normalized to 91XXXXXXXXXX)
curl -X POST http://localhost:8080/auth/otp/request \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "9876543210",
    "purpose": "LOGIN"
  }'

# Test with international format
curl -X POST http://localhost:8080/auth/otp/request \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "+919876543210",
    "purpose": "LOGIN"
  }'
```

## Error Responses

### Validation Error
```json
{
  "error": "VALIDATION_ERROR",
  "message": "phone: Invalid phone number format"
}
```

### OTP Request Expired
```json
{
  "error": "OTP_EXPIRED",
  "message": "Request expired or invalid"
}
```

### Invalid OTP Code
```json
{
  "error": "INVALID_CODE",
  "message": "Code incorrect"
}
```


### OTP Send Failed
```json
{
  "error": "OTP_SEND_FAILED",
  "message": "Invalid mobile number"
}
```

## Simplified OTP Flow

The application uses MSG91's managed OTP flow with pure phone-based user management:

1. **Request OTP**: Client sends phone number → Server calls MSG91 → Returns `requestId`
2. **Verify OTP**: Client sends `requestId` + OTP code + phone → Server calls MSG91 → Finds/creates user by phone → Returns JWT tokens
3. **Update Profile**: Client can update user profile using JWT token

**Key Benefits**:
- **Maximum Simplicity**: Pure phone-based user lookup - no complex session management
- **Reliable**: Not dependent on MSG91's requestId system for user management
- **Clean Architecture**: Single source of truth - phone number
- **Fast**: Direct phone lookup in database
- **Scalable**: Works across multiple server instances
- **Maintainable**: Simple, easy-to-understand flow
- **Secure**: Phone number validation through MSG91 OTP verification
- **Flexible**: Handles both new user registration and existing user login seamlessly

## Phone Number Normalization

The application automatically normalizes phone numbers:

- **Input**: `+919876543210` → **Normalized**: `919876543210`
- **Input**: `9876543210` → **Normalized**: `919876543210` (assumes Indian number)
- **Input**: `+1234567890` → **Normalized**: `1234567890`

## Project Structure

The project follows the standard Spring Boot layered architecture:

```
src/main/java/com/example/demo/
├── auth/                       # Authentication-specific components
│   ├── dtos/                   # Data Transfer Objects (DTOs)
│   ├── config/                 # Configuration properties
│   ├── jwt/                    # JWT utility classes
│   ├── util/                   # Utility classes
│   └── vendor/                 # MSG91 API client
├── controllers/                # All REST controllers
│   ├── AuthController.java     # Authentication endpoints
│   └── GlobalExceptionHandler.java
├── models/                     # All JPA entities
│   └── User.java              # User entity
├── repositories/               # All JPA repositories
│   └── UserRepository.java    # User repository
├── services/                   # All business logic services
│   └── UserService.java       # User service
└── DemoApplication.java        # Main application class
```

### **Layered Architecture Benefits:**

- **🏗️ Standard Structure**: Follows Spring Boot conventions
- **📁 Clear Organization**: Grouped by layer (controllers, services, repositories)
- **🔍 Easy Navigation**: Developers know exactly where to find components
- **♻️ Reusability**: Services and repositories can be used by any controller
- **🧪 Testability**: Each layer can be tested independently
- **👥 Team Development**: Clear separation allows parallel development
- **📈 Scalability**: Easy to add new controllers, services, or repositories

## Dependencies

- **Spring Boot Web**: REST API framework
- **Spring Boot Validation**: Input validation
- **Spring WebFlux**: Reactive HTTP client for MSG91 API
- **Spring Boot Data JPA**: Database persistence
- **PostgreSQL Driver**: Database connectivity
- **JJWT**: JWT token generation and parsing
- **Lombok**: Reduces boilerplate code

## Security Considerations

1. **JWT Secret**: Use a strong, randomly generated secret key
2. **Environment Variables**: Store sensitive credentials in environment variables
3. **HTTPS**: Use HTTPS in production
4. **Rate Limiting**: Consider implementing rate limiting for OTP requests
5. **Session Cleanup**: OTP sessions are automatically cleaned up after expiry

## Future Enhancements

1. **User Profile Management**: Add endpoints to update user profile
2. **Rate Limiting**: Implement rate limiting for OTP requests
3. **Refresh Token Endpoint**: Add token refresh functionality
4. **Email Verification**: Add email verification flow
5. **Password Reset**: Add password reset functionality
6. **Audit Logging**: Add comprehensive audit logging
7. **Caching**: Add Redis caching for better performance

## Troubleshooting

### Common Issues

1. **MSG91 API Errors**: Check your API credentials and template ID
2. **JWT Errors**: Ensure JWT secret is properly configured
3. **Phone Validation**: Check phone number format requirements
4. **Session Expiry**: OTP sessions expire after 5 minutes

### Logs

Enable debug logging by setting:
```yaml
logging:
  level:
    com.example.demo: DEBUG
```

## License

This project is for demonstration purposes. Please ensure you comply with MSG91's terms of service when using their API.
