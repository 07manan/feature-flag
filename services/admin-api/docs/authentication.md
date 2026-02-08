# Authentication API

The Authentication API provides endpoints for user registration, login, and OAuth authentication in the feature flag system. It uses JWT (JSON Web Token) for stateless authentication.

## Table of Contents

- [Overview](#overview)
- [Endpoints](#endpoints)
  - [Register](#register)
  - [Login](#login)
  - [OAuth2 Login](#oauth2-login)
- [Data Models](#data-models)
- [Error Responses](#error-responses)
- [Security Considerations](#security-considerations)

---

## Overview

The Authentication API allows users to create accounts and obtain JWT tokens for accessing protected endpoints. Users can authenticate via email/password or OAuth providers (Google, with GitHub and Microsoft coming soon).

**Base URL:** `/auth`

**Public Endpoints:** All authentication endpoints are publicly accessible (no token required).

**Supported OAuth Providers:**

| Provider | Status | ID Token Source |
|----------|--------|----------------|
| Google | ✅ Available | Google Sign-In SDK |
| GitHub | 🔜 Coming Soon | - |
| Microsoft | 🔜 Coming Soon | - |

---

## Endpoints

### Register

Create a new user account and receive a JWT token.

**Request**

```
POST /auth/register
Content-Type: application/json
```

```json
{
  "email": "user@example.com",
  "password": "securePassword123",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Request Body**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `email` | `string` | Yes | Valid email format | User's email address (used as username) |
| `password` | `string` | Yes | Minimum 8 characters | User's password |
| `firstName` | `string` | Yes | Non-empty | User's first name |
| `lastName` | `string` | Yes | Non-empty | User's last name |

**Response**

```
HTTP/1.1 201 Created
Content-Type: application/json
```

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": "GUEST",
    "enabled": true,
    "createdAt": "2026-01-28T14:00:00Z",
    "updatedAt": "2026-01-28T14:00:00Z"
  }
}
```

**Error Responses**

| Status | Condition |
|--------|-----------|
| `400 Bad Request` | Validation failed (missing/invalid fields) |
| `400 Bad Request` | Email already registered |

**Example: Email Already Registered**

```json
{
  "timestamp": "2026-01-28T14:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Email already registered"
}
```

---

### Login

Authenticate with credentials and receive a JWT token.

**Request**

```
POST /auth/login
Content-Type: application/json
```

```json
{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

**Request Body**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `email` | `string` | Yes | Valid email format | Registered email address |
| `password` | `string` | Yes | Non-empty | User's password |

**Response**

```
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": "ADMIN",
    "enabled": true,
    "createdAt": "2026-01-15T10:00:00Z",
    "updatedAt": "2026-01-28T14:00:00Z"
  }
}
```

**Error Responses**

| Status | Condition |
|--------|-----------|
| `400 Bad Request` | Validation failed (missing/invalid email format) |
| `401 Unauthorized` | Invalid email or password |
| `401 Unauthorized` | Account is disabled |

---

### OAuth2 Login

Authenticate using an OAuth provider (e.g., Google) and receive a JWT token.

**Request**

```
POST /auth/oauth2/{provider}
Content-Type: application/json
```

**Path Parameters**

| Parameter | Type | Description |
|-----------|------|-------------|
| `provider` | `string` | OAuth provider name: `google`, `github`, `microsoft` (case-insensitive) |

**Request Body**

```json
{
  "token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `token` | `string` | Yes | The ID token from the OAuth provider |

**Response**

```
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@gmail.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": "GUEST",
    "enabled": true,
    "createdAt": "2026-01-28T14:00:00Z",
    "updatedAt": "2026-01-28T14:00:00Z"
  }
}
```

**Behavior**

| Scenario | Result |
|----------|--------|
| New user (email not in system) | Creates new account, links OAuth provider |
| Existing user (same email) | Links OAuth provider to existing account (merge) |
| Returning OAuth user | Returns existing user |

**Error Responses**

| Status | Condition |
|--------|-----------||
| `400 Bad Request` | Missing token in request body |
| `400 Bad Request` | Unsupported OAuth provider |
| `401 Unauthorized` | Invalid or expired OAuth token |
| `401 Unauthorized` | Email not verified by provider |

**Example: Invalid Token**

```json
{
  "timestamp": "2026-01-28T14:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid Google ID token"
}
```

---

## Data Models

### AuthResponse

Returned on successful registration or login.

| Field | Type | Description |
|-------|------|-------------|
| `token` | `string` | JWT access token |
| `tokenType` | `string` | Token type, always `"Bearer"` |
| `user` | `UserDto` | Authenticated user's details |

### UserDto (in AuthResponse)

| Field | Type | Description |
|-------|------|-------------|
| `id` | `UUID` | User's unique identifier |
| `email` | `string` | User's email address |
| `firstName` | `string` | User's first name |
| `lastName` | `string` | User's last name |
| `role` | `string` | User's role: `ADMIN` or `GUEST` |
| `enabled` | `boolean` | Whether the account is enabled |
| `createdAt` | `ISO 8601` | Account creation timestamp |
| `updatedAt` | `ISO 8601` | Last update timestamp |

### Roles

| Role | Description |
|------|-------------|
| `ADMIN` | Full access to all endpoints including user management |
| `GUEST` | Limited access (read-only for most resources) |

---

## Error Responses

### Standard Error Format

```json
{
  "timestamp": "2026-01-28T14:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Descriptive error message"
}
```

### Validation Errors

```json
{
  "timestamp": "2026-01-28T14:00:00Z",
  "status": 400,
  "error": "Validation Failed",
  "errors": {
    "email": "Invalid email format",
    "password": "Password must be at least 8 characters"
  }
}
```

### Common Error Codes

| Status Code | Description |
|-------------|-------------|
| `400 Bad Request` | Validation error or duplicate email |
| `401 Unauthorized` | Invalid credentials or disabled account |
| `500 Internal Server Error` | Unexpected server error |

---

## Security Considerations

### Password Requirements

- Minimum 8 characters
- Passwords are hashed using BCrypt before storage
- Plain text passwords are never stored or logged

### JWT Token

- Include in subsequent requests via the `Authorization` header:
  ```
  Authorization: Bearer <token>
  ```
- Tokens have an expiration time (configured server-side)
- Store tokens securely (avoid localStorage for sensitive applications)

### Role Assignment

- **First registered user** automatically receives the `ADMIN` role
- **All subsequent users** are assigned the `GUEST` role by default
- Role can only be changed by an admin via the [Users API](users.md)

### Account Status

- New accounts are enabled by default
- Disabled accounts cannot authenticate (receive `401 Unauthorized`)
- Account status can be modified by admins via the [Users API](users.md)

### OAuth Authentication

- **Token Verification**: OAuth tokens are verified directly with the provider (Google's public keys)
- **Account Merging**: If a user with the same email already exists, the OAuth provider is linked to the existing account
- **Multiple Providers**: A single user can have multiple OAuth providers linked (e.g., both Google and GitHub)
- **No Password Required**: OAuth-only users don't have a password set
- **Email Verification**: Only verified emails from OAuth providers are accepted

---

## Examples

### cURL Examples

**Register a new user:**
```bash
curl -X POST "http://localhost:8080/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@example.com",
    "password": "securePassword123",
    "firstName": "Jane",
    "lastName": "Smith"
  }'
```

**Login:**
```bash
curl -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@example.com",
    "password": "securePassword123"
  }'
```

**OAuth2 Login (Google):**
```bash
# The token is obtained from Google Sign-In on the frontend
curl -X POST "http://localhost:8080/auth/oauth2/google" \
  -H "Content-Type: application/json" \
  -d '{
    "token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'
```

**Using the token in subsequent requests:**
```bash
# Store the token from login response
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Use token in protected endpoint
curl -X GET "http://localhost:8080/users" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Related Documentation

- [Users API](users.md) - User management endpoints
- [Environments API](environments.md) - Environment management endpoints
