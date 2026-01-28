# Feature Flags Admin API

Welcome to the Feature Flags Admin API documentation. This API provides endpoints for managing feature flags, environments, and users in your feature flag system.

## Quick Start

### Base URL

```
http://localhost:8080
```

### Authentication

Most endpoints require JWT authentication. Obtain a token via the login endpoint:

```bash
# Login to get a token
curl -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@example.com", "password": "password123"}'

# Use the token in subsequent requests
curl -X GET "http://localhost:8080/users" \
  -H "Authorization: Bearer <your_jwt_token>"
```

---

## API Documentation

| API | Description | Documentation |
|-----|-------------|---------------|
| **Authentication** | User registration and login | [authentication.md](authentication.md) |
| **Users** | User management (CRUD) | [users.md](users.md) |
| **Environments** | Deployment environment management | [environments.md](environments.md) |

---

## Common Concepts

### Roles & Permissions

| Role | Description |
|------|-------------|
| `ADMIN` | Full access to all endpoints |
| `GUEST` | Read-only access to most resources |

> The first registered user automatically receives the `ADMIN` role.

### Error Response Format

All API errors follow a consistent format:

```json
{
  "timestamp": "2026-01-28T14:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Descriptive error message"
}
```

### Validation Errors

Validation errors include field-specific details:

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

### HTTP Status Codes

| Code | Meaning |
|------|---------|
| `200 OK` | Request succeeded |
| `201 Created` | Resource created successfully |
| `204 No Content` | Request succeeded (no response body) |
| `400 Bad Request` | Validation error or business rule violation |
| `401 Unauthorized` | Missing or invalid authentication |
| `403 Forbidden` | Insufficient permissions |
| `404 Not Found` | Resource not found |
| `405 Method Not Allowed` | HTTP method not supported |
| `500 Internal Server Error` | Unexpected server error |

---

## Soft Delete vs Hard Delete

| Entity | Delete Type | Description |
|--------|-------------|-------------|
| Environments | Soft Delete | Sets `isActive=false`, preserves data |
| Users | Hard Delete | Permanently removes from database |

---

## Audit Fields

All entities include automatic audit tracking:

| Field | Description |
|-------|-------------|
| `createdAt` | Timestamp when the entity was created |
| `updatedAt` | Timestamp of the last modification |

---

## Getting Started

1. **Register** the first admin account:
   ```bash
   curl -X POST "http://localhost:8080/auth/register" \
     -H "Content-Type: application/json" \
     -d '{
       "email": "admin@example.com",
       "password": "securePassword123",
       "firstName": "Admin",
       "lastName": "User"
     }'
   ```

2. **Create environments** for your deployment targets:
   ```bash
   curl -X POST "http://localhost:8080/environments" \
     -H "Authorization: Bearer <token>" \
     -H "Content-Type: application/json" \
     -d '{"key": "production", "name": "Production", "description": "Live environment"}'
   ```

3. Refer to individual API documentation for detailed usage.
